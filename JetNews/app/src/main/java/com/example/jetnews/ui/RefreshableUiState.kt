package com.example.jetnews.ui

import com.example.jetnews.data.Result

/**
 * Model for UiStates that can refresh. The Success state contains whether there's data loading
 * apart from the current data to display on the screen. The error state also returns previously
 * loaded data apart from the error.
 */
sealed class RefreshableUiState<out T> {
    data class Success<out T>(val data: T?, val loading: Boolean) : RefreshableUiState<T>()
    data class Error<out T>(val exception: Exception, val previousData: T?) :
        RefreshableUiState<T>()
}

val <T> RefreshableUiState<T>.loading: Boolean
    get() = this is RefreshableUiState.Success && this.loading && this.data == null

val <T> RefreshableUiState<T>.refreshing: Boolean
    get() = this is RefreshableUiState.Success && this.loading && this.data != null

val <T> RefreshableUiState<T>.currentData: T?
    get() = when (this) {
        is RefreshableUiState.Success -> this.data
        is RefreshableUiState.Error -> this.previousData
    }

/**
 * The state while a (re)load is in flight: keeps showing the current data.
 */
fun <T> RefreshableUiState<T>.toLoading(): RefreshableUiState<T> =
    RefreshableUiState.Success(data = currentData, loading = true)

/**
 * The state after a (re)load finished with [result]. An error keeps the current data.
 */
fun <T> RefreshableUiState<T>.withResult(result: Result<T>): RefreshableUiState<T> =
    when (result) {
        is Result.Success -> RefreshableUiState.Success(data = result.data, loading = false)
        is Result.Error -> RefreshableUiState.Error(
            exception = result.exception,
            previousData = currentData
        )
    }
