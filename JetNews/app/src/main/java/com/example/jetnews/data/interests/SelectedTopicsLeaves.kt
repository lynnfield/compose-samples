package com.example.jetnews.data.interests

import com.genovich.components.Action

/**
 * A topic the user selected ([selected] is `true`) or deselected.
 */
data class TopicSelection(val topicKey: String, val selected: Boolean)

/**
 * Reads the keys of the selected topics.
 */
class GetSelectedTopics(
    val store: SelectedTopicsStore
) : Action<Unit, Set<String>>() {

    override suspend fun invoke(input: Unit): Set<String> = store.selectedTopics.value
}

/**
 * Selects or deselects a topic.
 */
class SelectTopic(
    val store: SelectedTopicsStore
) : Action<TopicSelection, Unit>() {

    override suspend fun invoke(input: TopicSelection) =
        store.setSelected(input.topicKey, input.selected)
}
