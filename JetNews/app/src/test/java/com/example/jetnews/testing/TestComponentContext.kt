package com.example.jetnews.testing

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher

/**
 * A resumed [DefaultComponentContext] whose keepers and back handler are exposed to the test.
 *
 * Creating a second context with the same [instanceKeeper] simulates a configuration change.
 */
class TestComponentContext(
    val instanceKeeper: InstanceKeeperDispatcher = InstanceKeeperDispatcher(),
    val stateKeeper: StateKeeperDispatcher = StateKeeperDispatcher(),
    val backDispatcher: BackDispatcher = BackDispatcher()
) {
    val lifecycle = LifecycleRegistry()

    val context = DefaultComponentContext(
        lifecycle = lifecycle,
        stateKeeper = stateKeeper,
        instanceKeeper = instanceKeeper,
        backHandler = backDispatcher
    )

    init {
        lifecycle.resume()
    }
}
