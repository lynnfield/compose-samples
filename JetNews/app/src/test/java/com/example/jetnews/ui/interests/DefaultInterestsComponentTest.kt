package com.example.jetnews.ui.interests

import com.example.jetnews.data.Result
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.testing.TestComponentContext
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.interests.InterestsComponent.Section
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInterestsComponentTest {

    private val selectedTopicsStore = SelectedTopicsStore()
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createComponent(
        repository: InterestsRepository = FakeInterestsRepository(),
        context: TestComponentContext = TestComponentContext()
    ) = DefaultInterestsComponent(
        componentContext = context.context,
        interestsRepository = repository,
        selectedTopicsStore = selectedTopicsStore,
        mainContext = dispatcher
    )

    @Test
    fun loadsTopicsPeopleAndPublications() {
        val repository = FakeInterestsRepository()
        val model = createComponent(repository).model.value

        assertEquals(expected(repository::getTopics), model.topics)
        assertEquals(expected(repository::getPeople), model.people)
        assertEquals(expected(repository::getPublications), model.publications)
    }

    @Test
    fun startsOnTopicsAndSwitchesSections() {
        val component = createComponent()
        assertEquals(Section.Topics, component.model.value.section)

        component.onSectionSelected(Section.Publications)

        assertEquals(Section.Publications, component.model.value.section)
    }

    @Test
    fun onTopicToggled_goesThroughTheStore() {
        val component = createComponent()
        val key = topicKey("Topics", "Android", "Kotlin")

        component.onTopicToggled(key, true)
        assertEquals(setOf(key), selectedTopicsStore.selectedTopics.value)
        assertEquals(setOf(key), component.model.value.selectedTopics)

        component.onTopicToggled(key, false)
        assertTrue(component.model.value.selectedTopics.isEmpty())
    }

    @Test
    fun selectedTopicsFollowTheStore() {
        selectedTopicsStore.setSelected("a", true)
        val component = createComponent()

        selectedTopicsStore.setSelected("b", true)

        assertEquals(setOf("a", "b"), component.model.value.selectedTopics)
    }

    @Test
    fun keepsSectionAcrossConfigurationChange() {
        val first = TestComponentContext()
        createComponent(context = first).onSectionSelected(Section.People)

        val recreated =
            createComponent(context = TestComponentContext(instanceKeeper = first.instanceKeeper))

        assertEquals(Section.People, recreated.model.value.section)
    }

    private fun <T> expected(call: ((Result<T>) -> Unit) -> Unit): UiState<T> {
        var state: UiState<T> = UiState.Loading
        call { result -> state = UiState.Success((result as Result.Success).data) }
        return state
    }
}
