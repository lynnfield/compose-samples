package com.example.jetnews.data.interests

import com.example.jetnews.data.RepositoryCall
import com.example.jetnews.data.await
import com.genovich.components.Action
import com.genovich.components.OneOf

/**
 * Loads the topics, grouped by section, from [repository].
 */
class LoadTopics(
    val repository: InterestsRepository
) : Action<Unit, OneOf<Map<String, List<String>>, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<Map<String, List<String>>, Throwable> {
        val call: RepositoryCall<Map<String, List<String>>> = repository::getTopics
        return call.await()
    }
}

/**
 * Loads the people from [repository].
 */
class LoadPeople(
    val repository: InterestsRepository
) : Action<Unit, OneOf<List<String>, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<List<String>, Throwable> {
        val call: RepositoryCall<List<String>> = repository::getPeople
        return call.await()
    }
}

/**
 * Loads the publications from [repository].
 */
class LoadPublications(
    val repository: InterestsRepository
) : Action<Unit, OneOf<List<String>, Throwable>>() {

    override suspend fun invoke(input: Unit): OneOf<List<String>, Throwable> {
        val call: RepositoryCall<List<String>> = repository::getPublications
        return call.await()
    }
}
