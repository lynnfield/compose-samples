package com.example.jetnews.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Starts something in the Activity and suspends until the Activity reports its result.
 *
 * The program runs in the ViewModel and outlives the Activity, so the Activity [attach]es its
 * launcher when it's created, [detach]es it when it's destroyed, and calls [onResult] from its
 * result callback. A result that arrives in a recreated Activity (after a rotation) still
 * resumes the caller. A launch waits until a launcher is attached.
 */
class ActivityResultPort<Input> {

    private val launcher = MutableStateFlow<((Input) -> Unit)?>(null)
    private var pending: CompletableDeferred<Unit>? = null

    fun attach(launch: (Input) -> Unit) {
        launcher.value = launch
    }

    fun detach(launch: (Input) -> Unit) {
        launcher.compareAndSet(launch, null)
    }

    fun onResult() {
        pending?.complete(Unit)
        pending = null
    }

    suspend fun launchAndAwait(input: Input) {
        val launch = launcher.filterNotNull().first()
        val result = CompletableDeferred<Unit>()
        pending = result
        launch(input)
        result.await()
    }
}
