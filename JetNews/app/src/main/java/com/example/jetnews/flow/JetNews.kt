package com.example.jetnews.flow

import com.example.jetnews.flow.article.ArticleUiState
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.home.ShowHomeUiState
import com.example.jetnews.flow.interests.InterestsFeatureUiState
import com.example.jetnews.flow.interests.InterestsUiState
import com.genovich.components.Action
import com.genovich.components.OnDemandFeatureUiState
import com.genovich.components.UiState
import com.genovich.components.combine
import com.genovich.components.emitSelfWhenHaveValue
import com.genovich.components.parallel
import com.genovich.components.whileActive
import kotlinx.coroutines.flow.StateFlow

/**
 * The whole app: Home runs forever, and next to it the Interests feature can be opened from the
 * drawer and closed again, as many times as the user likes.
 */
class JetNews(
    val home: Action<Unit, Nothing>,
    val interests: Action<Unit, Unit>
) : Action<Unit, Nothing>() {

    override suspend fun invoke(input: Unit): Nothing =
        parallel(
            input,
            { home(it) },
            { whileActive { interests(it) } }
        )
}

/**
 * Home: "Loading", then the posts (or an open article), or blank on error.
 */
typealias HomeUiState = LoadAndShowUiState<ShowHomeUiState<ArticleUiState>, UiState<Throwable, Nothing>>

/**
 * What the app shows. The UI renders exactly one case.
 */
sealed class JetNewsUiState {

    /**
     * Home (or an article opened from it), with the drawer's way to open Interests.
     */
    class Home(
        val home: StateFlow<HomeUiState>,
        val openInterests: StateFlow<UiState<Unit, Unit>>
    ) : JetNewsUiState()

    /**
     * Interests, with the way back to Home (the drawer's Home item or Back).
     */
    class Interests(
        val interests: StateFlow<InterestsUiState>,
        val close: StateFlow<UiState<Unit, Unit>>
    ) : JetNewsUiState()
}

/**
 * [JetNews] wired with its projection: open Interests win over Home.
 */
fun JetNews(
    home: Pair<Action<Unit, Nothing>, StateFlow<HomeUiState?>>,
    interests: Pair<Action<Unit, Unit>, StateFlow<InterestsFeatureUiState?>>
): Pair<Action<Unit, Nothing>, StateFlow<JetNewsUiState?>> =
    JetNews(home.first, interests.first) to combine(
        flow = home.second.emitSelfWhenHaveValue(),
        flow2 = interests.second,
        transform = { homeFlow, feature ->
            when (feature) {
                is OnDemandFeatureUiState.FeatureAndClose ->
                    JetNewsUiState.Interests(feature.featureFlow, feature.closeFlow)
                is OnDemandFeatureUiState.Open ->
                    homeFlow?.let { JetNewsUiState.Home(it, feature.flow) }
                null -> null
            }
        }
    )
