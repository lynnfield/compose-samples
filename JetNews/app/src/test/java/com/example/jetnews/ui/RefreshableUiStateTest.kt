package com.example.jetnews.ui

import com.example.jetnews.data.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshableUiStateTest {

    private val exception = IllegalStateException("boom")

    @Test
    fun loadingWithoutData_isLoadingNotRefreshing() {
        val state = RefreshableUiState.Success<String>(data = null, loading = true)

        assertTrue(state.loading)
        assertFalse(state.refreshing)
        assertNull(state.currentData)
    }

    @Test
    fun loadingWithData_isRefreshingNotLoading() {
        val state = RefreshableUiState.Success(data = "a", loading = true)

        assertFalse(state.loading)
        assertTrue(state.refreshing)
        assertEquals("a", state.currentData)
    }

    @Test
    fun error_keepsPreviousData() {
        val state = RefreshableUiState.Error(exception, previousData = "a")

        assertFalse(state.loading)
        assertFalse(state.refreshing)
        assertEquals("a", state.currentData)
    }

    @Test
    fun toLoading_keepsCurrentData() {
        assertEquals(
            RefreshableUiState.Success(data = "a", loading = true),
            RefreshableUiState.Error(exception, previousData = "a").toLoading()
        )
        assertEquals(
            RefreshableUiState.Success<String>(data = null, loading = true),
            RefreshableUiState.Success<String>(data = null, loading = false).toLoading()
        )
    }

    @Test
    fun withResult_successReplacesData() {
        val state = RefreshableUiState.Success(data = "a", loading = true)

        assertEquals(
            RefreshableUiState.Success(data = "b", loading = false),
            state.withResult(Result.Success("b"))
        )
    }

    @Test
    fun withResult_errorKeepsData() {
        val state = RefreshableUiState.Success(data = "a", loading = true)

        assertEquals(
            RefreshableUiState.Error(exception, previousData = "a"),
            state.withResult(Result.Error(exception))
        )
    }
}
