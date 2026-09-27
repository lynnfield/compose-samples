package com.example.jetnews.ui.article

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.example.jetnews.data.Result
import com.example.jetnews.data.awaitPost
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.ui.UiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Article screen: a single post, which can be bookmarked.
 */
interface ArticleComponent {

    val model: Value<Model>

    fun onFavoriteToggled()

    fun onBackClicked()

    data class Model(
        val post: UiState<Post>,
        val isFavorite: Boolean
    )
}

class DefaultArticleComponent(
    componentContext: ComponentContext,
    private val postId: String,
    postsRepository: PostsRepository,
    private val favoritesStore: FavoritesStore,
    mainContext: CoroutineContext,
    private val onBack: () -> Unit
) : ArticleComponent, ComponentContext by componentContext {

    // Survives configuration changes, so the post is loaded once per component
    private val retained = instanceKeeper.getOrCreate {
        Retained(postId, postsRepository, favoritesStore, mainContext)
    }

    override val model: Value<ArticleComponent.Model> = retained.model

    override fun onFavoriteToggled() {
        favoritesStore.toggle(postId)
    }

    override fun onBackClicked() {
        onBack()
    }

    private class Retained(
        postId: String,
        postsRepository: PostsRepository,
        favoritesStore: FavoritesStore,
        mainContext: CoroutineContext
    ) : InstanceKeeper.Instance {

        private val scope = CoroutineScope(mainContext + SupervisorJob())

        val model = MutableValue(
            ArticleComponent.Model(
                post = UiState.Loading,
                isFavorite = postId in favoritesStore.favorites.value
            )
        )

        init {
            scope.launch {
                val post = when (val result = postsRepository.awaitPost(postId)) {
                    is Result.Success -> {
                        if (result.data != null) {
                            UiState.Success(result.data)
                        } else {
                            UiState.Error(Exception("postId doesn't exist"))
                        }
                    }
                    is Result.Error -> UiState.Error(result.exception)
                }
                model.update { it.copy(post = post) }
            }
            scope.launch {
                favoritesStore.favorites.collect { favorites ->
                    model.update { it.copy(isFavorite = postId in favorites) }
                }
            }
        }

        override fun onDestroy() {
            scope.cancel()
        }
    }
}
