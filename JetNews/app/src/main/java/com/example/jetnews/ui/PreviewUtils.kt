/*
 * Copyright 2020 Google, Inc.
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

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.jetnews.data.RepositoryCall
import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.impl.PreviewPostsRepository
import com.example.jetnews.model.Post

@Composable
internal fun ThemedPreview(
    colors: Colors = lightThemeColors,
    typography: Typography = themeTypography,
    children: @Composable() () -> Unit
) {
    MaterialTheme(colors = colors, typography = typography) {
        Surface {
            children()
        }
    }
}

/**
 * Reads the data of a repository that answers synchronously. Only use in Previews!
 */
fun <T> previewData(call: RepositoryCall<T>): T {
    var data: T? = null
    call { result -> data = (result as Result.Success).data }
    @Suppress("UNCHECKED_CAST")
    return data as T
}

/**
 * All posts with their images loaded. Only use in Previews!
 */
@Composable
fun previewPosts(): List<Post> {
    val context = LocalContext.current
    return remember(context) { previewData(PreviewPostsRepository(context)::getPosts) }
}

/**
 * A post with its images loaded. Only use in Previews!
 */
@Composable
fun previewPost(postId: String): Post {
    val context = LocalContext.current
    return remember(context, postId) {
        previewData<Post?> { callback -> PreviewPostsRepository(context).getPost(postId, callback) }!!
    }
}
