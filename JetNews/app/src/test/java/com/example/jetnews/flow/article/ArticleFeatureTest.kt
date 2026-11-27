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
import com.example.jetnews.testing.content
import com.example.jetnews.testing.error
import com.example.jetnews.testing.loaded
import com.example.jetnews.testing.testPosts
import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.TryAgainUiState
import kotlinx.coroutines.CompletableDeferred
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

    private fun TestScope.startArticleFeature(
        postId: String,
        loadPost: Action<String, OneOf<Post, Throwable>> = LoadPost(FakePostsRepository())
    ) = ArticleFeature(
        content = ArticleContent(
            loadPost = loadPost,
            getFavorites = GetFavorites(favoritesStore),
            toggleFavorite = ToggleFavorite(favoritesStore),
            sharePost = RecordingAction<Post, Unit> { }
        ),
        close = showChannel()
    ).let { (feature, uiState) ->
        backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { feature(postId) } to uiState
    }

    @Test
    fun showsThePostAndClosesOnBack() = runTest {
        val (result, uiState) = startArticleFeature("post4")

        val open = uiState.value!!
        assertEquals(testPosts[4], open.article.value.content.article.value.input.post)

        open.close.value.action(Unit)

        assertTrue(result.isCompleted)
        assertNull(uiState.value)
    }

    @Test
    fun showsProgressWhileThePostLoads() = runTest {
        val loaded = CompletableDeferred<OneOf<Post, Throwable>>()
        val (_, uiState) = startArticleFeature("post4", Action { loaded.await() })

        assertTrue(uiState.value!!.article.value.loaded is LoadAndShowUiState.Loading)

        loaded.complete(OneOf.First(testPosts[4]))
        assertEquals(testPosts[4], uiState.value!!.article.value.content.article.value.input.post)
    }

    @Test
    fun aMissingPostIsAnErrorWithRetry() = runTest {
        val loads = RecordingAction<String, OneOf<Post, Throwable>> {
            OneOf.Second(NoSuchElementException("postId doesn't exist"))
        }
        val (_, uiState) = startArticleFeature("missing", loads)

        val error = uiState.value!!.article.value.error
        assertEquals("postId doesn't exist", error.input.message)

        error.action(Unit)
        assertEquals(listOf("missing", "missing"), loads.inputs)
        assertTrue(uiState.value!!.article.value is TryAgainUiState.Error)
    }
}
