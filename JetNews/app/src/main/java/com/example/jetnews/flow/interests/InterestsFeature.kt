package com.example.jetnews.flow.interests

import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.flow.common.LoadAndShow
import com.example.jetnews.flow.common.RetryableUiState
import com.example.jetnews.flow.common.showChannel
import com.genovich.components.Action
import com.genovich.components.OnDemandFeature
import com.genovich.components.OnDemandFeatureUiState
import com.genovich.components.OneOf
import com.genovich.components.Progress
import com.genovich.components.TryAgain
import com.genovich.components.UiState
import com.genovich.components.UiStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The open Interests screen: blank while loading (as in the base app), then the interests, or
 * the error with Retry.
 */
typealias InterestsUiState = RetryableUiState<UiState<InterestsState, TopicSelection>>

/**
 * The Interests feature's UI state: waiting to be opened from the drawer, or open.
 */
typealias InterestsFeatureUiState =
    OnDemandFeatureUiState<UiState<Unit, Unit>, UiState<Unit, Unit>, InterestsUiState>

/**
 * The content of the Interests screen: load everything, then show it. If loading fails, show the
 * error and load again on Retry.
 */
fun InterestsContent(
    loadInterests: Action<Unit, OneOf<Interests, Throwable>>,
    getSelectedTopics: Action<Unit, Set<String>>,
    selectTopic: Action<TopicSelection, Unit>
): Pair<Action<Unit, Nothing>, StateFlow<InterestsUiState?>> = TryAgain(
    action = LoadAndShow(
        load = loadInterests to MutableStateFlow<Progress?>(null),
        show = ShowInterests(getSelectedTopics, showChannel(), selectTopic)
    ),
    showError = showChannel()
)

/**
 * The Interests feature: waits on [openInterests] (the drawer's Interests item), then runs a
 * fresh [content] until [closeInterests] is answered (the drawer's Home item or Back).
 */
fun InterestsFeature(
    openInterests: Pair<Action<Unit, Unit>, UiStateFlow<Unit, Unit>>,
    closeInterests: Pair<Action<Unit, Unit>, UiStateFlow<Unit, Unit>>,
    content: () -> Pair<Action<Unit, Nothing>, StateFlow<InterestsUiState?>>
): Pair<Action<Unit, Unit>, StateFlow<InterestsFeatureUiState?>> =
    OnDemandFeature(
        open = openInterests,
        close = closeInterests,
        featureBuilder = { _: Unit -> content() }
    )
