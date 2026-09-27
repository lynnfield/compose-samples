package com.example.jetnews.ui.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.ui.article.DefaultArticleComponent
import com.example.jetnews.ui.home.DefaultHomeComponent
import com.example.jetnews.ui.interests.DefaultInterestsComponent
import com.example.jetnews.ui.root.RootComponent.Child
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val postsRepository: PostsRepository,
    private val interestsRepository: InterestsRepository,
    private val favoritesStore: FavoritesStore,
    private val selectedTopicsStore: SelectedTopicsStore,
    private val mainContext: CoroutineContext = Dispatchers.Main.immediate
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    // Home is always at the bottom of the stack. The configurations are saved, so the stack
    // survives process death, and system back pops it.
    override val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = true,
        childFactory = ::child
    )

    override fun onHomeClicked() {
        navigation.popToFirst()
    }

    override fun onInterestsClicked() {
        navigation.bringToFront(Config.Interests)
    }

    private fun child(config: Config, componentContext: ComponentContext): Child =
        when (config) {
            is Config.Home -> Child.Home(
                DefaultHomeComponent(
                    componentContext = componentContext,
                    postsRepository = postsRepository,
                    favoritesStore = favoritesStore,
                    mainContext = mainContext,
                    onArticleSelected = { postId -> navigation.pushNew(Config.Article(postId)) }
                )
            )
            is Config.Article -> Child.Article(
                DefaultArticleComponent(
                    componentContext = componentContext,
                    postId = config.postId,
                    postsRepository = postsRepository,
                    favoritesStore = favoritesStore,
                    mainContext = mainContext,
                    onBack = { navigation.pop() }
                )
            )
            is Config.Interests -> Child.Interests(
                DefaultInterestsComponent(
                    componentContext = componentContext,
                    interestsRepository = interestsRepository,
                    selectedTopicsStore = selectedTopicsStore,
                    mainContext = mainContext
                )
            )
        }

    /**
     * The screens of the app: home, article details and interests.
     */
    @Serializable
    private sealed class Config {
        @Serializable
        object Home : Config()

        @Serializable
        data class Article(val postId: String) : Config()

        @Serializable
        object Interests : Config()
    }
}
