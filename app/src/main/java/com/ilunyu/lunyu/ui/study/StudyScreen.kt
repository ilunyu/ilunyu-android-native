package com.ilunyu.lunyu.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilunyu.lunyu.data.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    exercises: List<Exercise>,
    favoriteExerciseIds: Set<String>,
    selectedYears: Set<String> = emptySet(),
    selectedSources: Set<String> = emptySet(),
    selectedGrades: Set<Int> = emptySet(),
    selectedTypes: Set<String> = emptySet(),
    onUpdateFilters: (Set<String>, Set<String>, Set<Int>, Set<String>) -> Unit = { _, _, _, _ -> },
    scrollIndex: Int = 0,
    scrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onToggleExerciseFavorite: (String) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val hasActiveFilters = selectedYears.isNotEmpty() ||
            selectedSources.isNotEmpty() ||
            selectedGrades.isNotEmpty() ||
            selectedTypes.isNotEmpty()

    val availableYears = remember(exercises) {
        exercises.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val availableSources = remember(exercises) {
        exercises.map { it.source }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableTypes = remember(exercises) {
        exercises.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredExercises = remember(exercises, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        exercises.filter {
            (selectedYears.isEmpty() || selectedYears.contains(it.year)) &&
            (selectedSources.isEmpty() || selectedSources.contains(it.source)) &&
            (selectedGrades.isEmpty() || selectedGrades.contains(it.grade)) &&
            (selectedTypes.isEmpty() || selectedTypes.contains(it.type))
        }.sortedWith(
            compareByDescending<Exercise> { it.month }
                .thenByDescending { it.year }
                .thenBy { it.id }
        )
    }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollIndex,
        initialFirstVisibleItemScrollOffset = scrollOffset
    )

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        onSaveScroll(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
    }

    val isScrolledUnder by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        LunyuTopBar(
            showDivider = isScrolledUnder,
            navigationIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(4.dp))
                    // 筛选按钮（左侧）
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "筛选题目",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = {
                // 搜索按钮（右侧）
                IconButton(onClick = onNavigateToSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有符合条件的题目。",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "共 ${filteredExercises.size} 题",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                    )
                }

                itemsIndexed(
                    items = filteredExercises,
                    key = { _, exercise -> exercise.id }
                ) { index, exercise ->
                    val isFav = favoriteExerciseIds.contains(exercise.id)
                    ExerciseListRow(
                        exercise = exercise,
                        isFavorite = isFav,
                        onToggleFavorite = { onToggleExerciseFavorite(exercise.id) },
                        onClick = { onNavigateToExercise(exercise.id) },
                        showDivider = index < filteredExercises.size - 1
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

    if (showFilterDialog) {
        ExerciseFilterDialog(
            availableYears = availableYears,
            availableSources = availableSources,
            availableTypes = availableTypes,
            initialYears = selectedYears,
            initialSources = selectedSources,
            initialGrades = selectedGrades,
            initialTypes = selectedTypes,
            onDismiss = { showFilterDialog = false },
            onApply = { years, sources, grades, types ->
                onUpdateFilters(years, sources, grades, types)
                showFilterDialog = false
                coroutineScope.launch {
                    lazyListState.scrollToItem(0, 0)
                }
            }
        )
    }
}
