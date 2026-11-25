package com.example.jetnews.platform

import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityResultPortTest {

    private val port = ActivityResultPort<String>()
    private val launched = mutableListOf<String>()
    private val launcher: (String) -> Unit = { launched += it }

    @Test
    fun suspendsUntilTheActivityReportsTheResult() = runTest {
        port.attach(launcher)

        val share = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            port.launchAndAwait("post")
        }
        assertEquals(listOf("post"), launched)
        assertFalse(share.isCompleted)

        port.onResult()
        assertTrue(share.isCompleted)
    }

    @Test
    fun waitsForALauncherToBeAttached() = runTest {
        val share = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            port.launchAndAwait("post")
        }
        assertTrue(launched.isEmpty())

        port.attach(launcher)
        assertEquals(listOf("post"), launched)
        assertFalse(share.isCompleted)
    }

    @Test
    fun aResultInTheRecreatedActivityResumesTheCaller() = runTest {
        port.attach(launcher)
        val share = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            port.launchAndAwait("post")
        }

        // Rotation: the old Activity detaches, the new one attaches and gets the result
        port.detach(launcher)
        val recreated: (String) -> Unit = { }
        port.attach(recreated)
        port.onResult()

        assertTrue(share.isCompleted)
    }

    @Test
    fun aStaleDetachKeepsTheNewLauncher() = runTest {
        val recreated: (String) -> Unit = { launched += "recreated:$it" }
        port.attach(launcher)
        port.attach(recreated)

        port.detach(launcher)
        backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { port.launchAndAwait("post") }

        assertEquals(listOf("recreated:post"), launched)
    }
}
