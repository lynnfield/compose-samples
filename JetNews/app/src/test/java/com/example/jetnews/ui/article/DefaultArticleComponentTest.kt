package com.example.jetnews.ui.article

import com.example.jetnews.data.Result
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.testing.ControllablePostsRepository
import com.example.jetnews.testing.TestComponentContext
import com.example.jetnews.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultArticleComponentTest {

    private val repository = ControllablePostsRepository()
    private val favoritesStore = FavoritesStore()
    private val dispatcher = UnconfinedTestDispatcher()
    private var backClicks = 0

    private fun createComponent(context: TestComponentContext = TestComponentContext()) =
        DefaultArticleComponent(
            componentContext = context.context,
            postId = post3.id,
            postsRepository = repository,
            favoritesStore = favoritesStore,
            mainContext = dispatcher,
            onBack = { backClicks++ }
        )

    @Test
    fun startsLoadingTheRequestedPost() {
        val component = createComponent()

        assertEquals(UiState.Loading, component.model.value.post)
        assertEquals(listOf(post3.id), repository.postRequests)
    }

    @Test
    fun loadingSucceeds() {
        val component = createComponent()

        repository.completePost(Result.Success(post3))

        assertEquals(UiState.Success(post3), component.model.value.post)
    }

    @Test
    fun missingPostIsAnError() {
        val component = createComponent()

        repository.completePost(Result.Success(null))

        val state = component.model.value.post
        assertTrue(state is UiState.Error)
        assertEquals("postId doesn't exist", (state as UiState.Error).exception.message)
    }

    @Test
    fun repositoryErrorIsAnError() {
        val component = createComponent()
        val exception = IllegalStateException("boom")

        repository.completePost(Result.Error(exception))

        assertEquals(UiState.Error(exception), component.model.value.post)
    }

    @Test
    fun isFavoriteFollowsTheStore() {
        favoritesStore.toggle(post3.id)
        val component = createComponent()
        assertTrue(component.model.value.isFavorite)

        favoritesStore.toggle(post3.id)
        assertFalse(component.model.value.isFavorite)

        favoritesStore.toggle("another post")
        assertFalse(component.model.value.isFavorite)
    }

    @Test
    fun onFavoriteToggled_togglesThisPost() {
        val component = createComponent()

        component.onFavoriteToggled()

        assertEquals(setOf(post3.id), favoritesStore.favorites.value)
        assertTrue(component.model.value.isFavorite)
    }

    @Test
    fun onBackClicked_goesBack() {
        val component = createComponent()

        component.onBackClicked()

        assertEquals(1, backClicks)
    }

    @Test
    fun survivesConfigurationChangeWithoutReloading() {
        val first = TestComponentContext()
        createComponent(first)
        repository.completePost(Result.Success(post3))

        val recreated = createComponent(TestComponentContext(instanceKeeper = first.instanceKeeper))

        assertEquals(listOf(post3.id), repository.postRequests)
        assertEquals(UiState.Success(post3), recreated.model.value.post)
    }
}
