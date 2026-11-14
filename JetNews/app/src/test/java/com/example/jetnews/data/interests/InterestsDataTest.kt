package com.example.jetnews.data.interests

import com.example.jetnews.testing.FakeInterestsRepository
import com.genovich.components.OneOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InterestsDataTest {

    @Test
    fun selectTopic_selectsAndDeselects() = runTest {
        val store = SelectedTopicsStore()
        val selectTopic = SelectTopic(store)
        val getSelectedTopics = GetSelectedTopics(store)

        selectTopic(TopicSelection("Topics-Android-Kotlin", selected = true))
        assertEquals(setOf("Topics-Android-Kotlin"), getSelectedTopics(Unit))

        selectTopic(TopicSelection("Topics-Android-Kotlin", selected = false))
        assertEquals(emptySet<String>(), getSelectedTopics(Unit))
    }

    @Test
    fun loaders_returnRepositoryData() = runTest {
        val repository = FakeInterestsRepository()

        assertEquals(OneOf.First(mapOf("Android" to listOf("Kotlin"))), LoadTopics(repository)(Unit))
        assertEquals(OneOf.First(listOf("Person")), LoadPeople(repository)(Unit))
        assertEquals(OneOf.First(listOf("Publication")), LoadPublications(repository)(Unit))
    }
}
