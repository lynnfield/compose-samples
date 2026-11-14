package com.example.jetnews.data.favorites

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesTest {

    @Test
    fun toggle_addsThenRemoves() = runTest {
        val store = FavoritesStore()
        val toggleFavorite = ToggleFavorite(store)
        val getFavorites = GetFavorites(store)

        toggleFavorite("post1")
        assertEquals(setOf("post1"), getFavorites(Unit))

        toggleFavorite("post1")
        assertEquals(emptySet<String>(), getFavorites(Unit))
    }
}
