package com.example.jetnews.flow.common

import com.example.jetnews.testing.RecordingAction
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.Publish
import com.genovich.components.ShowProgress
import com.genovich.components.TryAgain
import com.genovich.components.TryAgainUiState
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
        val loadAndShow = LoadAndShow(
            load = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.First("data") },
            show = show
        )

        assertEquals(OneOf.First(4), loadAndShow(Unit))
        assertEquals(listOf("data"), show.inputs)
    }

    @Test
    fun returnsTheErrorWhenLoadingFails() = runTest {
        val exception = IllegalStateException("boom")
        val show = RecordingAction<String, Int> { it.length }
        val loadAndShow = LoadAndShow(
            load = RecordingAction<Unit, OneOf<String, Throwable>> { OneOf.Second(exception) },
            show = show
        )

        assertEquals(OneOf.Second(exception), loadAndShow(Unit))
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
            show = show to showFlow
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
    fun insideTryAgain_showsTheErrorAndLoadsAgainOnRetry() = runTest {
        val results = mutableListOf<OneOf<String, Throwable>>(
            OneOf.Second(IllegalStateException("boom")),
            OneOf.First("data")
        )
        val load = RecordingAction<Unit, OneOf<String, Throwable>> { results.removeAt(0) }
        val (retryable, uiState) = TryAgain(
            action = LoadAndShow(
                load = load to MutableStateFlow<Progress?>(null),
                show = showChannel<String, Unit>()
            ),
            showError = showChannel()
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { retryable(Unit) }

        val error = uiState.value as TryAgainUiState.Error
        assertEquals("boom", error.flow.value.input.message)

        error.flow.value.action(Unit)

        val content = (uiState.value as TryAgainUiState.Action).flow.value
            as LoadAndShowUiState.Content
        assertEquals("data", content.content.value.input)
        assertEquals(2, load.inputs.size)
    }
}
