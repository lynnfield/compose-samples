package com.example.jetnews.flow.home

import com.example.jetnews.flow.article.ArticleFeatureUiState
import com.example.jetnews.flow.article.CloseArticleUiState
import com.example.jetnews.flow.common.RefreshUiState
import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.parallel
import com.genovich.components.updateLoop
import com.genovich.components.whileActive
import kotlinx.coroutines.flow.StateFlow

/**
 * What the Home screen shows: the posts and which of them are bookmarked.
 */
data class HomeState(val posts: List<Post>, val favorites: Set<String>)

/**
 * Shows the Home screen for the loaded posts, forever.
 *
 * While Home is on screen, three things run side by side: [showHome], which answers the id of a
 * post whose bookmark was tapped (it is toggled and Home is shown again), [openArticle], which
 * answers the id of a tapped post, and [refresh] (pull to refresh), which returns new posts.
 * A tapped post opens the [article] and Home comes back with the same posts when it's closed;
 * refreshed posts come back right away. Either way Home is shown again with fresh favorites.
 *
 * Opening an article cancels a refresh that is still running, as leaving Home did upstream.
 */
class ShowHome(
    val getFavorites: Action<Unit, Set<String>>,
    val showHome: Action<HomeState, String>,
    val toggleFavorite: Action<String, Unit>,
    val openArticle: Action<List<Post>, String>,
    val article: Action<String, Unit>,
    val refresh: Action<List<Post>, List<Post>>
) : Action<List<Post>, Nothing>() {

    override suspend fun invoke(input: List<Post>): Nothing =
        updateLoop(input) { posts ->
            val next = parallel<List<Post>, OneOf<String, List<Post>>>(
                posts,
                { p -> whileActive { toggleFavorite(showHome(HomeState(p, getFavorites(Unit)))) } },
                { p -> OneOf.First(openArticle(p)) },
                { p -> OneOf.Second(refresh(p)) }
            )
            when (next) {
                is OneOf.First -> {
                    article(next.first)
                    posts
                }
                is OneOf.Second -> next.second
            }
        }
}

/**
 * The open article T-function: answers the id of the post the user tapped.
 */
typealias OpenArticleUiState = UiState<List<Post>, String>

/**
 * What [ShowHome] wants on screen.
 */
sealed class ShowHomeUiState<out ArticleUiState : Any> {

    /**
     * The Home screen: its content, the way to open an article and pull to refresh (`null` for
     * an instant between two refresh states).
     */
    class Home(
        val home: StateFlow<UiState<HomeState, String>>,
        val openArticle: StateFlow<OpenArticleUiState>,
        val refresh: StateFlow<RefreshUiState?>
    ) : ShowHomeUiState<Nothing>()

    /**
     * An open article and the way to close it.
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
    openArticle: Pair<Action<List<Post>, String>, UiStateFlow<List<Post>, String>>,
    article: Pair<Action<String, Unit>, StateFlow<ArticleFeatureUiState<ArticleUiState>?>>,
    refresh: Pair<Action<List<Post>, List<Post>>, StateFlow<RefreshUiState?>>
): Pair<Action<List<Post>, Nothing>, StateFlow<ShowHomeUiState<ArticleUiState>?>> =
    ShowHome(
        getFavorites,
        showHome.first,
        toggleFavorite,
        openArticle.first,
        article.first,
        refresh.first
    ) to combine(
        flow = showHome.second.emitSelfWhenHaveValue(),
        flow2 = openArticle.second.emitSelfWhenHaveValue(),
        flow3 = article.second,
        transform = { home, open, feature ->
            when {
                feature != null -> ShowHomeUiState.Article(feature.article, feature.close)
                home != null && open != null -> ShowHomeUiState.Home(home, open, refresh.second)
                else -> null
            }
        }
    )
