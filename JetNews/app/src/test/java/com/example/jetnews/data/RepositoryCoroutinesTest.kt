package com.example.jetnews.data

import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.toUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryCoroutinesTest {

    @Test
    fun awaitResult_returnsSuccess() = runTest {
        val result = awaitResult<String> { callback -> callback(Result.Success("post")) }

        assertEquals(Result.Success("post"), result)
    }

    @Test
    fun awaitResult_returnsError() = runTest {
        val exception = IllegalStateException("boom")

        val result = awaitResult<String> { callback -> callback(Result.Error(exception)) }

        assertEquals(Result.Error(exception), result)
    }

    @Test
    fun awaitResult_resumesWhenCallbackArrivesLater() = runTest {
        var pending: ((Result<String>) -> Unit)? = null
        val deferred = async(UnconfinedTestDispatcher(testScheduler)) {
            awaitResult<String> { callback -> pending = callback }
        }
        assertFalse(deferred.isCompleted)

        pending!!(Result.Success("late"))

        assertEquals(Result.Success("late"), deferred.await())
    }

    @Test
    fun awaitResult_ignoresCallbackAfterCancellation() = runTest {
        var pending: ((Result<String>) -> Unit)? = null
        val deferred = async(UnconfinedTestDispatcher(testScheduler), CoroutineStart.DEFAULT) {
            awaitResult<String> { callback -> pending = callback }
        }

        deferred.cancel()
        pending!!(Result.Success("ignored"))

        assertTrue(deferred.isCancelled)
    }

    @Test
    fun toUiState_mapsSuccessAndError() {
        val exception = IllegalStateException("boom")

        assertEquals(UiState.Success(1), Result.Success(1).toUiState())
        assertEquals(UiState.Error(exception), Result.Error(exception).toUiState())
    }
}
