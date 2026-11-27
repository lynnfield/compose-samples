package com.example.jetnews.ui.interests

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.example.jetnews.data.awaitPeople
import com.example.jetnews.data.awaitPublications
import com.example.jetnews.data.awaitTopics
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.toUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Interests screen: topics, people and publications the user can follow, in tabs. A list
 * that fails to load shows an error with Retry.
 */
interface InterestsComponent {

    val model: Value<Model>

    fun onSectionSelected(section: Section)

    fun onTopicToggled(topicKey: String, selected: Boolean)

    fun onRetry()

    enum class Section(val title: String) {
        Topics("Topics"),
        People("People"),
        Publications("Publications")
    }

    data class Model(
        val section: Section,
        val topics: UiState<Map<String, List<String>>>,
        val people: UiState<List<String>>,
        val publications: UiState<List<String>>,
        val selectedTopics: Set<String>
    )
}

/**
 * Key under which a followed item is stored in [SelectedTopicsStore].
 */
fun topicKey(tab: String, group: String, topic: String) = "$tab-$group-$topic"

class DefaultInterestsComponent(
    componentContext: ComponentContext,
    interestsRepository: InterestsRepository,
    private val selectedTopicsStore: SelectedTopicsStore,
    mainContext: CoroutineContext
) : InterestsComponent, ComponentContext by componentContext {

    // Survives configuration changes, so the lists and the selected tab are kept
    private val retained = instanceKeeper.getOrCreate {
        Retained(interestsRepository, selectedTopicsStore, mainContext)
    }

    override val model: Value<InterestsComponent.Model> = retained.model

    override fun onSectionSelected(section: InterestsComponent.Section) {
        retained.model.update { it.copy(section = section) }
    }

    override fun onTopicToggled(topicKey: String, selected: Boolean) {
        selectedTopicsStore.setSelected(topicKey, selected)
    }

    override fun onRetry() {
        retained.loadFailed()
    }

    private class Retained(
        private val interestsRepository: InterestsRepository,
        selectedTopicsStore: SelectedTopicsStore,
        mainContext: CoroutineContext
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(mainContext + SupervisorJob())

        val model = MutableValue(
            InterestsComponent.Model(
                section = InterestsComponent.Section.Topics,
                topics = UiState.Loading,
                people = UiState.Loading,
                publications = UiState.Loading,
                selectedTopics = selectedTopicsStore.selectedTopics.value
            )
        )

        init {
            loadTopics()
            loadPeople()
            loadPublications()
            scope.launch {
                selectedTopicsStore.selectedTopics.collect { selectedTopics ->
                    model.update { it.copy(selectedTopics = selectedTopics) }
                }
            }
        }

        // Loads again the lists whose last load failed
        fun loadFailed() {
            val current = model.value
            if (current.topics is UiState.Error) loadTopics()
            if (current.people is UiState.Error) loadPeople()
            if (current.publications is UiState.Error) loadPublications()
        }

        private fun loadTopics() {
            model.update { it.copy(topics = UiState.Loading) }
            scope.launch {
                val topics = interestsRepository.awaitTopics().toUiState()
                model.update { it.copy(topics = topics) }
            }
        }

        private fun loadPeople() {
            model.update { it.copy(people = UiState.Loading) }
            scope.launch {
                val people = interestsRepository.awaitPeople().toUiState()
                model.update { it.copy(people = people) }
            }
        }

        private fun loadPublications() {
            model.update { it.copy(publications = UiState.Loading) }
            scope.launch {
                val publications = interestsRepository.awaitPublications().toUiState()
                model.update { it.copy(publications = publications) }
            }
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}
