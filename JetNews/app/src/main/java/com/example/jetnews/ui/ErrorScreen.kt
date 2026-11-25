package com.example.jetnews.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * A load that failed: a message and a Retry button, in the middle of the screen.
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.body1)
        TextButton(onClick = onRetry) {
            Text(text = "RETRY")
        }
    }
}

@Preview("Error screen")
@Composable
fun PreviewErrorScreen() {
    ThemedPreview {
        ErrorScreen(message = "Can't load the article", onRetry = {})
    }
}
