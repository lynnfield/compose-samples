package com.example.jetnews.data

import com.genovich.components.OneOf
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A callback-based repository call, e.g. `postsRepository::getPosts`.
 */
typealias RepositoryCall<T> = ((Result<T>) -> Unit) -> Unit

/**
 * Suspends until the repository delivers its [Result], as [OneOf.First] on success or
 * [OneOf.Second] with the exception on failure.
 *
 * The callback repositories can't be cancelled, so a result that arrives after the calling
 * coroutine was cancelled is dropped.
 */
suspend fun <T> RepositoryCall<T>.await(): OneOf<T, Throwable> =
    suspendCancellableCoroutine { continuation ->
        this { result ->
            if (continuation.isActive) {
                continuation.resume(
                    when (result) {
                        is Result.Success -> OneOf.First(result.data)
                        is Result.Error -> OneOf.Second(result.exception)
                    }
                )
            }
        }
    }
