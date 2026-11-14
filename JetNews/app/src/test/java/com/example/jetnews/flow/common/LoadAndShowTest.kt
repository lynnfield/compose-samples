package com.example.jetnews.flow.common

import com.example.jetnews.testing.RecordingAction
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.Publish
import com.genovich.components.ShowProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadAndShowTest {

    @Test
    fun showsLoadedData() = runTest {
        val show = RecordingAction<String, Int> { it.length }
        val showError = RecordingAction<Throwable, Int> { -1 }
        val loadAndShow = LoadAndShow(
            load = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.First("data") },
            show = show,
            showError = showError
        )

        assertEquals(4, loadAndShow(Unit))
        assertEquals(listOf("data"), show.inputs)
        assertTrue(showError.inputs.isEmpty())
    }

    @Test
    fun showsTheErrorWhenLoadingFails() = runTest {
        val exception = IllegalStateException("boom")
        val show = RecordingAction<String, Int> { it.length }
        val showError = RecordingAction<Throwable, Int> { -1 }
        val loadAndShow = LoadAndShow(
            load = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.Second(exception) },
            show = show,
            showError = showError
        )

        assertEquals(-1, loadAndShow(Unit))
        assertEquals(listOf<Throwable>(exception), showError.inputs)
        assertTrue(show.inputs.isEmpty())
    }

    @Test
    fun projectsLoadingThenContent() = runTest {
        val loaded = CompletableDeferred<OneOf<String, Throwable>>()
        val (show, showFlow) = showChannel<String, Unit>()
        val (loadAndShow, uiState) = LoadAndShow(
            load = ShowProgress(
                RecordingAction<Unit, OneOf<String, Throwable>> { loaded.await() },
                Publish<Progress>()
            ),
            show = show to showFlow,
            showError = showChannel<Throwable, Unit>()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { loadAndShow(Unit) }

        assertTrue(uiState.value is LoadAndShowUiState.Loading)

        loaded.complete(OneOf.First("data"))
        val content = uiState.value as LoadAndShowUiState.Content
        assertEquals("data", content.content.value.input)

        content.content.value.action(Unit)
        assertNull(uiState.value)
    }

    @Test
    fun projectsTheError() = runTest {
        val exception = IllegalStateException("boom")
        val (loadAndShow, uiState) = LoadAndShow(
            load = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.Second(exception) } to
                MutableStateFlow<Progress?>(null),
            show = showChannel<String, Unit>(),
            showError = showChannel<Throwable, Unit>()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { loadAndShow(Unit) }

        val error = uiState.value as LoadAndShowUiState.Error
        assertEquals(exception, error.error.value.input)
    }
}
