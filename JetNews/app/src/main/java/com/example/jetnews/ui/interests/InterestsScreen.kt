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
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.JetnewsStatus
import com.example.jetnews.ui.Screen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.UiState
import com.example.jetnews.ui.darkThemeColors
import com.example.jetnews.ui.previewDataFrom
import com.example.jetnews.ui.uiStateFrom
import kotlinx.coroutines.launch

private enum class Sections(val title: String) {
    Topics("Topics"),
    People("People"),
    Publications("Publications")
}

@Composable
fun InterestsScreen(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    interestsRepository: InterestsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentScreen = Screen.Interests,
                closeDrawer = { coroutineScope.launch { scaffoldState.drawerState.close() } }
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
                currentSection,
                updateSection,
                interestsRepository,
                Modifier.padding(innerPadding)
            )
        }
    )
}

@Composable
private fun InterestsScreenBody(
    currentSection: Sections,
    updateSection: (Sections) -> Unit,
    interestsRepository: InterestsRepository,
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
            when (currentSection) {
                Sections.Topics -> {
                    val topicsState = uiStateFrom(interestsRepository::getTopics)
                    if (topicsState is UiState.Success) {
                        TopicsTab(topicsState.data)
                    }
                }
                Sections.People -> {
                    val peopleState = uiStateFrom(interestsRepository::getPeople)
                    if (peopleState is UiState.Success) {
                        PeopleTab(peopleState.data)
                    }
                }
                Sections.Publications -> {
                    val publicationsState = uiStateFrom(interestsRepository::getPublications)
                    if (publicationsState is UiState.Success) {
                        PublicationsTab(publicationsState.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicsTab(topics: Map<String, List<String>>) {
    TabWithSections(tabName = Sections.Topics.title, sections = topics)
}

@Composable
private fun PeopleTab(people: List<String>) {
    TabWithTopics(tabName = Sections.People.title, topics = people)
}

@Composable
private fun PublicationsTab(publications: List<String>) {
    TabWithTopics(tabName = Sections.Publications.title, topics = publications)
}

@Composable
private fun TabWithTopics(tabName: String, topics: List<String>) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
        topics.forEach { topic ->
            TopicItem(
                getTopicKey(
                    tabName,
                    "- ",
                    topic
                ),
                topic
            )
            TopicDivider()
        }
    }
}

@Composable
private fun TabWithSections(
    tabName: String,
    sections: Map<String, List<String>>
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        sections.forEach { (section, topics) ->
            Text(
                text = section,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.subtitle1
            )
            topics.forEach { topic ->
                TopicItem(
                    getTopicKey(
                        tabName,
                        section,
                        topic
                    ), topic
                )
                TopicDivider()
            }
        }
    }
}

@Composable
private fun TopicItem(topicKey: String, itemTitle: String) {
    val image = painterResource(R.drawable.placeholder_1_1)
    val selected = isTopicSelected(topicKey)
    val onSelected = { it: Boolean ->
        selectTopic(topicKey, it)
    }
    Row(
        modifier = Modifier
            .toggleable(
                value = selected,
                onValueChange = onSelected
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

private fun isTopicSelected(key: String) = JetnewsStatus.selectedTopics.contains(key)

private fun selectTopic(key: String, select: Boolean) {
    if (select) {
        JetnewsStatus.selectedTopics.add(key)
    } else {
        JetnewsStatus.selectedTopics.remove(key)
    }
}

@Preview("Interests screen")
@Composable
fun PreviewInterestsScreen() {
    ThemedPreview {
        InterestsScreen(interestsRepository = FakeInterestsRepository())
    }
}

@Preview("Interests screen dark theme")
@Composable
fun PreviewInterestsScreenDark() {
    ThemedPreview(darkThemeColors) {
        InterestsScreen(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open)),
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen drawer open")
@Composable
private fun PreviewDrawerOpen() {
    ThemedPreview {
        InterestsScreen(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open)),
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen drawer open dark theme")
@Composable
private fun PreviewDrawerOpenDark() {
    ThemedPreview(darkThemeColors) {
        InterestsScreen(
            scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Open)),
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen topics tab")
@Composable
fun PreviewTopicsTab() {
    ThemedPreview {
        TopicsTab(loadFakeTopics())
    }
}

@Preview("Interests screen topics tab dark theme")
@Composable
fun PreviewTopicsTabDark() {
    ThemedPreview(darkThemeColors) {
        TopicsTab(loadFakeTopics())
    }
}

@Composable
private fun loadFakeTopics(): Map<String, List<String>> {
    return previewDataFrom(FakeInterestsRepository()::getTopics)
}

@Preview("Interests screen people tab")
@Composable
fun PreviewPeopleTab() {
    ThemedPreview {
        PeopleTab(loadFakePeople())
    }
}

@Preview("Interests screen people tab dark theme")
@Composable
fun PreviewPeopleTabDark() {
    ThemedPreview(darkThemeColors) {
        PeopleTab(loadFakePeople())
    }
}

@Composable
private fun loadFakePeople(): List<String> {
    return previewDataFrom(FakeInterestsRepository()::getPeople)
}

@Preview("Interests screen publications tab")
@Composable
fun PreviewPublicationsTab() {
    ThemedPreview {
        PublicationsTab(loadFakePublications())
    }
}

@Preview("Interests screen publications tab dark theme")
@Composable
fun PreviewPublicationsTabDark() {
    ThemedPreview(darkThemeColors) {
        PublicationsTab(loadFakePublications())
    }
}

@Composable
private fun loadFakePublications(): List<String> {
    return previewDataFrom(FakeInterestsRepository()::getPublications)
}

@Preview("Interests screen tab with topics")
@Composable
fun PreviewTabWithTopics() {
    ThemedPreview {
        TabWithTopics(tabName = "preview", topics = listOf("Hello", "Compose"))
    }
}

@Preview("Interests screen tab with topics dark theme")
@Composable
fun PreviewTabWithTopicsDark() {
    ThemedPreview {
        TabWithTopics(tabName = "preview", topics = listOf("Hello", "Compose"))
    }
}
