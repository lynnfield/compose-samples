package com.example.jetnews.data.interests

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedTopicsStoreTest {

    private val store = SelectedTopicsStore()

    @Test
    fun startsEmpty() {
        assertEquals(emptySet<String>(), store.selectedTopics.value)
    }

    @Test
    fun setSelected_addsAndRemoves() {
        store.setSelected("topic", true)
        assertEquals(setOf("topic"), store.selectedTopics.value)

        store.setSelected("topic", false)
        assertEquals(emptySet<String>(), store.selectedTopics.value)
    }

    @Test
    fun setSelected_isIdempotent() {
        store.setSelected("topic", true)
        store.setSelected("topic", true)
        store.setSelected("other", false)

        assertEquals(setOf("topic"), store.selectedTopics.value)
    }
}
