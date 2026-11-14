package com.example.jetnews.flow.common

import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import kotlinx.coroutines.flow.StateFlow

/**
 * Loads data with [load], then shows it with [show]. If loading fails, the error goes to
 * [showError] instead.
 */
class LoadAndShow<Input, Data, Output>(
    val load: Action<Input, OneOf<Data, Throwable>>,
    val show: Action<Data, Output>,
    val showError: Action<Throwable, Output>
) : Action<Input, Output>() {

    override suspend fun invoke(input: Input): Output =
        when (val loaded = load(input)) {
            is OneOf.First -> show(loaded.first)
            is OneOf.Second -> showError(loaded.second)
        }
}

/**
 * What [LoadAndShow] wants on screen: the progress while loading, then the content, or the error.
 */
sealed class LoadAndShowUiState<out ContentUiState : Any, out ErrorUiState : Any> {
    class Loading(val progress: StateFlow<Progress>) : LoadAndShowUiState<Nothing, Nothing>()
    class Content<ContentUiState : Any>(
        val content: StateFlow<ContentUiState>
    ) : LoadAndShowUiState<ContentUiState, Nothing>()
    class Error<ErrorUiState : Any>(
        val error: StateFlow<ErrorUiState>
    ) : LoadAndShowUiState<Nothing, ErrorUiState>()
}

/**
 * [LoadAndShow] wired with the projection of its children's UI states. The error wins over the
 * content, and the content wins over the progress.
 */
fun <Input, Data, Output, ContentUiState : Any, ErrorUiState : Any> LoadAndShow(
    load: Pair<Action<Input, OneOf<Data, Throwable>>, StateFlow<Progress?>>,
    show: Pair<Action<Data, Output>, StateFlow<ContentUiState?>>,
    showError: Pair<Action<Throwable, Output>, StateFlow<ErrorUiState?>>
): Pair<Action<Input, Output>, StateFlow<LoadAndShowUiState<ContentUiState, ErrorUiState>?>> =
    LoadAndShow(load.first, show.first, showError.first) to combine(
        flow = load.second.emitSelfWhenHaveValue(),
        flow2 = show.second.emitSelfWhenHaveValue(),
        flow3 = showError.second.emitSelfWhenHaveValue(),
        transform = { progress, content, error ->
            when {
                error != null -> LoadAndShowUiState.Error(error)
                content != null -> LoadAndShowUiState.Content(content)
                progress != null -> LoadAndShowUiState.Loading(progress)
                else -> null
            }
        }
    )
