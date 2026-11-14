package com.example.jetnews.testing

import com.genovich.components.Action

/**
 * An [Action] that records its inputs and answers with [answer].
 */
class RecordingAction<Input, Output>(
    private val answer: suspend (Input) -> Output
) : Action<Input, Output>() {

    val inputs = mutableListOf<Input>()

    override suspend fun invoke(input: Input): Output {
        inputs += input
        return answer(input)
    }
}
