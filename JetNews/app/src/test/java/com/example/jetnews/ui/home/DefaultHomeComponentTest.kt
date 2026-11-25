package com.example.jetnews.ui.home

import com.example.jetnews.data.Result
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.impl.posts
import com.example.jetnews.testing.ControllablePostsRepository
import com.example.jetnews.testing.TestComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun startsLoading() {
        val component = createComponent()

        assertNull(component.model.value.posts)
        assertTrue(component.model.value.isLoading)
        assertEquals(1, repository.postsRequests)
    }

    @Test
    fun loadingSucceeds() {
        val component = createComponent()

        repository.completePosts(Result.Success(posts))

        assertEquals(posts, component.model.value.posts)
        assertFalse(component.model.value.isLoading)
    }

    @Test
    fun loadingFailsWithAnError() {
        val component = createComponent()

        repository.completePosts(Result.Error(IllegalStateException("boom")))

        assertNull(component.model.value.posts)
        assertFalse(component.model.value.isLoading)
        assertTrue(component.model.value.showError)
    }

    @Test
    fun onRetry_loadsAgain() {
        val component = createComponent()
        repository.completePosts(Result.Error(IllegalStateException("boom")))

        component.onRetry()
        assertTrue(component.model.value.isLoading)
        assertFalse(component.model.value.showError)
        assertEquals(2, repository.postsRequests)

        repository.completePosts(Result.Success(posts))
        assertEquals(posts, component.model.value.posts)
    }

    @Test
    fun onRefresh_keepsThePostsWhileReloading() {
        val component = createComponent()
        repository.completePosts(Result.Success(posts))

        component.onRefresh()
        assertEquals(posts, component.model.value.posts)
        assertTrue(component.model.value.isLoading)

        val newPosts = posts.take(1)
        repository.completePosts(Result.Success(newPosts))
        assertEquals(newPosts, component.model.value.posts)
        assertFalse(component.model.value.isLoading)
    }

    @Test
    fun aFailedRefreshKeepsThePostsAndShowsTheError() {
        val component = createComponent()
        repository.completePosts(Result.Success(posts))

        component.onRefresh()
        repository.completePosts(Result.Error(IllegalStateException("offline")))

        assertEquals(posts, component.model.value.posts)
        assertTrue(component.model.value.showError)
    }

    @Test
    fun onRefresh_whileLoadingDoesNotLoadTwice() {
        val component = createComponent()

        component.onRefresh()

        assertEquals(1, repository.postsRequests)
    }

    @Test
    fun onErrorDismissed_hidesTheError() {
        val component = createComponent()
        repository.completePosts(Result.Error(IllegalStateException("boom")))

        component.onErrorDismissed()

        assertFalse(component.model.value.showError)
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
        assertEquals(posts, recreated.model.value.posts)
    }

    @Test
    fun destroyingTheInstanceKeeperCancelsLoading() {
        val context = TestComponentContext()
        val component = createComponent(context)

        context.instanceKeeper.destroy()
        repository.completePosts(Result.Success(posts))

        assertNull(component.model.value.posts)
    }
}
