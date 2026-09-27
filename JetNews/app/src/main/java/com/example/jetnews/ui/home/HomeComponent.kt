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
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.toUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
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

    data class Model(
        val posts: UiState<List<Post>>,
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

    private class Retained(
        postsRepository: PostsRepository,
        favoritesStore: FavoritesStore,
        mainContext: CoroutineContext
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(mainContext + SupervisorJob())

        val model = MutableValue(
            HomeComponent.Model(
                posts = UiState.Loading,
                favorites = favoritesStore.favorites.value
            )
        )

        init {
            scope.launch {
                val posts = postsRepository.awaitPosts().toUiState()
                model.update { it.copy(posts = posts) }
            }
            scope.launch {
                favoritesStore.favorites.collect { favorites ->
                    model.update { it.copy(favorites = favorites) }
                }
            }
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}
