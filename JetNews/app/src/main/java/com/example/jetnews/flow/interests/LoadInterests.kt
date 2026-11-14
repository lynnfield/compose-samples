package com.example.jetnews.flow.interests

import com.genovich.components.Action
import com.genovich.components.OneOf
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope

/**
 * Everything the Interests screen lists.
 */
data class Interests(
    val topics: Map<String, List<String>>,
    val people: List<String>,
    val publications: List<String>
)

/**
 * Loads topics, people and publications in parallel. The first error (in list order) wins and
 * cancels the loads that are still running.
 */
class LoadInterests(
    val loadTopics: Action<Unit, OneOf<Map<String, List<String>>, Throwable>>,
    val loadPeople: Action<Unit, OneOf<List<String>, Throwable>>,
    val loadPublications: Action<Unit, OneOf<List<String>, Throwable>>
) : Action<Unit, OneOf<Interests, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<Interests, Throwable> = coroutineScope {
        val topicsAsync = async { loadTopics(input) }
        val peopleAsync = async { loadPeople(input) }
        val publicationsAsync = async { loadPublications(input) }
        val topics = when (val result = topicsAsync.await()) {
            is OneOf.First -> result.first
            is OneOf.Second -> {
                coroutineContext.cancelChildren()
                return@coroutineScope result
            }
        }
        val people = when (val result = peopleAsync.await()) {
            is OneOf.First -> result.first
            is OneOf.Second -> {
                coroutineContext.cancelChildren()
                return@coroutineScope result
            }
        }
        val publications = when (val result = publicationsAsync.await()) {
            is OneOf.First -> result.first
            is OneOf.Second -> return@coroutineScope result
        }
        OneOf.First(Interests(topics, people, publications))
    }
}
