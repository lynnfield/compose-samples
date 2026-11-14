package com.example.jetnews.data.favorites

import com.genovich.components.Action

/**
 * Reads the ids of the bookmarked posts.
 */
class GetFavorites(
    val store: FavoritesStore
) : Action<Unit, Set<String>>() {

    override suspend fun invoke(input: Unit): Set<String> = store.favorites.value
}

/**
 * Bookmarks the post with the given id, or removes its bookmark.
 */
class ToggleFavorite(
    val store: FavoritesStore
) : Action<String, Unit>() {

    override suspend fun invoke(input: String) = store.toggle(input)
}
