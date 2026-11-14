package com.example.jetnews.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetnews.flow.JetNewsUiState
import com.genovich.components.Action
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs the app's program for as long as the Activity's task lives, so it survives rotation.
 * The Activity only renders [uiState].
 */
class JetnewsViewModel(
    program: Pair<Action<Unit, Nothing>, StateFlow<JetNewsUiState?>>
) : ViewModel() {

    val uiState: StateFlow<JetNewsUiState?> = program.second

    init {
        viewModelScope.launch { program.first(Unit) }
    }
}
