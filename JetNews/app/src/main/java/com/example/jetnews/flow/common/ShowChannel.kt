package com.example.jetnews.flow.common

import com.genovich.components.Action
import com.genovich.components.Show
import com.genovich.components.UiStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A T-function: a [Show] together with the flow it publishes its `UiState(input, callback)` to.
 * The UI renders the state and answers by calling its callback.
 */
fun <Input, Output> showChannel(): Pair<Action<Input, Output>, UiStateFlow<Input, Output>> {
    val flow = MutableStateFlow<com.genovich.components.UiState<Input, Output>?>(null)
    return Show(flow) to flow
}
