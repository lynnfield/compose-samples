package com.example.jetnews.flow.home

import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OnDemandFeatureUiState
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.parallel
import com.genovich.components.updateLoop
import kotlinx.coroutines.flow.StateFlow

/**
 * What the Home screen shows: the posts and which of them are bookmarked.
 */
data class HomeState(val posts: List<Post>, val favorites: Set<String>)

/**
 * Shows the Home screen for the loaded posts, forever.
 *
 * Two things run side by side: [showHome], which answers the id of the post whose bookmark was
 * tapped, and the [article] feature, which waits for a post to be opened and returns when the
 * article is closed. Whichever finishes first restarts the loop, so Home is shown again with
 * fresh favorites.
 */
class ShowHome(
    val getFavorites: Action<Unit, Set<String>>,
    val showHome: Action<HomeState, String>,
    val toggleFavorite: Action<String, Unit>,
    val article: Action<List<Post>, Unit>
) : Action<List<Post>, Nothing>() {

    override suspend fun invoke(input: List<Post>): Nothing =
        updateLoop(Unit) {
            parallel(
                input,
                { posts -> toggleFavorite(showHome(HomeState(posts, getFavorites(Unit)))) },
                { posts -> article(posts) }
            )
        }
}

/**
 * The open article T-function: answers the id of the post the user tapped.
 */
typealias OpenArticleUiState = UiState<List<Post>, String>

/**
 * The close article T-function: answered by Back.
 */
typealias CloseArticleUiState = UiState<List<Post>, Unit>

/**
 * The article feature's UI state: waiting for a post to be opened, or the open article.
 */
typealias ArticleFeatureUiState<ArticleUiState> =
    OnDemandFeatureUiState<OpenArticleUiState, CloseArticleUiState, ArticleUiState>

/**
 * What [ShowHome] wants on screen.
 */
sealed class ShowHomeUiState<out ArticleUiState : Any> {

    /**
     * The Home screen: its content and the way to open an article.
     */
    class Home(
        val home: StateFlow<UiState<HomeState, String>>,
        val openArticle: StateFlow<OpenArticleUiState>
    ) : ShowHomeUiState<Nothing>()

    /**
     * An open article, on top of Home, and the way to close it.
     */
    class Article<ArticleUiState : Any>(
        val article: StateFlow<ArticleUiState>,
        val close: StateFlow<CloseArticleUiState>
    ) : ShowHomeUiState<ArticleUiState>()
}

/**
 * [ShowHome] wired with its projection: the open article wins over Home.
 */
fun <ArticleUiState : Any> ShowHome(
    getFavorites: Action<Unit, Set<String>>,
    showHome: Pair<Action<HomeState, String>, UiStateFlow<HomeState, String>>,
    toggleFavorite: Action<String, Unit>,
    article: Pair<Action<List<Post>, Unit>, StateFlow<ArticleFeatureUiState<ArticleUiState>?>>
): Pair<Action<List<Post>, Nothing>, StateFlow<ShowHomeUiState<ArticleUiState>?>> =
    ShowHome(getFavorites, showHome.first, toggleFavorite, article.first) to combine(
        flow = showHome.second.emitSelfWhenHaveValue(),
        flow2 = article.second,
        transform = { home, feature ->
            when (feature) {
                is OnDemandFeatureUiState.FeatureAndClose ->
                    ShowHomeUiState.Article(feature.featureFlow, feature.closeFlow)
                is OnDemandFeatureUiState.Open ->
                    home?.let { ShowHomeUiState.Home(it, feature.flow) }
                null -> null
            }
        }
    )
