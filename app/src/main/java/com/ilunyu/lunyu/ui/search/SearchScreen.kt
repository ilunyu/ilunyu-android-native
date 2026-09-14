package com.ilunyu.lunyu.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.reading.PianChapterRow
import com.ilunyu.lunyu.ui.study.ExerciseListRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    library: AnalectsLibrary?,
    exercises: List<Exercise>,
    favoriteChapterIds: Set<String> = emptySet(),
    favoriteExerciseIds: Set<String> = emptySet(),
    onToggleChapterFavorite: (String) -> Unit = {},
    onToggleExerciseFavorite: (String) -> Unit = {},
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val queryTrimmed = query.trim()
    val highlightTerms = remember(queryTrimmed) {
        if (queryTrimmed.isBlank()) emptyList() else listOf(queryTrimmed)
    }

    val matchingChapters = remember(queryTrimmed, library) {
        if (queryTrimmed.isBlank() || library == null) emptyList()
        else {
            val list = mutableListOf<Pair<Pian, Chapter>>()
            for (pian in library.pians) {
                for (chapter in pian.chapters) {
                    if (chapter.plainText.contains(queryTrimmed, ignoreCase = true) ||
                        chapter.text.contains(queryTrimmed, ignoreCase = true) ||
                        chapter.translation.contains(queryTrimmed, ignoreCase = true) ||
                        chapter.displayId.contains(queryTrimmed, ignoreCase = true) ||
                        pian.title.contains(queryTrimmed, ignoreCase = true)
                    ) {
                        list.add(pian to chapter)
                    }
                }
            }
            list
        }
    }

    val matchingExercises = remember(queryTrimmed, exercises) {
        if (queryTrimmed.isBlank()) emptyList()
        else {
            exercises.filter {
                it.title.contains(queryTrimmed, ignoreCase = true) ||
                it.source.contains(queryTrimmed, ignoreCase = true) ||
                it.year.contains(queryTrimmed, ignoreCase = true) ||
                it.type.contains(queryTrimmed, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("搜索论语章节或试题...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            com.ilunyu.lunyu.ui.common.LunyuFixedTabRow(
                selectedTabIndex = selectedTab,
                tabs = listOf("章节 (${matchingChapters.size})", "试题 (${matchingExercises.size})"),
                onTabSelected = { selectedTab = it }
            )

            if (queryTrimmed.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "输入关键词搜索论语章节与试题",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else if (selectedTab == 0) {
                if (matchingChapters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "没有找到匹配的章节",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(matchingChapters) { index, (pian, chapter) ->
                            val isFav = favoriteChapterIds.contains(chapter.id)
                            PianChapterRow(
                                chapter = chapter,
                                isFavorite = isFav,
                                onToggleFavorite = { onToggleChapterFavorite(chapter.id) },
                                onClick = { onNavigateToChapter(pian.slug, chapter.number) },
                                highlightTerms = highlightTerms,
                                showDivider = index < matchingChapters.size - 1
                            )
                        }
                    }
                }
            } else {
                if (matchingExercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "没有找到匹配的试题",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(matchingExercises) { index, exercise ->
                            val isFav = favoriteExerciseIds.contains(exercise.id)
                            ExerciseListRow(
                                exercise = exercise,
                                isFavorite = isFav,
                                onToggleFavorite = { onToggleExerciseFavorite(exercise.id) },
                                onClick = { onNavigateToExercise(exercise.id) },
                                highlightTerms = highlightTerms,
                                showDivider = index < matchingExercises.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}
