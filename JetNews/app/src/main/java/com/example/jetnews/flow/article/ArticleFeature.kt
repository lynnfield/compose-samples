package com.example.jetnews.flow.article

import com.example.jetnews.flow.common.LoadAndShow
import com.example.jetnews.flow.common.RetryableUiState
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.Publish
import com.genovich.components.ShowProgress
import com.genovich.components.TryAgain
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.parallel
import kotlinx.coroutines.flow.StateFlow

/**
 * An open article: the progress while the post loads, then the post, or the error with Retry.
 */
typealias ArticleUiState = RetryableUiState<ShowArticleUiState>

/**
 * The close article T-function: answered by Back.
 */
typealias CloseArticleUiState = UiState<Unit, Unit>

/**
 * The content of an open article: load the post with the given id, then show it. If loading
 * fails, show the error and load again on Retry.
 */
fun ArticleContent(
    loadPost: Action<String, OneOf<Post, Throwable>>,
    getFavorites: Action<Unit, Set<String>>,
    toggleFavorite: Action<String, Unit>,
    sharePost: Action<Post, Unit>
): Pair<Action<String, Nothing>, StateFlow<ArticleUiState?>> = TryAgain(
    action = LoadAndShow(
        load = ShowProgress(loadPost, Publish<Progress>()),
        show = ShowArticle(
            getFavorites = getFavorites,
            showArticle = showChannel(),
            toggleFavorite = toggleFavorite,
            share = showChannel(),
            sharePost = sharePost
        )
    ),
    showError = showChannel()
)

/**
 * An open article: runs [content] for the post id until [close] is answered (Back).
 */
class ArticleFeature(
    val content: Action<String, Nothing>,
    val close: Action<Unit, Unit>
) : Action<String, Unit>() {

    override suspend fun invoke(input: String) =
        parallel(input, { postId -> content(postId) }, { close(Unit) })
}

/**
 * What [ArticleFeature] wants on screen: the article and the way to close it.
 */
class ArticleFeatureUiState<ContentUiState : Any>(
    val article: StateFlow<ContentUiState>,
    val close: StateFlow<CloseArticleUiState>
)

/**
 * [ArticleFeature] wired with its projection, present while both the content and [close] show
 * something.
 */
fun <ContentUiState : Any> ArticleFeature(
    content: Pair<Action<String, Nothing>, StateFlow<ContentUiState?>>,
    close: Pair<Action<Unit, Unit>, UiStateFlow<Unit, Unit>>
): Pair<Action<String, Unit>, StateFlow<ArticleFeatureUiState<ContentUiState>?>> =
    ArticleFeature(content.first, close.first) to combine(
        flow = content.second.emitSelfWhenHaveValue(),
        flow2 = close.second.emitSelfWhenHaveValue(),
        transform = { article, closeFlow ->
            if (article != null && closeFlow != null) ArticleFeatureUiState(article, closeFlow) else null
        }
    )
