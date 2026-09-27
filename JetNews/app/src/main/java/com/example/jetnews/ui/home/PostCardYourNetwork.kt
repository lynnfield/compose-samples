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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.posts.impl.post1
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostAuthor
import com.example.jetnews.ui.Screen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.navigateTo

@Composable
fun PostCardPopular(post: Post, modifier: Modifier = Modifier) {
    Card(modifier = modifier.size(280.dp, 240.dp), shape = RoundedCornerShape(4.dp)) {
        Column(modifier = Modifier.clickable(onClick = { navigateTo(Screen.Article(post.id)) })) {
            val image = post.image?.let { BitmapPainter(it) }
                ?: painterResource(R.drawable.placeholder_4_3)
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.height(100.dp).fillMaxWidth()
            )
            Column(modifier = Modifier.padding(16.dp)) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.h6,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = post.metadata.author.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.body2
                    )
                }
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                    Text(
                        text = "${post.metadata.date} - " +
                                "${post.metadata.readTimeMinutes} min read",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Preview("Regular colors")
@Composable
fun PreviewPostCardPopular() {
    ThemedPreview {
        PostCardPopular(post = post1)
    }
}

@Preview("Dark colors")
@Composable
fun PreviewPostCardPopularDark() {
    ThemedPreview(darkThemeColors) {
        PostCardPopular(post = post1)
    }
}

@Preview("Regular colors, long text")
@Composable
fun PreviewPostCardPopularLongText() {
    val loremIpsum = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ullamcorper pharetra massa,
        sed suscipit nunc mollis in. Sed tincidunt orci lacus, vel ullamcorper nibh congue quis.
        Etiam imperdiet facilisis ligula id facilisis. Suspendisse potenti. Cras vehicula neque sed
        nulla auctor scelerisque. Vestibulum at congue risus, vel aliquet eros. In arcu mauris,
        facilisis eget magna quis, rhoncus volutpat mi. Phasellus vel sollicitudin quam, eu
        consectetur dolor. Proin lobortis venenatis sem, in vestibulum est. Duis ac nibh interdum,
    """.trimIndent()
    ThemedPreview {
        PostCardPopular(
            post = post1.copy(
                title = "Title$loremIpsum",
                metadata = post1.metadata.copy(
                    author = PostAuthor("Author: $loremIpsum"),
                    readTimeMinutes = Int.MAX_VALUE
                )
            )
        )
    }
}
