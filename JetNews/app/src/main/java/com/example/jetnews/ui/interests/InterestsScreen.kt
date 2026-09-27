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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.jetnews.R
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.DrawerItem
import com.example.jetnews.ui.PreviewInterestsComponent
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.interests.InterestsComponent.Section
import com.example.jetnews.ui.previewData
import kotlinx.coroutines.launch

@Composable
fun InterestsScreen(
    component: InterestsComponent,
    onHomeClicked: () -> Unit,
    onInterestsClicked: () -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {
    val model by component.model.subscribeAsState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentItem = DrawerItem.Interests,
                // Close before navigating, so the drawer isn't restored open when coming back
                onHomeClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                        onHomeClicked()
                    }
                },
                onInterestsClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                        onInterestsClicked()
                    }
                }
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
            InterestsScreenBody(
                model = model,
                onSectionSelected = component::onSectionSelected,
                onTopicToggled = component::onTopicToggled,
                modifier = Modifier.padding(innerPadding)
            )
        }
    )
}

@Composable
private fun InterestsScreenBody(
    model: InterestsComponent.Model,
    onSectionSelected: (Section) -> Unit,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = Section.values()

    Column(modifier) {
        TabRow(selectedTabIndex = model.section.ordinal) {
            sections.forEach { section ->
                Tab(
                    text = { Text(section.title) },
                    selected = model.section == section,
                    onClick = {
                        onSectionSelected(section)
                    }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (model.section) {
                Section.Topics -> {
                    val topicsState = model.topics
                    if (topicsState is UiState.Success) {
                        TopicsTab(topicsState.data, model.selectedTopics, onTopicToggled)
                    }
                }
                Section.People -> {
                    val peopleState = model.people
                    if (peopleState is UiState.Success) {
                        PeopleTab(peopleState.data, model.selectedTopics, onTopicToggled)
                    }
                }
                Section.Publications -> {
                    val publicationsState = model.publications
                    if (publicationsState is UiState.Success) {
                        PublicationsTab(
                            publicationsState.data,
                            model.selectedTopics,
                            onTopicToggled
                        )
                    }
                }
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
    TabWithSections(
        tabName = Section.Topics.title,
        sections = topics,
        selectedTopics = selectedTopics,
        onTopicToggled = onTopicToggled
    )
}

@Composable
private fun PeopleTab(
    people: List<String>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    TabWithTopics(
        tabName = Section.People.title,
        topics = people,
        selectedTopics = selectedTopics,
        onTopicToggled = onTopicToggled
    )
}

@Composable
private fun PublicationsTab(
    publications: List<String>,
    selectedTopics: Set<String>,
    onTopicToggled: (topicKey: String, selected: Boolean) -> Unit
) {
    TabWithTopics(
        tabName = Section.Publications.title,
        topics = publications,
        selectedTopics = selectedTopics,
        onTopicToggled = onTopicToggled
    )
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
            val key = topicKey(tabName, "- ", topic)
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
                val key = topicKey(tabName, section, topic)
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

@Preview("Interests screen")
@Composable
fun PreviewInterestsScreen() {
    ThemedPreview {
        InterestsScreen(
            component = PreviewInterestsComponent(),
            onHomeClicked = {},
            onInterestsClicked = {}
        )
    }
}

@Preview("Interests screen dark theme")
@Composable
fun PreviewInterestsScreenDark() {
    ThemedPreview(darkThemeColors) {
        InterestsScreen(
            component = PreviewInterestsComponent(),
            onHomeClicked = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen drawer open")
@Composable
private fun PreviewDrawerOpen() {
    ThemedPreview {
        InterestsScreen(
            component = PreviewInterestsComponent(),
            onHomeClicked = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen drawer open dark theme")
@Composable
private fun PreviewDrawerOpenDark() {
    ThemedPreview(darkThemeColors) {
        InterestsScreen(
            component = PreviewInterestsComponent(),
            onHomeClicked = {},
            onInterestsClicked = {},
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open))
        )
    }
}

@Preview("Interests screen topics tab")
@Composable
fun PreviewTopicsTab() {
    ThemedPreview {
        TopicsTab(loadFakeTopics(), emptySet()) { _, _ -> }
    }
}

@Preview("Interests screen topics tab dark theme")
@Composable
fun PreviewTopicsTabDark() {
    ThemedPreview(darkThemeColors) {
        TopicsTab(loadFakeTopics(), emptySet()) { _, _ -> }
    }
}

@Composable
private fun loadFakeTopics(): Map<String, List<String>> {
    return previewData(FakeInterestsRepository()::getTopics)
}

@Preview("Interests screen people tab")
@Composable
fun PreviewPeopleTab() {
    ThemedPreview {
        PeopleTab(loadFakePeople(), emptySet()) { _, _ -> }
    }
}

@Preview("Interests screen people tab dark theme")
@Composable
fun PreviewPeopleTabDark() {
    ThemedPreview(darkThemeColors) {
        PeopleTab(loadFakePeople(), emptySet()) { _, _ -> }
    }
}

@Composable
private fun loadFakePeople(): List<String> {
    return previewData(FakeInterestsRepository()::getPeople)
}

@Preview("Interests screen publications tab")
@Composable
fun PreviewPublicationsTab() {
    ThemedPreview {
        PublicationsTab(loadFakePublications(), emptySet()) { _, _ -> }
    }
}

@Preview("Interests screen publications tab dark theme")
@Composable
fun PreviewPublicationsTabDark() {
    ThemedPreview(darkThemeColors) {
        PublicationsTab(loadFakePublications(), emptySet()) { _, _ -> }
    }
}

@Composable
private fun loadFakePublications(): List<String> {
    return previewData(FakeInterestsRepository()::getPublications)
}

@Preview("Interests screen tab with topics")
@Composable
fun PreviewTabWithTopics() {
    ThemedPreview {
        TabWithTopics(
            tabName = "preview",
            topics = listOf("Hello", "Compose"),
            selectedTopics = emptySet(),
            onTopicToggled = { _, _ -> }
        )
    }
}

@Preview("Interests screen tab with topics dark theme")
@Composable
fun PreviewTabWithTopicsDark() {
    ThemedPreview {
        TabWithTopics(
            tabName = "preview",
            topics = listOf("Hello", "Compose"),
            selectedTopics = emptySet(),
            onTopicToggled = { _, _ -> }
        )
    }
}
