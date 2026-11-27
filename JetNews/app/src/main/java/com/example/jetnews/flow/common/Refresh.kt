package com.example.jetnews.flow.common

import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.updateLoopUntil
import kotlinx.coroutines.flow.StateFlow

/**
 * Pull to refresh for data that is already on screen.
 *
 * Waits for [waitForRefresh] (the user pulled), then runs [reload] and returns the new data. If
 * reloading fails, [showError] shows the error and answers whether to try again (Retry) or to
 * give up and keep the [input][invoke] data (dismissed).
 */
class Refresh<Data>(
    val waitForRefresh: Action<Unit, Unit>,
    val reload: Action<Unit, OneOf<Data, Throwable>>,
    val showError: Action<Throwable, Boolean>
) : Action<Data, Data>() {

    override suspend fun invoke(input: Data): Data {
        waitForRefresh(Unit)
        return updateLoopUntil(Unit) {
            when (val reloaded = reload(Unit)) {
                is OneOf.First -> OneOf.Second(reloaded.first)
                is OneOf.Second ->
                    if (showError(reloaded.second)) OneOf.First(Unit) else OneOf.Second(input)
            }
        }
    }
}

/**
 * What [Refresh] wants on screen.
 */
sealed class RefreshUiState {

    /**
     * Waiting for the user to pull.
     */
    class Idle(val refresh: StateFlow<UiState<Unit, Unit>>) : RefreshUiState()

    /**
     * Reloading.
     */
    class Refreshing(val progress: StateFlow<Progress>) : RefreshUiState()

    /**
     * Reloading failed: answer `true` to try again, `false` to keep the old data.
     */
    class Error(val error: StateFlow<UiState<Throwable, Boolean>>) : RefreshUiState()
}

/**
 * [Refresh] wired with its projection: the error wins over the progress, and the progress wins
 * over waiting.
 */
fun <Data> Refresh(
    waitForRefresh: Pair<Action<Unit, Unit>, UiStateFlow<Unit, Unit>>,
    reload: Pair<Action<Unit, OneOf<Data, Throwable>>, StateFlow<Progress?>>,
    showError: Pair<Action<Throwable, Boolean>, UiStateFlow<Throwable, Boolean>>
): Pair<Action<Data, Data>, StateFlow<RefreshUiState?>> =
    Refresh(waitForRefresh.first, reload.first, showError.first) to combine(
        flow = waitForRefresh.second.emitSelfWhenHaveValue(),
        flow2 = reload.second.emitSelfWhenHaveValue(),
        flow3 = showError.second.emitSelfWhenHaveValue(),
        transform = { wait, progress, error ->
            when {
                error != null -> RefreshUiState.Error(error)
                progress != null -> RefreshUiState.Refreshing(progress)
                wait != null -> RefreshUiState.Idle(wait)
                else -> null
            }
        }
    )
