package com.example.jetnews.flow.article

import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.favorites.GetFavorites
import com.example.jetnews.data.favorites.ToggleFavorite
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.model.Post
import com.example.jetnews.testing.RecordingAction
import com.example.jetnews.testing.testPost
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShowArticleTest {

    private val post = testPost("post1")
    private val favoritesStore = FavoritesStore()
    private val sharePost = RecordingAction<Post, Unit> { }

    private fun TestScope.startShowArticle(): StateFlow<ShowArticleUiState?> {
        val (showArticle, uiState) = ShowArticle(
            getFavorites = GetFavorites(favoritesStore),
            showArticle = showChannel(),
            toggleFavorite = ToggleFavorite(favoritesStore),
            share = showChannel(),
            sharePost = sharePost
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { showArticle(post) }
        return uiState
    }

    @Test
    fun showsThePost() = runTest {
        val uiState = startShowArticle()

        assertEquals(ArticleState(post, isFavorite = false), uiState.value!!.article.value.input)
    }

    @Test
    fun bookmarkTogglesAndShowsAgain() = runTest {
        val uiState = startShowArticle()

        uiState.value!!.article.value.action(Unit)

        assertEquals(setOf("post1"), favoritesStore.favorites.value)
        assertEquals(ArticleState(post, isFavorite = true), uiState.value!!.article.value.input)
    }

    @Test
    fun shareSharesThePostAndShowsAgain() = runTest {
        val uiState = startShowArticle()

        uiState.value!!.share.value.action(post)

        assertEquals(listOf(post), sharePost.inputs)
        assertEquals(post, uiState.value!!.share.value.input)
    }
}
