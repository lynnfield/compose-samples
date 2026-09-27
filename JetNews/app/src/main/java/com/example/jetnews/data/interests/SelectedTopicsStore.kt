package com.example.jetnews.data.interests

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-scoped holder of the keys of the topics, people and publications the user follows.
 */
class SelectedTopicsStore {

    private val _selectedTopics = MutableStateFlow<Set<String>>(emptySet())
    val selectedTopics: StateFlow<Set<String>> = _selectedTopics.asStateFlow()

    fun setSelected(topicKey: String, selected: Boolean) {
        _selectedTopics.update { topics ->
            if (selected) topics + topicKey else topics - topicKey
        }
    }
}
