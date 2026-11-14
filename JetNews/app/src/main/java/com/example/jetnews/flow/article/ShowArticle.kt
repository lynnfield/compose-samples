package com.example.jetnews.flow.article

import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.parallel
import com.genovich.components.updateLoop
import kotlinx.coroutines.flow.StateFlow

/**
 * What the Article screen shows: the post and whether it's bookmarked.
 */
data class ArticleState(val post: Post, val isFavorite: Boolean)

/**
 * Shows a post, forever.
 *
 * Two T-functions run side by side: [showArticle], answered when the bookmark is tapped, and
 * [share], answered when Share is tapped. Either one restarts the loop after its effect.
 */
class ShowArticle(
    val getFavorites: Action<Unit, Set<String>>,
    val showArticle: Action<ArticleState, Unit>,
    val toggleFavorite: Action<String, Unit>,
    val share: Action<Post, Post>,
    val sharePost: Action<Post, Unit>
) : Action<Post, Nothing>() {

    override suspend fun invoke(input: Post): Nothing =
        updateLoop(Unit) {
            parallel(
                input,
                { post ->
                    showArticle(ArticleState(post, post.id in getFavorites(Unit)))
                    toggleFavorite(post.id)
                },
                { post -> sharePost(share(post)) }
            )
        }
}

/**
 * What [ShowArticle] wants on screen: one screen answering two T-functions.
 */
class ShowArticleUiState(
    val article: StateFlow<UiState<ArticleState, Unit>>,
    val share: StateFlow<UiState<Post, Post>>
)

/**
 * [ShowArticle] wired with its projection, present while both T-functions are waiting.
 */
fun ShowArticle(
    getFavorites: Action<Unit, Set<String>>,
    showArticle: Pair<Action<ArticleState, Unit>, UiStateFlow<ArticleState, Unit>>,
    toggleFavorite: Action<String, Unit>,
    share: Pair<Action<Post, Post>, UiStateFlow<Post, Post>>,
    sharePost: Action<Post, Unit>
): Pair<Action<Post, Nothing>, StateFlow<ShowArticleUiState?>> =
    ShowArticle(getFavorites, showArticle.first, toggleFavorite, share.first, sharePost) to combine(
        flow = showArticle.second.emitSelfWhenHaveValue(),
        flow2 = share.second.emitSelfWhenHaveValue(),
        transform = { article, shareFlow ->
            if (article != null && shareFlow != null) ShowArticleUiState(article, shareFlow) else null
        }
    )
