package com.example.jetnews.flow.common

import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.TryAgainUiState
import com.genovich.components.UiState
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import kotlinx.coroutines.flow.StateFlow

/**
 * Loads data with [load], then shows it with [show]. If loading fails, the error is returned
 * instead, so a [com.genovich.components.TryAgain] around it can show the error and run it again.
 */
class LoadAndShow<Input, Data, Output>(
    val load: Action<Input, OneOf<Data, Throwable>>,
    val show: Action<Data, Output>
) : Action<Input, OneOf<Output, Throwable>>() {

    override suspend fun invoke(input: Input): OneOf<Output, Throwable> =
        when (val loaded = load(input)) {
            is OneOf.First -> OneOf.First(show(loaded.first))
            is OneOf.Second -> loaded
        }
}

/**
 * What [LoadAndShow] wants on screen: the progress while loading, then the content.
 */
sealed class LoadAndShowUiState<out ContentUiState : Any> {
    class Loading(val progress: StateFlow<Progress>) : LoadAndShowUiState<Nothing>()
    class Content<ContentUiState : Any>(
        val content: StateFlow<ContentUiState>
    ) : LoadAndShowUiState<ContentUiState>()
}

/**
 * [LoadAndShow] wired with the projection of its children's UI states. The content wins over the
 * progress.
 */
fun <Input, Data, Output, ContentUiState : Any> LoadAndShow(
    load: Pair<Action<Input, OneOf<Data, Throwable>>, StateFlow<Progress?>>,
    show: Pair<Action<Data, Output>, StateFlow<ContentUiState?>>
): Pair<Action<Input, OneOf<Output, Throwable>>, StateFlow<LoadAndShowUiState<ContentUiState>?>> =
    LoadAndShow(load.first, show.first) to combine(
        flow = load.second.emitSelfWhenHaveValue(),
        flow2 = show.second.emitSelfWhenHaveValue(),
        transform = { progress, content ->
            when {
                content != null -> LoadAndShowUiState.Content(content)
                progress != null -> LoadAndShowUiState.Loading(progress)
                else -> null
            }
        }
    )

/**
 * The error T-function of a [com.genovich.components.TryAgain]: shows the error and is answered
 * by Retry.
 */
typealias ErrorUiState = UiState<Throwable, Unit>

/**
 * A [LoadAndShow] inside a [com.genovich.components.TryAgain]: loading, the content, or the
 * error with Retry.
 */
typealias RetryableUiState<ContentUiState> =
    TryAgainUiState<LoadAndShowUiState<ContentUiState>, ErrorUiState>
