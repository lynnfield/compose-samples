package com.example.jetnews.flow.common

import com.example.jetnews.testing.RecordingAction
import com.genovich.components.OneOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RefreshTest {

    private val boom = IllegalStateException("boom")

    @Test
    fun waitsForThePullThenReturnsTheReloadedData() = runTest {
        val reload = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.First("new") }
        val (refresh, uiState) = Refresh(
            waitForRefresh = showChannel(),
            reload = reload to MutableStateFlow(null),
            showError = showChannel<Throwable, Boolean>()
        )
        val result = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { refresh("old") }

        assertFalse(result.isCompleted)
        assertEquals(0, reload.inputs.size)

        (uiState.value as RefreshUiState.Idle).refresh.value.action(Unit)
        assertEquals("new", result.await())
    }

    @Test
    fun retriesUntilTheReloadSucceeds() = runTest {
        val results = mutableListOf<OneOf<String, Throwable>>(OneOf.Second(boom), OneOf.First("new"))
        val refresh = Refresh(
            waitForRefresh = RecordingAction<Unit, Unit> { },
            reload = RecordingAction<Unit, OneOf<String, Throwable>> { results.removeAt(0) },
            showError = RecordingAction<Throwable, Boolean> { true }
        )

        assertEquals("new", refresh("old"))
    }

    @Test
    fun keepsTheOldDataWhenTheErrorIsDismissed() = runTest {
        val showError = RecordingAction<Throwable, Boolean> { false }
        val refresh = Refresh(
            waitForRefresh = RecordingAction<Unit, Unit> { },
            reload = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.Second(boom) },
            showError = showError
        )

        assertEquals("old", refresh("old"))
        assertEquals(listOf<Throwable>(boom), showError.inputs)
    }
}
