package com.example.jetnews.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.example.jetnews.data.Result
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.data.posts.impl.PreviewPostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.ui.article.ArticleComponent
import com.example.jetnews.ui.home.HomeComponent
import com.example.jetnews.ui.interests.InterestsComponent

/*
 * Fake components for @Preview. They hold their model in memory and react to intents locally.
 */

class PreviewHomeComponent(
    posts: List<Post>,
    favorites: Set<String> = emptySet()
) : HomeComponent {

    override val model = MutableValue(HomeComponent.Model(UiState.Success(posts), favorites))

    override fun onPostClicked(postId: String) = Unit

    override fun onFavoriteToggled(postId: String) {
        model.update { it.copy(favorites = it.favorites.toggle(postId)) }
    }
}

class PreviewArticleComponent(
    post: Post,
    isFavorite: Boolean = false
) : ArticleComponent {

    override val model = MutableValue(ArticleComponent.Model(UiState.Success(post), isFavorite))

    override fun onFavoriteToggled() {
        model.update { it.copy(isFavorite = !it.isFavorite) }
    }

    override fun onBackClicked() = Unit
}

class PreviewInterestsComponent(
    repository: InterestsRepository = FakeInterestsRepository()
) : InterestsComponent {

    override val model = MutableValue(
        InterestsComponent.Model(
            section = InterestsComponent.Section.Topics,
            topics = UiState.Success(previewData(repository::getTopics)),
            people = UiState.Success(previewData(repository::getPeople)),
            publications = UiState.Success(previewData(repository::getPublications)),
            selectedTopics = emptySet()
        )
    )

    override fun onSectionSelected(section: InterestsComponent.Section) {
        model.update { it.copy(section = section) }
    }

    override fun onTopicToggled(topicKey: String, selected: Boolean) {
        model.update {
            val topics = if (selected) it.selectedTopics + topicKey else it.selectedTopics - topicKey
            it.copy(selectedTopics = topics)
        }
    }
}

/**
 * Returns the data of a repository call that completes synchronously. Only use in Previews!
 */
fun <T> previewData(repositoryCall: ((Result<T>) -> Unit) -> Unit): T {
    var data: T? = null
    repositoryCall { result ->
        data = (result as Result.Success).data
    }
    @Suppress("UNCHECKED_CAST")
    return data as T
}

/**
 * Posts with their images loaded. Only use in Previews!
 */
@Composable
fun previewPosts(): List<Post> {
    val context = LocalContext.current
    return remember(context) { previewData(PreviewPostsRepository(context)::getPosts) }
}

/**
 * A post with its images loaded. Only use in Previews!
 */
@Composable
fun previewPost(postId: String): Post {
    val context = LocalContext.current
    return remember(context, postId) {
        previewData<Post?> { callback -> PreviewPostsRepository(context).getPost(postId, callback) }!!
    }
}

private fun Set<String>.toggle(item: String) = if (item in this) this - item else this + item
