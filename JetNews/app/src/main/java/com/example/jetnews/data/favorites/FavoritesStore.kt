package com.example.jetnews.data.favorites

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-scoped holder of the ids of the posts the user bookmarked.
 */
class FavoritesStore {

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggle(postId: String) {
        _favorites.update { favorites ->
            if (postId in favorites) favorites - postId else favorites + postId
        }
    }
}
