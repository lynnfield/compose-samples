package com.example.jetnews.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.example.jetnews.data.awaitPosts
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.ui.RefreshableUiState
import com.example.jetnews.ui.toLoading
import com.example.jetnews.ui.withResult
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Home screen: the list of posts, with bookmarks.
 */
interface HomeComponent {

    val model: Value<Model>

    fun onPostClicked(postId: String)

    fun onFavoriteToggled(postId: String)

    /**
     * Reloads the posts, keeping the current ones on screen. Ignored while a load is running.
     */
    fun onRefresh()

    data class Model(
        val posts: RefreshableUiState<List<Post>>,
        val favorites: Set<String>
    )
}

class DefaultHomeComponent(
    componentContext: ComponentContext,
    postsRepository: PostsRepository,
    private val favoritesStore: FavoritesStore,
    mainContext: CoroutineContext,
    private val onArticleSelected: (postId: String) -> Unit
) : HomeComponent, ComponentContext by componentContext {

    // Survives configuration changes, so the posts are loaded once per component
    private val retained = instanceKeeper.getOrCreate {
        Retained(postsRepository, favoritesStore, mainContext)
    }

    override val model: Value<HomeComponent.Model> = retained.model

    override fun onPostClicked(postId: String) {
        onArticleSelected(postId)
    }

    override fun onFavoriteToggled(postId: String) {
        favoritesStore.toggle(postId)
    }

    override fun onRefresh() {
        retained.refresh()
    }

    private class Retained(
        private val postsRepository: PostsRepository,
        favoritesStore: FavoritesStore,
        mainContext: CoroutineContext
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(mainContext + SupervisorJob())

        val model = MutableValue(
            HomeComponent.Model(
                posts = RefreshableUiState.Success(data = null, loading = true),
                favorites = favoritesStore.favorites.value
            )
        )

        private var loading: Job? = null

        init {
            refresh()
            scope.launch {
                favoritesStore.favorites.collect { favorites ->
                    model.update { it.copy(favorites = favorites) }
                }
            }
        }

        fun refresh() {
            if (loading?.isActive == true) return
            model.update { it.copy(posts = it.posts.toLoading()) }
            loading = scope.launch {
                val result = postsRepository.awaitPosts()
                model.update { it.copy(posts = it.posts.withResult(result)) }
            }
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}
