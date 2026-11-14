package com.example.jetnews.data.posts

import com.example.jetnews.testing.FakePostsRepository
import com.example.jetnews.testing.testPosts
import com.genovich.components.OneOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostsLeavesTest {

    @Test
    fun loadPosts_returnsAllPosts() = runTest {
        assertEquals(OneOf.First(testPosts), LoadPosts(FakePostsRepository())(Unit))
    }

    @Test
    fun loadPost_returnsThePost() = runTest {
        assertEquals(OneOf.First(testPosts[2]), LoadPost(FakePostsRepository())("post2"))
    }

    @Test
    fun loadPost_missingPostIsAnError() = runTest {
        val result = LoadPost(FakePostsRepository())("missing")

        assertTrue((result as OneOf.Second).second is NoSuchElementException)
        assertEquals("postId doesn't exist", result.second.message)
    }

    @Test
    fun loadPost_passesRepositoryErrorsThrough() = runTest {
        val exception = IllegalStateException("boom")

        assertEquals(
            OneOf.Second(exception),
            LoadPost(FakePostsRepository(error = exception))("post1")
        )
    }
}
