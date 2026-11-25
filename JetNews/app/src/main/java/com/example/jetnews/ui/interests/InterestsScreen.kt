/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.ui.interests

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jetnews.R
import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.flow.common.LoadAndShowUiState
import com.example.jetnews.flow.interests.Interests
import com.example.jetnews.flow.interests.InterestsUiState
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.DrawerItem
import com.example.jetnews.ui.ErrorScreen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.previewData
import com.genovich.components.TryAgainUiState
import com.genovich.components.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private enum class Sections(val title: String) {
    Topics("Topics"),
    People("People"),
    Publications("Publications")
}

/**
 * Renders Interests from the program's UI state. The drawer's Home item and system back close it.
 */
@Composable
fun InterestsRoute(
    interests: StateFlow<InterestsUiState>,
    close: StateFlow<UiState<Unit, Unit>>
) {
    val closeState by close.collectAsState()
    BackHandler { closeState.action(Unit) }
    val interestsState by interests.collectAsState()
    val retryable = interestsState
    val error = if (retryable is TryAgainUiState.Error) {
        retryable.flow.collectAsState().value
    } else {
        null
    }
    val loaded = if (retryable is TryAgainUiState.Action) {
        retryable.flow.collectAsState().value
    } else {
        null
    }
    val showInterests = if (loaded is LoadAndShowUiState.Content) {
        loaded.content.collectAsState().value
    } else {
        null
    }
    InterestsScreen(
        interests = showInterests?.input?.interests,
        selectedTopics = showInterests?.input?.selectedTopics ?: emptySet(),
        onTopicToggled = { key, selected ->
            showInterests?.action?.invoke(TopicSelection(key, selected))
        },
        onHomeClicked = { closeState.action(Unit) },
        onRetry = error?.let { { it.action(Unit) } }
    )
}

@Composable
fun InterestsScreen(
    interests: Interests?,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit,
    onHomeClicked: () -> Unit,
    onRetry: (() -> Unit)? = null,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentItem = DrawerItem.Interests,
                // Close before navigating, as on the decompose branch
                onHomeClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                        onHomeClicked()
                    }
                },
                onInterestsClicked = { coroutineScope.launch { scaffoldState.drawerState.close() } }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Interests") },
                navigationIcon = {
                    IconButton(
                        onClick = { coroutineScope.launch { scaffoldState.drawerState.open() } }
                    ) {
                        Icon(painterResource(R.drawable.ic_jetnews_logo), contentDescription = null)
                    }
                }
            )
        },
        content = { innerPadding ->
            val (currentSection, updateSection) = remember { mutableStateOf(Sections.Topics) }
            InterestsScreenBody(
                currentSection = currentSection,
                updateSection = updateSection,
                interests = interests,
                selectedTopics = selectedTopics,
                onTopicToggled = onTopicToggled,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )
        }
    )
}

@Composable
private fun InterestsScreenBody(
    currentSection: Sections,
    updateSection: (Sections) -> Unit,
    interests: Interests?,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val sectionTitles = Sections.values().map { it.title }

    Column(modifier) {
        TabRow(selectedTabIndex = currentSection.ordinal) {
            sectionTitles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = currentSection.ordinal == index,
                    onClick = {
                        updateSection(Sections.values()[index])
                    }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (interests != null) {
                when (currentSection) {
                    Sections.Topics -> TopicsTab(interests.topics, selectedTopics, onTopicToggled)
                    Sections.People -> PeopleTab(interests.people, selectedTopics, onTopicToggled)
                    Sections.Publications ->
                        PublicationsTab(interests.publications, selectedTopics, onTopicToggled)
                }
            } else if (onRetry != null) {
                ErrorScreen(message = "Can't load your interests", onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun TopicsTab(
    topics: Map<String, List<String>>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    TabWithSections(Sections.Topics.title, topics, selectedTopics, onTopicToggled)
}

@Composable
private fun PeopleTab(
    people: List<String>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    TabWithTopics(Sections.People.title, people, selectedTopics, onTopicToggled)
}

@Composable
private fun PublicationsTab(
    publications: List<String>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    TabWithTopics(Sections.Publications.title, publications, selectedTopics, onTopicToggled)
}

@Composable
private fun TabWithTopics(
    tabName: String,
    topics: List<String>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
        topics.forEach { topic ->
            val key = getTopicKey(tabName, "- ", topic)
            TopicItem(
                itemTitle = topic,
                selected = key in selectedTopics,
                onToggle = { selected -> onTopicToggled(key, selected) }
            )
            TopicDivider()
        }
    }
}

@Composable
private fun TabWithSections(
    tabName: String,
    sections: Map<String, List<String>>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        sections.forEach { (section, topics) ->
            Text(
                text = section,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.subtitle1
            )
            topics.forEach { topic ->
                val key = getTopicKey(tabName, section, topic)
                TopicItem(
                    itemTitle = topic,
                    selected = key in selectedTopics,
                    onToggle = { selected -> onTopicToggled(key, selected) }
                )
                TopicDivider()
            }
        }
    }
}

@Composable
private fun TopicItem(itemTitle: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    val image = painterResource(R.drawable.placeholder_1_1)
    Row(
        modifier = Modifier
            .toggleable(
                value = selected,
                onValueChange = onToggle
            )
            .padding(start = 16.dp, end = 16.dp)
    ) {
        Image(
            image,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(56.dp, 56.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = itemTitle,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(16.dp),
            style = MaterialTheme.typography.subtitle1
        )
        SelectTopicButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            selected = selected
        )
    }
}

@Composable
private fun TopicDivider() {
    Divider(
        modifier = Modifier.padding(start = 72.dp, top = 8.dp, bottom = 8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.08f)
    )
}

private fun getTopicKey(tab: String, group: String, topic: String) = "$tab-$group-$topic"

private fun loadFakeInterests(): Interests {
    val repository = FakeInterestsRepository()
    return Interests(
        topics = previewData(repository::getTopics),
        people = previewData(repository::getPeople),
        publications = previewData(repository::getPublications)
    )
}

@Composable
private fun PreviewInterestsScreenTemplate(scaffoldState: ScaffoldState = rememberScaffoldState()) {
    InterestsScreen(
        interests = loadFakeInterests(),
        selectedTopics = emptySet(),
        onTopicToggled = { _, _ -> },
        onHomeClicked = {},
        scaffoldState = scaffoldState
    )
}

@Preview("Interests screen")
@Composable
fun PreviewInterestsScreen() {
    ThemedPreview {
        PreviewInterestsScreenTemplate()
    }
}

@Preview("Interests screen dark theme")
@Composable
fun PreviewInterestsScreenDark() {
    ThemedPreview(darkThemeColors) {
        PreviewInterestsScreenTemplate(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen drawer open")
@Composable
private fun PreviewDrawerOpen() {
    ThemedPreview {
        PreviewInterestsScreenTemplate(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen drawer open dark theme")
@Composable
private fun PreviewDrawerOpenDark() {
    ThemedPreview(darkThemeColors) {
        PreviewInterestsScreenTemplate(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen topics tab")
@Composable
fun PreviewTopicsTab() {
    ThemedPreview {
        TopicsTab(loadFakeInterests().topics, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen topics tab dark theme")
@Composable
fun PreviewTopicsTabDark() {
    ThemedPreview(darkThemeColors) {
        TopicsTab(loadFakeInterests().topics, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen people tab")
@Composable
fun PreviewPeopleTab() {
    ThemedPreview {
        PeopleTab(loadFakeInterests().people, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen people tab dark theme")
@Composable
fun PreviewPeopleTabDark() {
    ThemedPreview(darkThemeColors) {
        PeopleTab(loadFakeInterests().people, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen publications tab")
@Composable
fun PreviewPublicationsTab() {
    ThemedPreview {
        PublicationsTab(loadFakeInterests().publications, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen publications tab dark theme")
@Composable
fun PreviewPublicationsTabDark() {
    ThemedPreview(darkThemeColors) {
        PublicationsTab(loadFakeInterests().publications, emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen tab with topics")
@Composable
fun PreviewTabWithTopics() {
    ThemedPreview {
        TabWithTopics("preview", listOf("Hello", "Compose"), emptySet(), { _, _ -> })
    }
}

@Preview("Interests screen tab with topics dark theme")
@Composable
fun PreviewTabWithTopicsDark() {
    ThemedPreview {
        TabWithTopics("preview", listOf("Hello", "Compose"), emptySet(), { _, _ -> })
    }
}
