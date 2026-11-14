package com.example.jetnews.ui.home

import com.example.jetnews.data.Result
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.impl.posts
import com.example.jetnews.model.Post
import com.example.jetnews.testing.ControllablePostsRepository
import com.example.jetnews.testing.TestComponentContext
import com.example.jetnews.ui.RefreshableUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHomeComponentTest {

    private val repository = ControllablePostsRepository()
    private val favoritesStore = FavoritesStore()
    private val dispatcher = UnconfinedTestDispatcher()
    private val selectedPosts = mutableListOf<String>()

    private fun createComponent(context: TestComponentContext = TestComponentContext()) =
        DefaultHomeComponent(
            componentContext = context.context,
            postsRepository = repository,
            favoritesStore = favoritesStore,
            mainContext = dispatcher,
            onArticleSelected = { selectedPosts += it }
        )

    private val loading = RefreshableUiState.Success<List<Post>>(data = null, loading = true)
    private val loaded = RefreshableUiState.Success(data = posts, loading = false)

    @Test
    fun startsLoading() {
        val component = createComponent()

        assertEquals(loading, component.model.value.posts)
        assertEquals(1, repository.postsRequests)
    }

    @Test
    fun loadingSucceeds() {
        val component = createComponent()

        repository.completePosts(Result.Success(posts))

        assertEquals(loaded, component.model.value.posts)
    }

    @Test
    fun loadingFails() {
        val component = createComponent()
        val exception = IllegalStateException("boom")

        repository.completePosts(Result.Error(exception))

        assertEquals(RefreshableUiState.Error(exception, null), component.model.value.posts)
    }

    @Test
    fun onRefresh_keepsPostsWhileReloading() {
        val component = createComponent()
        repository.completePosts(Result.Success(posts))

        component.onRefresh()

        assertEquals(2, repository.postsRequests)
        assertEquals(
            RefreshableUiState.Success(data = posts, loading = true),
            component.model.value.posts
        )
    }

    @Test
    fun onRefresh_replacesPostsOnSuccess() {
        val component = createComponent()
        repository.completePosts(Result.Success(posts))
        val newPosts = posts.take(1)

        component.onRefresh()
        repository.completePosts(Result.Success(newPosts))

        assertEquals(
            RefreshableUiState.Success(data = newPosts, loading = false),
            component.model.value.posts
        )
    }

    @Test
    fun onRefresh_keepsPostsOnError() {
        val component = createComponent()
        repository.completePosts(Result.Success(posts))
        val exception = IllegalStateException("boom")

        component.onRefresh()
        repository.completePosts(Result.Error(exception))

        assertEquals(RefreshableUiState.Error(exception, posts), component.model.value.posts)
    }

    @Test
    fun onRefresh_retriesAfterAnError() {
        val component = createComponent()
        repository.completePosts(Result.Error(IllegalStateException("boom")))

        component.onRefresh()
        assertEquals(loading, component.model.value.posts)
        repository.completePosts(Result.Success(posts))

        assertEquals(2, repository.postsRequests)
        assertEquals(loaded, component.model.value.posts)
    }

    @Test
    fun onRefresh_isIgnoredWhileLoading() {
        val component = createComponent()

        component.onRefresh()

        assertEquals(1, repository.postsRequests)
        assertEquals(loading, component.model.value.posts)
    }

    @Test
    fun favoritesFollowTheStore() {
        favoritesStore.toggle("a")
        val component = createComponent()
        assertEquals(setOf("a"), component.model.value.favorites)

        favoritesStore.toggle("b")

        assertEquals(setOf("a", "b"), component.model.value.favorites)
    }

    @Test
    fun onFavoriteToggled_updatesTheStore() {
        val component = createComponent()

        component.onFavoriteToggled("a")
        assertEquals(setOf("a"), favoritesStore.favorites.value)
        assertEquals(setOf("a"), component.model.value.favorites)

        component.onFavoriteToggled("a")
        assertEquals(emptySet<String>(), component.model.value.favorites)
    }

    @Test
    fun onPostClicked_selectsTheArticle() {
        val component = createComponent()

        component.onPostClicked("post")

        assertEquals(listOf("post"), selectedPosts)
    }

    @Test
    fun survivesConfigurationChangeWithoutReloading() {
        val first = TestComponentContext()
        createComponent(first)
        repository.completePosts(Result.Success(posts))

        // Same InstanceKeeper, new component: what happens on rotation
        val recreated = createComponent(TestComponentContext(instanceKeeper = first.instanceKeeper))

        assertEquals(1, repository.postsRequests)
        assertEquals(loaded, recreated.model.value.posts)
    }

    @Test
    fun destroyingTheInstanceKeeperCancelsLoading() {
        val context = TestComponentContext()
        val component = createComponent(context)

        context.instanceKeeper.destroy()
        repository.completePosts(Result.Success(posts))

        assertEquals(loading, component.model.value.posts)
    }
}
