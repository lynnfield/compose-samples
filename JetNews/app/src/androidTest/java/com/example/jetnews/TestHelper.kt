/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.example.jetnews.data.AppContainer
import com.example.jetnews.flow.JetNewsAssembly
import com.example.jetnews.platform.ActivityResultPort
import com.example.jetnews.platform.SharePost
import com.example.jetnews.ui.JetnewsApp

/**
 * Launches the app from a test context, with a fresh program and stores
 */
fun ComposeContentTestRule.launchJetNewsApp(context: Context) {
    // No random network errors, so the tests are deterministic
    val appContainer = AppContainer(context, postsFailureRate = 0f)
    val (program, uiState) = JetNewsAssembly(
        postsRepository = appContainer.postsRepository,
        interestsRepository = appContainer.interestsRepository,
        // Nothing attaches a share sheet in UI tests
        sharePost = SharePost(ActivityResultPort()),
        favoritesStore = appContainer.favoritesStore,
        selectedTopicsStore = appContainer.selectedTopicsStore
    )
    setContent {
        LaunchedEffect(program) { program(Unit) }
        JetnewsApp(uiState)
    }
}

/**
 * Helper method that can be used to test Jetnews UI Composables in isolation
 */
fun ComposeContentTestRule.setMaterialContent(children: @Composable() () -> Unit) {
    setContent {
        MaterialTheme {
            Surface {
                children()
            }
        }
    }
}

fun ComposeContentTestRule.findAllBySubstring(
    text: String,
    ignoreCase: Boolean = false
): List<SemanticsNodeInteraction> {
    val nodes = onAllNodes(hasText(text, substring = true, ignoreCase = ignoreCase))
    return List(nodes.fetchSemanticsNodes().size) { index -> nodes[index] }
}
