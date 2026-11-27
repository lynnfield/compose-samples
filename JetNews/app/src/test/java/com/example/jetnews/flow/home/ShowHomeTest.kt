package com.example.jetnews.flow.home

import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.favorites.GetFavorites
import com.example.jetnews.data.favorites.ToggleFavorite
import com.example.jetnews.flow.article.ArticleFeature
import com.example.jetnews.flow.common.Refresh
import com.example.jetnews.flow.common.RefreshUiState
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.model.Post
import com.example.jetnews.testing.testPost
import com.example.jetnews.testing.testPosts
import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.Publish
import com.genovich.components.ShowProgress
import com.genovich.components.UiState
import com.genovich.components.whileActive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowHomeTest {

    private val favoritesStore = FavoritesStore()
    private val newPosts = listOf(testPost("new"))
    private var reloaded = CompletableDeferred<OneOf<List<Post>, Throwable>>()

    /**
     * Runs [ShowHome] with a stand-in article whose content is a T-function that answers the id
     * of a post to bookmark from inside the article, and a refresh whose reload the test
     * completes through [reloaded].
     */
    private fun TestScope.startShowHome() = ShowHome(
        getFavorites = GetFavorites(favoritesStore),
        showHome = showChannel(),
        toggleFavorite = ToggleFavorite(favoritesStore),
        openArticle = showChannel(),
        article = ArticleFeature(
            content = showChannel<String, String>().let { (bookmark, flow) ->
                val toggle = ToggleFavorite(favoritesStore)
                Action<String, Nothing> { postId -> whileActive { toggle(bookmark(postId)) } } to flow
            },
            close = showChannel()
        ),
        refresh = Refresh(
            waitForRefresh = showChannel(),
            reload = ShowProgress(
                Action<Unit, OneOf<List<Post>, Throwable>> { reloaded.await() },
                Publish<Progress>()
            ),
            showError = showChannel()
        )
    ).also { (showHome, _) ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { showHome(testPosts) }
    }.second

    private val ShowHomeUiState<*>?.home get() = this as ShowHomeUiState.Home

    @Test
    fun showsPostsWithFavorites() = runTest {
        favoritesStore.toggle("post1")
        val uiState = startShowHome()

        assertEquals(HomeState(testPosts, setOf("post1")), uiState.value.home.home.value.input)
    }

    @Test
    fun bookmarkTogglesAndShowsHomeAgain() = runTest {
        val uiState = startShowHome()

        uiState.value.home.home.value.action("post2")

        assertEquals(setOf("post2"), favoritesStore.favorites.value)
        assertEquals(setOf("post2"), uiState.value.home.home.value.input.favorites)
    }

    @Test
    fun openArticleShowsTheArticle() = runTest {
        val uiState = startShowHome()

        uiState.value.home.openArticle.value.action("post3")

        val article = uiState.value as ShowHomeUiState.Article<*>
        assertEquals("post3", (article.article.value as UiState<*, *>).input)
    }

    @Test
    fun closingTheArticleShowsHomeWithFavoritesChangedInside() = runTest {
        val uiState = startShowHome()
        uiState.value.home.openArticle.value.action("post3")
        @Suppress("UNCHECKED_CAST")
        val article = uiState.value as ShowHomeUiState.Article<UiState<String, String>>

        article.article.value.action("post3")
        article.close.value.action(Unit)

        val home = uiState.value.home
        assertEquals(setOf("post3"), home.home.value.input.favorites)
        assertEquals(testPosts, home.home.value.input.posts)
    }

    @Test
    fun pullToRefreshShowsTheNewPosts() = runTest {
        val uiState = startShowHome()

        (uiState.value.home.refresh.value as RefreshUiState.Idle).refresh.value.action(Unit)
        assertTrue(uiState.value.home.refresh.value is RefreshUiState.Refreshing)
        assertEquals(testPosts, uiState.value.home.home.value.input.posts)

        reloaded.complete(OneOf.First(newPosts))

        assertEquals(newPosts, uiState.value.home.home.value.input.posts)
        assertTrue(uiState.value.home.refresh.value is RefreshUiState.Idle)
    }

    @Test
    fun aBookmarkDoesNotCancelARefresh() = runTest {
        val uiState = startShowHome()
        (uiState.value.home.refresh.value as RefreshUiState.Idle).refresh.value.action(Unit)

        uiState.value.home.home.value.action("post2")
        assertTrue(uiState.value.home.refresh.value is RefreshUiState.Refreshing)

        reloaded.complete(OneOf.First(newPosts))
        assertEquals(HomeState(newPosts, setOf("post2")), uiState.value.home.home.value.input)
    }

    @Test
    fun aFailedRefreshKeepsThePostsOrRetries() = runTest {
        val uiState = startShowHome()
        (uiState.value.home.refresh.value as RefreshUiState.Idle).refresh.value.action(Unit)
        reloaded.complete(OneOf.Second(IllegalStateException("offline")))

        val error = uiState.value.home.refresh.value as RefreshUiState.Error
        assertEquals("offline", error.error.value.input.message)
        assertEquals(testPosts, uiState.value.home.home.value.input.posts)

        reloaded = CompletableDeferred(OneOf.First(newPosts))
        error.error.value.action(true)
        assertEquals(newPosts, uiState.value.home.home.value.input.posts)
    }

    @Test
    fun dismissingARefreshErrorKeepsThePosts() = runTest {
        val uiState = startShowHome()
        (uiState.value.home.refresh.value as RefreshUiState.Idle).refresh.value.action(Unit)
        reloaded.complete(OneOf.Second(IllegalStateException("offline")))

        (uiState.value.home.refresh.value as RefreshUiState.Error).error.value.action(false)

        assertEquals(testPosts, uiState.value.home.home.value.input.posts)
        assertTrue(uiState.value.home.refresh.value is RefreshUiState.Idle)
    }

    @Test
    fun openingAnArticleCancelsARefresh() = runTest {
        val uiState = startShowHome()
        (uiState.value.home.refresh.value as RefreshUiState.Idle).refresh.value.action(Unit)

        uiState.value.home.openArticle.value.action("post3")
        reloaded.complete(OneOf.First(newPosts))
        (uiState.value as ShowHomeUiState.Article<*>).close.value.action(Unit)

        assertEquals(testPosts, uiState.value.home.home.value.input.posts)
        assertTrue(uiState.value.home.refresh.value is RefreshUiState.Idle)
    }
}
