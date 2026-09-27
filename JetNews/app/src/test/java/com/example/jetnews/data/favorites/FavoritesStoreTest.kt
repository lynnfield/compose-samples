package com.example.jetnews.data.favorites

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesStoreTest {

    private val store = FavoritesStore()

    @Test
    fun startsEmpty() {
        assertEquals(emptySet<String>(), store.favorites.value)
    }

    @Test
    fun toggle_addsThenRemoves() {
        store.toggle("post")
        assertEquals(setOf("post"), store.favorites.value)

        store.toggle("post")
        assertEquals(emptySet<String>(), store.favorites.value)
    }

    @Test
    fun toggle_keepsOtherFavorites() {
        store.toggle("a")
        store.toggle("b")
        store.toggle("a")

        assertEquals(setOf("b"), store.favorites.value)
    }
}
