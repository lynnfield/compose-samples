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

package com.example.jetnews.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.flow.JetNewsUiState
import com.example.jetnews.ui.home.HomeRoute
import com.example.jetnews.ui.interests.InterestsRoute
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders the program's UI state. Decides nothing: each case of [JetNewsUiState] is one screen,
 * and each user action answers the T-function that is waiting for it.
 */
@Composable
fun JetnewsApp(uiState: StateFlow<JetNewsUiState?>) {

    MaterialTheme(
        colors = lightThemeColors,
        typography = themeTypography
    ) {
        AppContent(uiState)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppContent(uiState: StateFlow<JetNewsUiState?>) {
    val screen = uiState.collectLastNonNull() ?: return
    updateTransition(screen, label = "screen").Crossfade(contentKey = { it::class }) { current ->
        Surface(color = MaterialTheme.colors.background) {
            when (current) {
                is JetNewsUiState.Home -> HomeRoute(
                    home = current.home,
                    openInterests = current.openInterests
                )
                is JetNewsUiState.Interests -> InterestsRoute(
                    interests = current.interests,
                    close = current.close
                )
            }
        }
    }
}

/**
 * Between two cases a projection is briefly `null` (for example, the instant a feature closes
 * and before the screen under it is shown again). Keep rendering the last case for that instant.
 */
@Composable
fun <T : Any> StateFlow<T?>.collectLastNonNull(): T? {
    val value by collectAsState()
    val last = remember(this) { LastValue<T>() }
    value?.let { last.value = it }
    return last.value
}

private class LastValue<T : Any> {
    var value: T? = null
}

/**
 * The top-level destinations reachable from the drawer.
 */
enum class DrawerItem {
    Home,
    Interests
}

@Composable
fun AppDrawer(
    currentItem: DrawerItem,
    onHomeClicked: () -> Unit,
    onInterestsClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        JetNewsLogo(Modifier.padding(16.dp))
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = .2f))
        DrawerButton(
            icon = R.drawable.ic_home,
            label = "Home",
            isSelected = currentItem == DrawerItem.Home,
            action = onHomeClicked
        )

        DrawerButton(
            icon = R.drawable.ic_interests,
            label = "Interests",
            isSelected = currentItem == DrawerItem.Interests,
            action = onInterestsClicked
        )
    }
}

@Composable
private fun JetNewsLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.ic_jetnews_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.ic_jetnews_wordmark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface)
        )
    }
}

@Composable
private fun DrawerButton(
    @DrawableRes icon: Int,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colors
    val imageAlpha = if (isSelected) {
        1f
    } else {
        0.6f
    }
    val textIconColor = if (isSelected) {
        colors.primary
    } else {
        colors.onSurface.copy(alpha = 0.6f)
    }
    val backgroundColor = if (isSelected) {
        colors.primary.copy(alpha = 0.12f)
    } else {
        colors.surface
    }

    val surfaceModifier = modifier
        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
        .fillMaxWidth()
    Surface(
        modifier = surfaceModifier,
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(textIconColor),
                    alpha = imageAlpha
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.body2.copy(color = textIconColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview("Drawer contents")
@Composable
fun PreviewJetnewsApp() {
    ThemedPreview {
        AppDrawer(
            currentItem = DrawerItem.Home,
            onHomeClicked = { },
            onInterestsClicked = { }
        )
    }
}

@Preview("Drawer contents dark theme")
@Composable
fun PreviewJetnewsAppDark() {
    ThemedPreview(darkThemeColors) {
        AppDrawer(
            currentItem = DrawerItem.Home,
            onHomeClicked = { },
            onInterestsClicked = { }
        )
    }
}
