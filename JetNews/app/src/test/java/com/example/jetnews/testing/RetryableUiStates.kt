package com.example.jetnews.testing

import com.example.jetnews.flow.common.ErrorUiState
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.common.RetryableUiState
import com.genovich.components.TryAgainUiState

/**
 * What a [RetryableUiState] shows when it isn't the error: loading or the content.
 */
val <T : Any> RetryableUiState<T>.loaded: LoadAndShowUiState<T>
    get() = (this as TryAgainUiState.Action).flow.value

/**
 * The content of a [RetryableUiState] that has loaded.
 */
val <T : Any> RetryableUiState<T>.content: T
    get() = (loaded as LoadAndShowUiState.Content).content.value

/**
 * The error of a [RetryableUiState] whose load failed.
 */
val RetryableUiState<*>.error: ErrorUiState
    get() = (this as TryAgainUiState.Error).flow.value
