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

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.data.posts.impl.PreviewPostsRepository
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.data.successOr
import com.example.jetnews.model.Post
import com.example.jetnews.ui.Screen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.effect.fetchPost
import com.example.jetnews.ui.home.BookmarkButton
import com.example.jetnews.ui.home.isFavorite
import com.example.jetnews.ui.home.toggleBookmark
import com.example.jetnews.ui.navigateTo

@Composable
fun ArticleScreen(postId: String, postsRepository: PostsRepository) {
    val postsState = fetchPost(postId, postsRepository)
    if (postsState is UiState.Success<Post>) {
        ArticleScreen(postsState.data)
    }
}

@Composable
private fun ArticleScreen(post: Post) {

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
                    IconButton(onClick = { navigateTo(Screen.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            PostContent(post, Modifier.padding(innerPadding))
        },
        bottomBar = {
            BottomBar(post) { showDialog = true }
        }
    )
}

@Composable
private fun BottomBar(post: Post, onUnimplementedAction: () -> Unit) {
    val context = LocalContext.current
    Surface(elevation = 2.dp) {
        Box(modifier = Modifier.height(56.dp).fillMaxWidth()) {
            Row {
                BottomBarAction(R.drawable.ic_favorite) { onUnimplementedAction() }
                BookmarkButton(
                    isBookmarked = isFavorite(postId = post.id),
                    onBookmark = { toggleBookmark(postId = post.id) }
                )
                BottomBarAction(R.drawable.ic_share) { sharePost(post, context) }
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

private fun sharePost(post: Post, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, post.title)
        putExtra(Intent.EXTRA_TEXT, post.url)
    }
    context.startActivity(Intent.createChooser(intent, "Share post"))
}

@Preview("Article screen")
@Composable
fun PreviewArticle() {
    ThemedPreview {
        val post = loadFakePost(post3.id)
        ArticleScreen(post)
    }
}

@Preview("Article screen dark theme")
@Composable
fun PreviewArticleDark() {
    ThemedPreview(darkThemeColors) {
        val post = loadFakePost(post3.id)
        ArticleScreen(post)
    }
}

@Composable
private fun loadFakePost(postId: String): Post {
    var post: Post? = null
    PreviewPostsRepository(LocalContext.current).getPost(postId) { result ->
        post = result.successOr(null)
    }
    return post!!
}
