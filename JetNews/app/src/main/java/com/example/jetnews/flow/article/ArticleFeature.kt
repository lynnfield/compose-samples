package com.example.jetnews.flow.article

import com.example.jetnews.flow.common.LoadAndShow
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.flow.home.ArticleFeatureUiState
import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OnDemandFeature
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * An open article: blank while the post loads or if it fails to load (as in the base app),
 * then the post.
 */
typealias ArticleUiState = LoadAndShowUiState<ShowArticleUiState, UiState<Throwable, Nothing>>

/**
 * The content of an open article: load the post with the given id, then show it.
 */
fun ArticleContent(
    loadPost: Action<String, OneOf<Post, Throwable>>,
    getFavorites: Action<Unit, Set<String>>,
    toggleFavorite: Action<String, Unit>,
    sharePost: Action<Post, Unit>
): Pair<Action<String, Nothing>, StateFlow<ArticleUiState?>> = LoadAndShow(
    load = loadPost to MutableStateFlow<Progress?>(null),
    show = ShowArticle(
        getFavorites = getFavorites,
        showArticle = showChannel(),
        toggleFavorite = toggleFavorite,
        share = showChannel(),
        sharePost = sharePost
    ),
    showError = showChannel<Throwable, Nothing>()
)

/**
 * The article feature: waits on [openArticle] for a post id, then runs a fresh [content] for it
 * until [closeArticle] is answered (Back).
 */
fun ArticleFeature(
    openArticle: Pair<Action<List<Post>, String>, UiStateFlow<List<Post>, String>>,
    closeArticle: Pair<Action<List<Post>, Unit>, UiStateFlow<List<Post>, Unit>>,
    content: () -> Pair<Action<String, Nothing>, StateFlow<ArticleUiState?>>
): Pair<Action<List<Post>, Unit>, StateFlow<ArticleFeatureUiState<ArticleUiState>?>> =
    OnDemandFeature(
        open = openArticle,
        close = closeArticle,
        featureBuilder = { _: List<Post> -> content() }
    )
