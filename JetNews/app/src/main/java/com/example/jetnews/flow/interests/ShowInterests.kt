package com.example.jetnews.flow.interests

import com.example.jetnews.data.interests.TopicSelection
import com.genovich.components.Action
import com.genovich.components.UiStateFlow
import com.genovich.components.updateLoop

/**
 * What the Interests screen shows: everything to pick from and the selected topic keys.
 */
data class InterestsState(val interests: Interests, val selectedTopics: Set<String>)

/**
 * Shows the Interests screen, forever. [showInterests] answers a topic the user selected or
 * deselected; the selection is stored and the screen is shown again.
 */
class ShowInterests(
    val getSelectedTopics: Action<Unit, Set<String>>,
    val showInterests: Action<InterestsState, TopicSelection>,
    val selectTopic: Action<TopicSelection, Unit>
) : Action<Interests, Nothing>() {

    override suspend fun invoke(input: Interests): Nothing =
        updateLoop(Unit) {
            selectTopic(showInterests(InterestsState(input, getSelectedTopics(Unit))))
        }
}

/**
 * [ShowInterests] wired with its projection, which is just [showInterests]' own flow.
 */
fun ShowInterests(
    getSelectedTopics: Action<Unit, Set<String>>,
    showInterests: Pair<Action<InterestsState, TopicSelection>, UiStateFlow<InterestsState, TopicSelection>>,
    selectTopic: Action<TopicSelection, Unit>
): Pair<Action<Interests, Nothing>, UiStateFlow<InterestsState, TopicSelection>> =
    ShowInterests(getSelectedTopics, showInterests.first, selectTopic) to showInterests.second
