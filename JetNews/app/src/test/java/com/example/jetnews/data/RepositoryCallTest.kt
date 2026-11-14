package com.example.jetnews.data

import com.genovich.components.OneOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCallTest {

    @Test
    fun await_returnsFirstOnSuccess() = runTest {
        val call: RepositoryCall<String> = { callback -> callback(Result.Success("post")) }

        assertEquals(OneOf.First("post"), call.await())
    }

    @Test
    fun await_returnsSecondOnError() = runTest {
        val exception = IllegalStateException("boom")
        val call: RepositoryCall<String> = { callback -> callback(Result.Error(exception)) }

        assertEquals(OneOf.Second(exception), call.await())
    }

    @Test
    fun await_resumesWhenCallbackArrivesLater() = runTest {
        var pending: ((Result<String>) -> Unit)? = null
        val call: RepositoryCall<String> = { callback -> pending = callback }
        val deferred = async(UnconfinedTestDispatcher(testScheduler)) { call.await() }
        assertFalse(deferred.isCompleted)

        pending!!(Result.Success("late"))

        assertEquals(OneOf.First("late"), deferred.await())
    }

    @Test
    fun await_ignoresCallbackAfterCancellation() = runTest {
        var pending: ((Result<String>) -> Unit)? = null
        val call: RepositoryCall<String> = { callback -> pending = callback }
        val deferred = async(UnconfinedTestDispatcher(testScheduler), CoroutineStart.DEFAULT) {
            call.await()
        }

        deferred.cancel()
        pending!!(Result.Success("ignored"))

        assertTrue(deferred.isCancelled)
    }
}
