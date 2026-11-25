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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.flow.HomeUiState
import com.example.jetnews.flow.common.ErrorUiState
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.home.ShowHomeUiState
import com.example.jetnews.model.Post
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.DrawerItem
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.article.ArticleRoute
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.previewPosts
import com.genovich.components.TryAgainUiState
import com.genovich.components.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Renders Home from the program's UI state: a spinner, the posts, an open article on top, or the
 * error with Retry.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeRoute(
    home: StateFlow<HomeUiState>,
    openInterests: StateFlow<UiState<Unit, Unit>>
) {
    // Hoisted, so the drawer keeps its state between loading, the error and the posts
    val scaffoldState = rememberScaffoldState()
    val homeState by home.collectAsState()
    val retryable = homeState
    val error = if (retryable is TryAgainUiState.Error) {
        retryable.flow.collectAsState().value
    } else {
        null
    }
    val loaded = if (retryable is TryAgainUiState.Action) {
        retryable.flow.collectAsState().value
    } else {
        null
    }
    val showHome = if (loaded is LoadAndShowUiState.Content) {
        loaded.content.collectAsState().value
    } else {
        null
    }
    val openArticle = if (showHome is ShowHomeUiState.Article) showHome else null
    updateTransition(openArticle, label = "article").Crossfade(contentKey = { it != null }) { article ->
        if (article != null) {
            ArticleRoute(article = article.article, close = article.close)
        } else {
            HomeBody(
                loading = loaded is LoadAndShowUiState.Loading,
                home = if (showHome is ShowHomeUiState.Home) showHome else null,
                error = error,
                openInterests = openInterests,
                scaffoldState = scaffoldState
            )
        }
    }
}

@Composable
private fun HomeBody(
    loading: Boolean,
    home: ShowHomeUiState.Home?,
    error: ErrorUiState?,
    openInterests: StateFlow<UiState<Unit, Unit>>,
    scaffoldState: ScaffoldState
) {
    val showHome = home?.home?.collectAsState()?.value
    val openArticle = home?.openArticle?.collectAsState()?.value
    val openInterestsState by openInterests.collectAsState()
    // The error waits for Retry; hiding the snackbar is only view state
    var showError by remember(error) { mutableStateOf(error != null) }
    HomeScreen(
        content = when {
            showHome != null -> HomeContent.Posts(showHome.input.posts)
            loading -> HomeContent.Loading
            else -> HomeContent.Empty
        },
        favorites = showHome?.input?.favorites ?: emptySet(),
        showError = showError,
        onRetry = { error?.action?.invoke(Unit) },
        onErrorDismissed = { showError = false },
        onPostClicked = { postId -> openArticle?.action?.invoke(postId) },
        onToggleFavorite = { postId -> showHome?.action?.invoke(postId) },
        onInterestsClicked = { openInterestsState.action(Unit) },
        scaffoldState = scaffoldState
    )
}

/**
 * What the body of the Home screen shows.
 */
sealed class HomeContent {
    object Loading : HomeContent()
    data class Posts(val posts: List<Post>) : HomeContent()
    object Empty : HomeContent()
}

@Composable
fun HomeScreen(
    content: HomeContent,
    favorites: Set<String>,
    showError: Boolean,
    onRetry: () -> Unit,
    onErrorDismissed: () -> Unit,
    onPostClicked: (postId: String) -> Unit,
    onToggleFavorite: (postId: String) -> Unit,
    onInterestsClicked: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentItem = DrawerItem.Home,
                onHomeClicked = { coroutineScope.launch { scaffoldState.drawerState.close() } },
                // Close before navigating, as on the decompose branch
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
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Crossfade(targetState = content) { bodyContent ->
                    when (bodyContent) {
                        is HomeContent.Posts -> HomeScreenBody(
                            posts = bodyContent.posts,
                            favorites = favorites,
                            onPostClicked = onPostClicked,
                            onToggleFavorite = onToggleFavorite
                        )
                        is HomeContent.Loading -> {
                            Box(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                                CircularProgressIndicator()
                            }
                        }
                        is HomeContent.Empty -> {
                            // Empty
                        }
                    }
                }
                ErrorSnackbar(
                    showError = showError,
                    onRetry = onRetry,
                    onDismiss = onErrorDismissed,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    )
}

/**
 * "Can't update latest news" with Retry. It hides itself after 5 seconds, as in upstream.
 */
@Composable
fun ErrorSnackbar(
    showError: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showError) {
        val currentOnDismiss by rememberUpdatedState(onDismiss)
        LaunchedEffect(Unit) {
            delay(5000L)
            currentOnDismiss()
        }
        Snackbar(
            modifier = modifier.padding(16.dp),
            action = {
                TextButton(
                    onClick = {
                        onRetry()
                        onDismiss()
                    }
                ) {
                    Text(text = "RETRY")
                }
            }
        ) {
            Text(text = "Can't update latest news")
        }
    }
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
            content = HomeContent.Posts(previewPosts()),
            favorites = emptySet(),
            showError = false,
            onRetry = {},
            onErrorDismissed = {},
            onPostClicked = {},
            onToggleFavorite = {},
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
            content = HomeContent.Posts(previewPosts()),
            favorites = emptySet(),
            showError = false,
            onRetry = {},
            onErrorDismissed = {},
            onPostClicked = {},
            onToggleFavorite = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}
