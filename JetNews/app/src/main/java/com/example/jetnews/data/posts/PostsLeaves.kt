package com.example.jetnews.data.posts

import com.example.jetnews.data.RepositoryCall
import com.example.jetnews.data.await
import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OneOf

/**
 * Loads all posts from [repository].
 */
class LoadPosts(
    val repository: PostsRepository
) : Action<Unit, OneOf<List<Post>, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<List<Post>, Throwable> {
        val call: RepositoryCall<List<Post>> = repository::getPosts
        return call.await()
    }
}

/**
 * Loads the post with the given id from [repository]. A post that doesn't exist is an error.
 */
class LoadPost(
    val repository: PostsRepository
) : Action<String, OneOf<Post, Throwable>>() {

    override suspend fun invoke(input: String): OneOf<Post, Throwable> {
        val call: RepositoryCall<Post?> = { callback -> repository.getPost(input, callback) }
        return when (val result = call.await()) {
            is OneOf.First -> result.first
                ?.let { OneOf.First(it) }
                ?: OneOf.Second(NoSuchElementException("postId doesn't exist"))
            is OneOf.Second -> result
        }
    }
}
