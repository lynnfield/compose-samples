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

package com.example.jetnews.ui.interests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Colors
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.lightThemeColors

@Composable
fun SelectTopicButton(
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    if (selected) {
        SelectTopicButtonOn(modifier.size(36.dp, 36.dp))
    } else {
        SelectTopicButtonOff(modifier.size(36.dp, 36.dp))
    }
}

@Composable
private fun SelectTopicButtonOn(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colors.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null)
    }
}

@Composable
private fun SelectTopicButtonOff(modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = modifier.background(borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
    }
}

@Preview("Off")
@Composable
fun SelectTopicButtonPreviewOff() {
    SelectTopicButtonPreviewTemplate(
        lightThemeColors,
        false
    )
}

@Preview("On")
@Composable
fun SelectTopicButtonPreviewOn() {
    SelectTopicButtonPreviewTemplate(
        lightThemeColors,
        true
    )
}

@Preview("Off - dark theme")
@Composable
fun SelectTopicButtonPreviewOffDark() {
    SelectTopicButtonPreviewTemplate(
        darkThemeColors,
        false
    )
}

@Preview("On - dark theme")
@Composable
fun SelectTopicButtonPreviewOnDark() {
    SelectTopicButtonPreviewTemplate(
        darkThemeColors,
        true
    )
}

@Composable
private fun SelectTopicButtonPreviewTemplate(themeColors: Colors, selected: Boolean) {
    ThemedPreview(themeColors) {
        SelectTopicButton(
            modifier = Modifier.padding(32.dp),
            selected = selected
        )
    }
}
