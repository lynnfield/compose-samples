package com.example.jetnews.flow.interests

import com.example.jetnews.data.interests.GetSelectedTopics
import com.example.jetnews.data.interests.LoadPeople
import com.example.jetnews.data.interests.LoadPublications
import com.example.jetnews.data.interests.LoadTopics
import com.example.jetnews.data.interests.SelectTopic
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.testing.FakeInterestsRepository
import com.genovich.components.Action
import com.genovich.components.OnDemandFeatureUiState
import com.genovich.components.OneOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestsFeatureTest {

    private val store = SelectedTopicsStore()

    private fun loadInterests(repository: FakeInterestsRepository = FakeInterestsRepository()) =
        LoadInterests(LoadTopics(repository), LoadPeople(repository), LoadPublications(repository))

    private fun TestScope.startInterestsFeature() = InterestsFeature(
        openInterests = showChannel(),
        closeInterests = showChannel(),
        content = { InterestsContent(loadInterests(), GetSelectedTopics(store), SelectTopic(store)) }
    ).let { (feature, uiState) ->
        backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { feature(Unit) } to uiState
    }

    @Test
    fun loadInterests_combinesAllThree() = runTest {
        assertEquals(
            OneOf.First(Interests(mapOf("Android" to listOf("Kotlin")), listOf("Person"), listOf("Publication"))),
            loadInterests()(Unit)
        )
    }

    @Test
    fun loadInterests_stopsAtTheFirstError() = runTest {
        val exception = IllegalStateException("boom")

        assertEquals(
            OneOf.Second(exception),
            loadInterests(FakeInterestsRepository(peopleError = exception))(Unit)
        )
    }

    @Test
    fun loadInterests_runsAllThreeInParallel() = runTest {
        val started = mutableListOf<String>()
        val release = CompletableDeferred<Unit>()
        fun <T> pending(name: String, value: T) = Action<Unit, OneOf<T, Throwable>> {
            started += name
            release.await()
            OneOf.First(value)
        }
        val load = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            LoadInterests(
                pending("topics", emptyMap()),
                pending("people", emptyList()),
                pending("publications", emptyList())
            )(Unit)
        }

        assertEquals(listOf("topics", "people", "publications"), started)
        release.complete(Unit)
        assertEquals(OneOf.First(Interests(emptyMap(), emptyList(), emptyList())), load.await())
    }

    @Test
    fun opensSelectsATopicAndCloses() = runTest {
        val (result, uiState) = startInterestsFeature()
        (uiState.value as OnDemandFeatureUiState.Open).flow.value.action(Unit)

        val open = uiState.value as OnDemandFeatureUiState.FeatureAndClose
        val content = open.featureFlow.value as LoadAndShowUiState.Content
        content.content.value.action(TopicSelection("Topics-Android-Kotlin", selected = true))

        assertEquals(setOf("Topics-Android-Kotlin"), content.content.value.input.selectedTopics)

        open.closeFlow.value.action(Unit)
        assertTrue(result.isCompleted)
    }

    @Test
    fun selectionSurvivesReopening() = runTest {
        val (_, uiState) = startInterestsFeature()
        store.setSelected("People- -Person", true)

        (uiState.value as OnDemandFeatureUiState.Open).flow.value.action(Unit)

        val open = uiState.value as OnDemandFeatureUiState.FeatureAndClose
        val content = open.featureFlow.value as LoadAndShowUiState.Content
        assertEquals(setOf("People- -Person"), content.content.value.input.selectedTopics)
    }
}
