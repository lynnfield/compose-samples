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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.data.posts.impl.getPostsWithImagesLoaded
import com.example.jetnews.data.posts.impl.post2
import com.example.jetnews.data.posts.impl.posts
import com.example.jetnews.model.Post
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.lightThemeColors

@Composable
fun PostCardTop(post: Post, modifier: Modifier = Modifier) {
// TUTORIAL CONTENT STARTS HERE
    val typography = MaterialTheme.typography
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        post.image?.let { image ->
            val imageModifier = Modifier
                .heightIn(min = 180.dp)
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(4.dp))
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(16.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Text(
                text = post.title,
                style = typography.h6
            )
            Text(
                text = post.metadata.author.name,
                style = typography.body2
            )
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = "${post.metadata.date} - ${post.metadata.readTimeMinutes} min read",
                style = typography.body2
            )
        }
    }
}
// TUTORIAL CONTENT ENDS HERE

// Preview section

@Preview("Default colors")
@Composable
fun TutorialPreview() {
    TutorialPreviewTemplate()
}

@Preview("Dark theme")
@Composable
fun TutorialPreviewDark() {
    TutorialPreviewTemplate(colors = darkThemeColors)
}

@Preview("Font scaling 1.5", fontScale = 1.5f)
@Composable
fun TutorialPreviewFontscale() {
    TutorialPreviewTemplate()
}

@Composable
fun TutorialPreviewTemplate(
    colors: Colors = lightThemeColors
) {
    val context = LocalContext.current
    val previewPosts = getPostsWithImagesLoaded(posts.subList(1, 2), context.resources)
    val post = previewPosts[0]

    ThemedPreview(colors) {
        PostCardTop(post)
    }
}

@Preview("Post card top")
@Composable
fun PreviewPostCardTop() {
    ThemedPreview {
        PostCardTop(post = post2)
    }
}

@Preview("Post card top dark theme")
@Composable
fun PreviewPostCardTopDark() {
    ThemedPreview(darkThemeColors) {
        PostCardTop(post = post2)
    }
}
