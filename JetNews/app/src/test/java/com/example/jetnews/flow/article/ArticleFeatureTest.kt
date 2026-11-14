package com.example.jetnews.flow.article

import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.favorites.GetFavorites
import com.example.jetnews.data.favorites.ToggleFavorite
import com.example.jetnews.data.posts.LoadPost
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.model.Post
import com.example.jetnews.testing.FakePostsRepository
import com.example.jetnews.testing.RecordingAction
import com.example.jetnews.testing.testPosts
import com.genovich.components.OnDemandFeatureUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleFeatureTest {

    private val favoritesStore = FavoritesStore()

    private fun TestScope.startArticleFeature() = ArticleFeature(
        openArticle = showChannel(),
        closeArticle = showChannel(),
        content = {
            ArticleContent(
                loadPost = LoadPost(FakePostsRepository()),
                getFavorites = GetFavorites(favoritesStore),
                toggleFavorite = ToggleFavorite(favoritesStore),
                sharePost = RecordingAction<Post, Unit> { }
            )
        }
    ).let { (feature, uiState) ->
        backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { feature(testPosts) } to
            uiState
    }

    @Test
    fun opensTheTappedPostAndClosesOnBack() = runTest {
        val (result, uiState) = startArticleFeature()
        (uiState.value as OnDemandFeatureUiState.Open).flow.value.action("post4")

        val open = uiState.value as OnDemandFeatureUiState.FeatureAndClose
        val content = open.featureFlow.value as LoadAndShowUiState.Content
        assertEquals(testPosts[4], content.content.value.article.value.input.post)

        open.closeFlow.value.action(Unit)

        assertTrue(result.isCompleted)
        assertNull(uiState.value)
    }

    @Test
    fun aMissingPostIsAnError() = runTest {
        val (_, uiState) = startArticleFeature()
        (uiState.value as OnDemandFeatureUiState.Open).flow.value.action("missing")

        val open = uiState.value as OnDemandFeatureUiState.FeatureAndClose
        val error = open.featureFlow.value as LoadAndShowUiState.Error
        assertEquals("postId doesn't exist", error.error.value.input.message)
    }
}
