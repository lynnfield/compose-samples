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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.jetnews.R
import com.example.jetnews.data.posts.impl.posts
import com.example.jetnews.model.Post
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.DrawerItem
import com.example.jetnews.ui.PreviewHomeComponent
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.previewPosts
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    component: HomeComponent,
    onHomeClicked: () -> Unit,
    onInterestsClicked: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val model by component.model.subscribeAsState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentItem = DrawerItem.Home,
                // Close before navigating, so the drawer isn't restored open when coming back
                onHomeClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                        onHomeClicked()
                    }
                },
                onInterestsClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                        onInterestsClicked()
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(text = "Jetnews") },
                navigationIcon = {
                    IconButton(
                        onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }
                    ) {
                        Icon(painterResource(R.drawable.ic_jetnews_logo), contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            Crossfade(targetState = model.posts) { uiState ->
                when (uiState) {
                    is UiState.Success -> HomeScreenBody(
                        modifier = modifier,
                        posts = uiState.data,
                        favorites = model.favorites,
                        onPostClicked = component::onPostClicked,
                        onToggleFavorite = component::onFavoriteToggled
                    )
                    is UiState.Loading -> {
                        Text(
                            modifier = modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                            text = "Loading"
                        )
                    }
                    else -> {
                        // Empty
                    }
                }
            }
        }
    )
}

@Composable
private fun HomeScreenBody(
    posts: List<Post>,
    favorites: Set<String>,
    onPostClicked: (postId: String) -> Unit,
    onToggleFavorite: (postId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val postTop = posts[3]
    val postsSimple = posts.subList(0, 2)
    val postsPopular = posts.subList(2, 7)
    val postsHistory = posts.subList(7, 10)

    Column(modifier.verticalScroll(rememberScrollState())) {
        HomeScreenTopSection(postTop, onPostClicked)
        HomeScreenSimpleSection(postsSimple, favorites, onPostClicked, onToggleFavorite)
        HomeScreenPopularSection(postsPopular, onPostClicked)
        HomeScreenHistorySection(postsHistory, onPostClicked)
    }
}

@Composable
private fun HomeScreenTopSection(post: Post, onPostClicked: (postId: String) -> Unit) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            text = "Top stories for you",
            style = MaterialTheme.typography.subtitle1
        )
    }
    PostCardTop(
        post = post,
        modifier = Modifier.clickable(onClick = { onPostClicked(post.id) })
    )
    HomeScreenDivider()
}

@Composable
private fun HomeScreenSimpleSection(
    posts: List<Post>,
    favorites: Set<String>,
    onPostClicked: (postId: String) -> Unit,
    onToggleFavorite: (postId: String) -> Unit
) {
    Column {
        posts.forEach { post ->
            PostCardSimple(
                post = post,
                isFavorite = post.id in favorites,
                onClick = onPostClicked,
                onToggleFavorite = onToggleFavorite
            )
            HomeScreenDivider()
        }
    }
}

@Composable
private fun HomeScreenPopularSection(posts: List<Post>, onPostClicked: (postId: String) -> Unit) {
    Column {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Popular on Jetnews",
                style = MaterialTheme.typography.subtitle1
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            posts.forEach { post ->
                PostCardPopular(post, onPostClicked, Modifier.padding(start = 16.dp))
            }
        }
        HomeScreenDivider()
    }
}

@Composable
private fun HomeScreenHistorySection(posts: List<Post>, onPostClicked: (postId: String) -> Unit) {
    Column {
        posts.forEach { post ->
            PostCardHistory(post, onPostClicked)
            HomeScreenDivider()
        }
    }
}

@Composable
private fun HomeScreenDivider() {
    Divider(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
    )
}

@Preview("Home screen body")
@Composable
fun PreviewHomeScreenBody() {
    ThemedPreview {
        HomeScreenBody(
            posts = previewPosts(),
            favorites = emptySet(),
            onPostClicked = {},
            onToggleFavorite = {}
        )
    }
}

@Preview("Home screen, open drawer")
@Composable
private fun PreviewDrawerOpen() {
    ThemedPreview {
        HomeScreen(
            component = PreviewHomeComponent(posts),
            onHomeClicked = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Home screen dark theme")
@Composable
fun PreviewHomeScreenBodyDark() {
    ThemedPreview(darkThemeColors) {
        HomeScreenBody(
            posts = previewPosts(),
            favorites = emptySet(),
            onPostClicked = {},
            onToggleFavorite = {}
        )
    }
}

@Preview("Home screen, open drawer dark theme")
@Composable
private fun PreviewDrawerOpenDark() {
    ThemedPreview(darkThemeColors) {
        HomeScreen(
            component = PreviewHomeComponent(posts),
            onHomeClicked = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}
