package com.example.jetnews.flow.home

import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.favorites.GetFavorites
import com.example.jetnews.data.favorites.ToggleFavorite
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.model.Post
import com.example.jetnews.testing.testPosts
import com.genovich.components.Action
import com.genovich.components.OnDemandFeature
import com.genovich.components.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowHomeTest {

    private val favoritesStore = FavoritesStore()

    /**
     * Runs [ShowHome] with a stand-in article feature whose content is a T-function that
     * answers the id of a post to bookmark from inside the article.
     */
    private fun TestScope.startShowHome() = ShowHome(
        getFavorites = GetFavorites(favoritesStore),
        showHome = showChannel(),
        toggleFavorite = ToggleFavorite(favoritesStore),
        article = OnDemandFeature(
            open = showChannel<List<Post>, String>(),
            close = showChannel<List<Post>, Unit>(),
            featureBuilder = { _: List<Post> ->
                val (bookmark, flow) = showChannel<String, String>()
                val toggle = ToggleFavorite(favoritesStore)
                Action<String, Unit> { postId -> while (true) toggle(bookmark(postId)) } to flow
            }
        )
    ).also { (showHome, _) ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { showHome(testPosts) }
    }.second

    @Test
    fun showsPostsWithFavorites() = runTest {
        favoritesStore.toggle("post1")
        val uiState = startShowHome()

        val home = uiState.value as ShowHomeUiState.Home
        assertEquals(HomeState(testPosts, setOf("post1")), home.home.value.input)
    }

    @Test
    fun bookmarkTogglesAndShowsHomeAgain() = runTest {
        val uiState = startShowHome()

        (uiState.value as ShowHomeUiState.Home).home.value.action("post2")

        assertEquals(setOf("post2"), favoritesStore.favorites.value)
        val home = uiState.value as ShowHomeUiState.Home
        assertEquals(setOf("post2"), home.home.value.input.favorites)
    }

    @Test
    fun openArticleShowsTheArticle() = runTest {
        val uiState = startShowHome()

        (uiState.value as ShowHomeUiState.Home).openArticle.value.action("post3")

        val article = uiState.value as ShowHomeUiState.Article<*>
        assertEquals("post3", (article.article.value as UiState<*, *>).input)
    }

    @Test
    fun closingTheArticleShowsHomeWithFavoritesChangedInside() = runTest {
        val uiState = startShowHome()
        (uiState.value as ShowHomeUiState.Home).openArticle.value.action("post3")
        @Suppress("UNCHECKED_CAST")
        val article = uiState.value as ShowHomeUiState.Article<UiState<String, String>>

        article.article.value.action("post3")
        article.close.value.action(Unit)

        val home = uiState.value as ShowHomeUiState.Home
        assertEquals(setOf("post3"), home.home.value.input.favorites)
        assertTrue(home.openArticle.value.input === testPosts)
    }
}
