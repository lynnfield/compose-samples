package com.example.jetnews.testing

import com.example.jetnews.data.Result
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post

/**
 * A [PostsRepository] that answers synchronously from [posts] and counts the calls.
 */
class FakePostsRepository(
    private val posts: List<Post> = testPosts,
    private val error: Exception? = null
) : PostsRepository {

    var getPostsCalls = 0
        private set

    override fun getPost(postId: String, callback: (Result<Post?>) -> Unit) {
        callback(error?.let { Result.Error(it) } ?: Result.Success(posts.find { it.id == postId }))
    }

    override fun getPosts(callback: (Result<List<Post>>) -> Unit) {
        getPostsCalls++
        callback(error?.let { Result.Error(it) } ?: Result.Success(posts))
    }
}

/**
 * A [PostsRepository] whose [getPosts] answers only when the test calls [completeGetPosts],
 * with the posts or with an error.
 */
class PendingPostsRepository(private val posts: List<Post> = testPosts) : PostsRepository {

    private var pending: ((Result<List<Post>>) -> Unit)? = null

    override fun getPost(postId: String, callback: (Result<Post?>) -> Unit) {
        callback(Result.Success(posts.find { it.id == postId }))
    }

    override fun getPosts(callback: (Result<List<Post>>) -> Unit) {
        pending = callback
    }

    fun completeGetPosts(error: Exception? = null) {
        val callback = pending!!
        pending = null
        callback(error?.let { Result.Error(it) } ?: Result.Success(posts))
    }
}

/**
 * An [InterestsRepository] that answers synchronously.
 */
class FakeInterestsRepository(
    private val topics: Map<String, List<String>> = mapOf("Android" to listOf("Kotlin")),
    private val people: List<String> = listOf("Person"),
    private val publications: List<String> = listOf("Publication"),
    private val peopleError: Exception? = null
) : InterestsRepository {

    override fun getTopics(callback: (Result<Map<String, List<String>>>) -> Unit) {
        callback(Result.Success(topics))
    }

    override fun getPeople(callback: (Result<List<String>>) -> Unit) {
        callback(peopleError?.let { Result.Error(it) } ?: Result.Success(people))
    }

    override fun getPublications(callback: (Result<List<String>>) -> Unit) {
        callback(Result.Success(publications))
    }
}
