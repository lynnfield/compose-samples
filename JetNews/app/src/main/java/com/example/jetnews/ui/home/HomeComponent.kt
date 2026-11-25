package com.example.jetnews.ui.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.example.jetnews.data.Result
import com.example.jetnews.data.awaitPosts
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Home screen: the list of posts, with bookmarks and pull to refresh. A failed load shows an
 * error with Retry, and keeps the posts that are already on screen.
 */
interface HomeComponent {

    val model: Value<Model>

    fun onPostClicked(postId: String)

    fun onFavoriteToggled(postId: String)

    fun onRefresh()

    fun onRetry()

    fun onErrorDismissed()

    /**
     * [posts] is `null` until the first load succeeds. [isLoading] is `true` while a load runs,
     * and [showError] while the error of the last load is shown.
     */
    data class Model(
        val posts: List<Post>?,
        val isLoading: Boolean,
        val showError: Boolean,
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
        retained.loadPosts()
    }

    override fun onRetry() {
        retained.loadPosts()
    }

    override fun onErrorDismissed() {
        retained.model.update { it.copy(showError = false) }
    }

    private class Retained(
        private val postsRepository: PostsRepository,
        favoritesStore: FavoritesStore,
        mainContext: CoroutineContext
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(mainContext + SupervisorJob())
        private var loading: Job? = null

        val model = MutableValue(
            HomeComponent.Model(
                posts = null,
                isLoading = true,
                showError = false,
                favorites = favoritesStore.favorites.value
            )
        )

        init {
            loadPosts()
            scope.launch {
                favoritesStore.favorites.collect { favorites ->
                    model.update { it.copy(favorites = favorites) }
                }
            }
        }

        // Keeps the posts on screen while loading; one load at a time
        fun loadPosts() {
            if (loading?.isActive == true) return
            model.update { it.copy(isLoading = true, showError = false) }
            loading = scope.launch {
                when (val result = postsRepository.awaitPosts()) {
                    is Result.Success -> model.update {
                        it.copy(posts = result.data, isLoading = false)
                    }
                    is Result.Error -> model.update { it.copy(isLoading = false, showError = true) }
                }
            }
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}
