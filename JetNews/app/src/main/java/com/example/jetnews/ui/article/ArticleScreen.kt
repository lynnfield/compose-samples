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

package com.example.jetnews.ui.article

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.flow.article.ArticleUiState
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.home.CloseArticleUiState
import com.example.jetnews.model.Post
import com.example.jetnews.ui.ErrorScreen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.home.BookmarkButton
import com.example.jetnews.ui.previewPost
import com.genovich.components.TryAgainUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders an open article from the program's UI state: a spinner while the post loads, then the
 * post, or the error with Retry. Back (the app bar arrow or system back) closes it.
 */
@Composable
fun ArticleRoute(
    article: StateFlow<ArticleUiState>,
    close: StateFlow<CloseArticleUiState>
) {
    val closeState by close.collectAsState()
    BackHandler { closeState.action(Unit) }
    val articleState by article.collectAsState()
    when (val retryable = articleState) {
        is TryAgainUiState.Error -> {
            val error by retryable.flow.collectAsState()
            ArticleStatusScreen(onBack = { closeState.action(Unit) }) {
                ErrorScreen(message = "Can't load the article", onRetry = { error.action(Unit) })
            }
        }
        is TryAgainUiState.Action -> {
            val loadedState by retryable.flow.collectAsState()
            val loaded = loadedState
            if (loaded is LoadAndShowUiState.Loading) {
                ArticleStatusScreen(onBack = { closeState.action(Unit) }) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else if (loaded is LoadAndShowUiState.Content) {
                val content by loaded.content.collectAsState()
                val showArticle by content.article.collectAsState()
                val share by content.share.collectAsState()
                ArticleScreen(
                    post = showArticle.input.post,
                    isFavorite = showArticle.input.isFavorite,
                    onBack = { closeState.action(Unit) },
                    onToggleFavorite = { showArticle.action(Unit) },
                    onShare = { share.action(share.input) }
                )
            }
        }
    }
}

@Composable
fun ArticleScreen(
    post: Post,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        FunctionalityNotAvailablePopup { showDialog = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Published in: ${post.publication?.name}",
                        style = MaterialTheme.typography.subtitle2.copy(
                            color = LocalContentColor.current
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            PostContent(post, Modifier.padding(innerPadding))
        },
        bottomBar = {
            BottomBar(
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onShare = onShare,
                onUnimplementedAction = { showDialog = true }
            )
        }
    )
}

/**
 * The article's app bar with Back, above something that isn't the post yet: the progress or
 * the error.
 */
@Composable
fun ArticleStatusScreen(
    onBack: () -> Unit,
    content: @Composable() BoxScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                content()
            }
        }
    )
}

@Composable
private fun BottomBar(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onUnimplementedAction: () -> Unit
) {
    Surface(elevation = 2.dp) {
        Box(modifier = Modifier.height(56.dp).fillMaxWidth()) {
            Row {
                BottomBarAction(R.drawable.ic_favorite) { onUnimplementedAction() }
                BookmarkButton(
                    isBookmarked = isFavorite,
                    onBookmark = { onToggleFavorite() }
                )
                BottomBarAction(R.drawable.ic_share) { onShare() }
                Spacer(modifier = Modifier.weight(1f))
                BottomBarAction(R.drawable.ic_text_settings) { onUnimplementedAction() }
            }
        }
    }
}

@Composable
private fun BottomBarAction(
    @DrawableRes id: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.padding(12.dp).size(24.dp, 24.dp)) {
        Icon(painterResource(id), contentDescription = null)
    }
}

@Composable
private fun FunctionalityNotAvailablePopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = "Functionality not available 🙈",
                style = MaterialTheme.typography.body2
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CLOSE")
            }
        }
    )
}

@Preview("Article screen")
@Composable
fun PreviewArticle() {
    ThemedPreview {
        ArticleScreen(
            post = previewPost(post3.id),
            isFavorite = false,
            onBack = {},
            onToggleFavorite = {},
            onShare = {}
        )
    }
}

@Preview("Article screen dark theme")
@Composable
fun PreviewArticleDark() {
    ThemedPreview(darkThemeColors) {
        ArticleScreen(
            post = previewPost(post3.id),
            isFavorite = false,
            onBack = {},
            onToggleFavorite = {},
            onShare = {}
        )
    }
}
