package com.example.jetnews.flow

import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.flow.article.ShowArticleUiState
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.common.RetryableUiState
import com.example.jetnews.flow.home.ShowHomeUiState
import com.example.jetnews.model.Post
import com.example.jetnews.testing.FakeInterestsRepository
import com.example.jetnews.testing.PendingPostsRepository
import com.example.jetnews.testing.RecordingAction
import com.example.jetnews.testing.content
import com.example.jetnews.testing.error
import com.example.jetnews.testing.loaded
import com.example.jetnews.testing.testPosts
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives the whole app through its UI state only, the way the composables do.
 */
class JetNewsAssemblyTest {

    private val postsRepository = PendingPostsRepository()
    private val sharePost = RecordingAction<Post, Unit> { }

    private fun TestScope.startApp(): StateFlow<JetNewsUiState?> {
        val (app, uiState) = JetNewsAssembly(postsRepository, FakeInterestsRepository(), sharePost)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { app(Unit) }
        return uiState
    }

    private val StateFlow<JetNewsUiState?>.home get() = value as JetNewsUiState.Home

    private val StateFlow<JetNewsUiState?>.homeContent
        get() = home.home.value.content

    @Test
    fun showsLoadingThenHome() = runTest {
        val uiState = startApp()

        assertTrue(uiState.home.home.value.loaded is LoadAndShowUiState.Loading)

        postsRepository.completeGetPosts()
        val home = uiState.homeContent as ShowHomeUiState.Home
        assertEquals(testPosts, home.home.value.input.posts)
    }

    @Test
    fun aFailedLoadShowsTheErrorAndRetryLoadsAgain() = runTest {
        val uiState = startApp()

        postsRepository.completeGetPosts(IllegalStateException("offline"))
        val error = uiState.home.home.value.error
        assertEquals("offline", error.input.message)

        error.action(Unit)
        assertTrue(uiState.home.home.value.loaded is LoadAndShowUiState.Loading)

        postsRepository.completeGetPosts()
        assertTrue(uiState.homeContent is ShowHomeUiState.Home)
    }

    @Test
    fun articleBookmarkShowsOnHomeAfterBack() = runTest {
        val uiState = startApp()
        postsRepository.completeGetPosts()

        (uiState.homeContent as ShowHomeUiState.Home).openArticle.value.action("post1")
        val article = uiState.homeContent as ShowHomeUiState.Article<*>
        val content = (article.article.value as RetryableUiState<*>).content as ShowArticleUiState
        assertEquals(testPosts[1], content.article.value.input.post)

        content.article.value.action(Unit)
        content.share.value.action(testPosts[1])
        article.close.value.action(Unit)

        val home = uiState.homeContent as ShowHomeUiState.Home
        assertEquals(setOf("post1"), home.home.value.input.favorites)
        assertEquals(listOf(testPosts[1]), sharePost.inputs)
    }

    @Test
    fun drawerOpensInterestsEvenWhileHomeIsLoading() = runTest {
        val uiState = startApp()

        uiState.home.openInterests.value.action(Unit)

        val interests = uiState.value as JetNewsUiState.Interests
        assertTrue(interests.interests.value.loaded is LoadAndShowUiState.Content)

        interests.close.value.action(Unit)
        assertTrue(uiState.home.home.value.loaded is LoadAndShowUiState.Loading)

        postsRepository.completeGetPosts()
        assertTrue(uiState.homeContent is ShowHomeUiState.Home)
    }

    @Test
    fun topicSelectionSurvivesLeavingInterests() = runTest {
        val uiState = startApp()
        postsRepository.completeGetPosts()

        uiState.home.openInterests.value.action(Unit)
        val first = uiState.value as JetNewsUiState.Interests
        first.interests.value.content
            .action(TopicSelection("Topics-Android-Kotlin", selected = true))
        first.close.value.action(Unit)

        uiState.home.openInterests.value.action(Unit)
        val second = uiState.value as JetNewsUiState.Interests
        val state = second.interests.value.content.input
        assertEquals(setOf("Topics-Android-Kotlin"), state.selectedTopics)
    }
}
