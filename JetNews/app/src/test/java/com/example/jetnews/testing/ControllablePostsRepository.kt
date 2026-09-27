package com.example.jetnews.testing

import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post

/**
 * A [PostsRepository] that holds every callback until the test completes it.
 */
class ControllablePostsRepository : PostsRepository {

    private val pendingPosts = mutableListOf<(Result<List<Post>>) -> Unit>()
    private val pendingPost = mutableListOf<(Result<Post?>) -> Unit>()

    var postsRequests = 0
        private set
    val postRequests = mutableListOf<String>()

    override fun getPosts(callback: (Result<List<Post>>) -> Unit) {
        postsRequests++
        pendingPosts += callback
    }

    override fun getPost(postId: String, callback: (Result<Post?>) -> Unit) {
        postRequests += postId
        pendingPost += callback
    }

    fun completePosts(result: Result<List<Post>>) {
        val callbacks = pendingPosts.toList()
        pendingPosts.clear()
        callbacks.forEach { it(result) }
    }

    fun completePost(result: Result<Post?>) {
        val callbacks = pendingPost.toList()
        pendingPost.clear()
        callbacks.forEach { it(result) }
    }
}
