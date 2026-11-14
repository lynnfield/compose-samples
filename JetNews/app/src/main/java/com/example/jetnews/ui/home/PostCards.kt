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

package com.example.jetnews.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.posts.impl.post3
import com.example.jetnews.model.Post
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors

@Composable
fun AuthorAndReadTime(post: Post) {
    Row {
        val textStyle = MaterialTheme.typography.body2
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(text = post.metadata.author.name, style = textStyle)
            Text(text = " - ${post.metadata.readTimeMinutes} min read", style = textStyle)
        }
    }
}

@Composable
fun PostImage(post: Post, modifier: Modifier = Modifier) {
    val image = post.imageThumb?.let { BitmapPainter(it) }
        ?: painterResource(R.drawable.placeholder_1_1)
    Image(image, contentDescription = null, modifier = modifier.size(40.dp, 40.dp))
}

@Composable
fun PostTitle(post: Post) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(post.title, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
fun PostCardSimple(
    post: Post,
    isFavorite: Boolean,
    onClick: (postId: String) -> Unit,
    onToggleFavorite: (postId: String) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick(post.id) })
            .padding(16.dp)
    ) {
        PostImage(post, Modifier.padding(end = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            PostTitle(post)
            AuthorAndReadTime(post)
        }
        BookmarkButton(
            isBookmarked = isFavorite,
            onBookmark = { onToggleFavorite(post.id) }
        )
    }
}

@Composable
fun PostCardHistory(post: Post, onClick: (postId: String) -> Unit) {
    Row(
        Modifier
            .clickable(onClick = { onClick(post.id) })
            .padding(16.dp)
    ) {
        PostImage(
            post,
            Modifier.padding(end = 16.dp)
        )
        Column(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    text = "BASED ON YOUR HISTORY",
                    style = MaterialTheme.typography.overline
                )
            }
            PostTitle(post = post)
            AuthorAndReadTime(post)
        }
        Image(painterResource(R.drawable.ic_more), contentDescription = null)
    }
}

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onBookmark: (Boolean) -> Unit
) {
    IconToggleButton(checked = isBookmarked, onCheckedChange = onBookmark) {
        Icon(
            painterResource(if (isBookmarked) R.drawable.ic_bookmarked else R.drawable.ic_bookmark),
            contentDescription = null
        )
    }
}

@Preview("Bookmark Button")
@Composable
fun PreviewBookmarkButton() {
    val (bookmarked, updateBookmarked) = remember { mutableStateOf(false) }
    BookmarkButton(isBookmarked = bookmarked, onBookmark = updateBookmarked)
}

@Preview("Simple post card")
@Composable
fun PreviewSimplePost() {
    ThemedPreview {
        PostCardSimple(post = post3, isFavorite = false, onClick = {}, onToggleFavorite = {})
    }
}

@Preview("History post card")
@Composable
fun PreviewHistoryPost() {
    ThemedPreview {
        PostCardHistory(post = post3, onClick = {})
    }
}

@Preview("Simple post card dark theme")
@Composable
fun PreviewSimplePostDark() {
    ThemedPreview(darkThemeColors) {
        PostCardSimple(post = post3, isFavorite = true, onClick = {}, onToggleFavorite = {})
    }
}
