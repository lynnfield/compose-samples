package com.example.jetnews.flow.interests

import com.genovich.components.Action
import com.genovich.components.OneOf

/**
 * Everything the Interests screen lists.
 */
data class Interests(
    val topics: Map<String, List<String>>,
    val people: List<String>,
    val publications: List<String>
)

/**
 * Loads topics, people and publications. The first error stops the load.
 */
class LoadInterests(
    val loadTopics: Action<Unit, OneOf<Map<String, List<String>>, Throwable>>,
    val loadPeople: Action<Unit, OneOf<List<String>, Throwable>>,
    val loadPublications: Action<Unit, OneOf<List<String>, Throwable>>
) : Action<Unit, OneOf<Interests, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<Interests, Throwable> {
        val topics = when (val result = loadTopics(input)) {
            is OneOf.First -> result.first
            is OneOf.Second -> return result
        }
        val people = when (val result = loadPeople(input)) {
            is OneOf.First -> result.first
            is OneOf.Second -> return result
        }
        val publications = when (val result = loadPublications(input)) {
            is OneOf.First -> result.first
            is OneOf.Second -> return result
        }
        return OneOf.First(Interests(topics, people, publications))
    }
}
