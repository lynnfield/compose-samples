package com.example.jetnews.ui.root

import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.testing.ControllablePostsRepository
import com.example.jetnews.testing.TestComponentContext
import com.example.jetnews.ui.root.RootComponent.Child
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRootComponentTest {

    private val postsRepository = ControllablePostsRepository()
    private val testContext = TestComponentContext()

    private fun createRoot(context: TestComponentContext = testContext) = DefaultRootComponent(
        componentContext = context.context,
        postsRepository = postsRepository,
        interestsRepository = FakeInterestsRepository(),
        favoritesStore = FavoritesStore(),
        selectedTopicsStore = SelectedTopicsStore(),
        mainContext = UnconfinedTestDispatcher()
    )

    private val RootComponent.active get() = stack.value.active.instance
    private val RootComponent.depth get() = stack.value.items.size

    @Test
    fun startsOnHome() {
        val root = createRoot()

        assertTrue(root.active is Child.Home)
        assertEquals(1, root.depth)
    }

    @Test
    fun openingAPostPushesTheArticle() {
        val root = createRoot()

        (root.active as Child.Home).component.onPostClicked("x")

        assertTrue(root.active is Child.Article)
        assertEquals(2, root.depth)
        assertEquals(listOf("x"), postsRepository.postRequests)
    }

    @Test
    fun openingTheSamePostTwiceKeepsOneArticle() {
        val root = createRoot()
        val home = (root.active as Child.Home).component

        home.onPostClicked("x")
        home.onPostClicked("x")

        assertEquals(2, root.depth)
    }

    @Test
    fun systemBackReturnsFromArticleToHome() {
        val root = createRoot()
        val home = root.active
        (home as Child.Home).component.onPostClicked("x")

        assertTrue(testContext.backDispatcher.back())

        assertSame(home, root.active)
        assertEquals(1, root.depth)
    }

    @Test
    fun articleBackArrowReturnsToHome() {
        val root = createRoot()
        (root.active as Child.Home).component.onPostClicked("x")

        (root.active as Child.Article).component.onBackClicked()

        assertTrue(root.active is Child.Home)
    }

    @Test
    fun systemBackOnHomeIsNotHandled() {
        createRoot()

        assertFalse(testContext.backDispatcher.back())
    }

    @Test
    fun drawerNavigatesBetweenHomeAndInterests() {
        val root = createRoot()
        val home = root.active

        root.onInterestsClicked()
        assertTrue(root.active is Child.Interests)
        assertEquals(2, root.depth)

        root.onHomeClicked()
        assertSame(home, root.active)
        assertEquals(1, root.depth)
    }

    @Test
    fun systemBackReturnsFromInterestsToHome() {
        val root = createRoot()
        root.onInterestsClicked()

        assertTrue(testContext.backDispatcher.back())

        assertTrue(root.active is Child.Home)
    }

    @Test
    fun openingInterestsTwiceKeepsOneInterests() {
        val root = createRoot()

        root.onInterestsClicked()
        root.onInterestsClicked()

        assertEquals(2, root.depth)
    }

    @Test
    fun stackIsRestoredAfterProcessDeath() {
        val root = createRoot()
        (root.active as Child.Home).component.onPostClicked("x")
        val savedState = testContext.stateKeeper.save()

        postsRepository.postRequests.clear()
        val restored = createRoot(
            TestComponentContext(stateKeeper = StateKeeperDispatcher(savedState))
        )

        assertTrue(restored.active is Child.Article)
        assertEquals(2, restored.depth)
        assertEquals(listOf("x"), postsRepository.postRequests)
    }
}
