package com.example.jetnews.flow

import com.example.jetnews.data.favorites.FavoritesStore
import com.example.jetnews.data.favorites.GetFavorites
import com.example.jetnews.data.favorites.ToggleFavorite
import com.example.jetnews.data.interests.GetSelectedTopics
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.LoadPeople
import com.example.jetnews.data.interests.LoadPublications
import com.example.jetnews.data.interests.LoadTopics
import com.example.jetnews.data.interests.SelectTopic
import com.example.jetnews.data.interests.SelectedTopicsStore
import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.data.posts.LoadPost
import com.example.jetnews.data.posts.LoadPosts
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.flow.article.ArticleContent
import com.example.jetnews.flow.article.ArticleFeature
import com.example.jetnews.flow.common.LoadAndShow
import com.example.jetnews.flow.common.showChannel
import com.example.jetnews.flow.home.ShowHome
import com.example.jetnews.flow.interests.Interests
import com.example.jetnews.flow.interests.InterestsContent
import com.example.jetnews.flow.interests.InterestsFeature
import com.example.jetnews.flow.interests.InterestsFeatureUiState
import com.example.jetnews.flow.interests.LoadInterests
import com.example.jetnews.model.Post
import com.genovich.components.Action
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.Publish
import com.genovich.components.ShowProgress
import com.genovich.components.TryAgain
import kotlinx.coroutines.flow.StateFlow

/**
 * Builds the whole app. Every port has a default, so a caller (or a test) only passes what it
 * wants to replace. The stores are declared once here and passed by name to every function
 * that needs them, so the whole app shares one instance of each.
 */
fun JetNewsAssembly(
    postsRepository: PostsRepository,
    interestsRepository: InterestsRepository,
    sharePost: Action<Post, Unit>,
    favoritesStore: FavoritesStore = FavoritesStore(),
    selectedTopicsStore: SelectedTopicsStore = SelectedTopicsStore(),
    getFavorites: Action<Unit, Set<String>> = GetFavorites(favoritesStore),
    toggleFavorite: Action<String, Unit> = ToggleFavorite(favoritesStore),
    getSelectedTopics: Action<Unit, Set<String>> = GetSelectedTopics(selectedTopicsStore),
    selectTopic: Action<TopicSelection, Unit> = SelectTopic(selectedTopicsStore),
    loadPosts: Action<Unit, OneOf<List<Post>, Throwable>> = LoadPosts(postsRepository),
    loadPost: Action<String, OneOf<Post, Throwable>> = LoadPost(postsRepository),
    loadInterests: Action<Unit, OneOf<Interests, Throwable>> = LoadInterests(
        loadTopics = LoadTopics(interestsRepository),
        loadPeople = LoadPeople(interestsRepository),
        loadPublications = LoadPublications(interestsRepository)
    ),
    home: Pair<Action<Unit, Nothing>, StateFlow<HomeUiState?>> = TryAgain(
        action = LoadAndShow(
            load = ShowProgress(loadPosts, Publish<Progress>()),
            show = ShowHome(
                getFavorites = getFavorites,
                showHome = showChannel(),
                toggleFavorite = toggleFavorite,
                article = ArticleFeature(
                    openArticle = showChannel(),
                    closeArticle = showChannel(),
                    content = { ArticleContent(loadPost, getFavorites, toggleFavorite, sharePost) }
                )
            )
        ),
        showError = showChannel()
    ),
    interests: Pair<Action<Unit, Unit>, StateFlow<InterestsFeatureUiState?>> = InterestsFeature(
        openInterests = showChannel(),
        closeInterests = showChannel(),
        content = { InterestsContent(loadInterests, getSelectedTopics, selectTopic) }
    )
): Pair<Action<Unit, Nothing>, StateFlow<JetNewsUiState?>> = JetNews(home, interests)
