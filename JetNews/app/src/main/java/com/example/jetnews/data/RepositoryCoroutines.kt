package com.example.jetnews.data

import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until a callback-based repository call delivers its [Result].
 *
 * The callback repositories can't be cancelled, so a result that arrives after the calling
 * coroutine was cancelled is dropped.
 */
suspend fun <T> awaitResult(call: ((Result<T>) -> Unit) -> Unit): Result<T> =
    suspendCancellableCoroutine { continuation ->
        call { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
    }

suspend fun PostsRepository.awaitPost(postId: String): Result<Post?> =
    awaitResult { callback -> getPost(postId, callback) }

suspend fun PostsRepository.awaitPosts(): Result<List<Post>> = awaitResult(::getPosts)

suspend fun InterestsRepository.awaitTopics(): Result<Map<String, List<String>>> =
    awaitResult(::getTopics)

suspend fun InterestsRepository.awaitPeople(): Result<List<String>> = awaitResult(::getPeople)

suspend fun InterestsRepository.awaitPublications(): Result<List<String>> =
    awaitResult(::getPublications)
