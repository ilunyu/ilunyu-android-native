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
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Color
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
    var activeFilterDimension by remember { mutableStateOf<ExerciseFilterDimension?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
        // 1. 顶栏：移除左上角图标，仅保留右侧搜索按钮
        LunyuTopBar(
            showDivider = false,
            actions = {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        )

        // 2. 列表与统计信息上方的四个 Chips（依次为学年、地区、年级、类别，支持横向延伸滑动）
        ExerciseFilterChipsRow(
            selectedYears = selectedYears,
            selectedSources = selectedSources,
            selectedGrades = selectedGrades,
            selectedTypes = selectedTypes,
            onChipClick = { dimension ->
                activeFilterDimension = dimension
            }
        )

        // 3. 滚动到底部分割线（跟随内容滚动状态）
        HorizontalDivider(
            thickness = 1.dp,
            color = if (isScrolledUnder) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
        )

        // 4. 内容展示区
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
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp)
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

                    item { Spacer(modifier = Modifier.height(48.dp)) }
                }
            }
        }
    }

    // 5. 底部多选列表 BottomSheet 容器
    if (activeFilterDimension != null) {
        ExerciseFilterBottomSheet(
            dimension = activeFilterDimension!!,
            availableYears = availableYears,
            availableSources = availableSources,
            availableTypes = availableTypes,
            selectedYears = selectedYears,
            selectedSources = selectedSources,
            selectedGrades = selectedGrades,
            selectedTypes = selectedTypes,
            onDismiss = { activeFilterDimension = null },
            onApply = { years, sources, grades, types ->
                onUpdateFilters(years, sources, grades, types)
                activeFilterDimension = null
                coroutineScope.launch {
                    lazyListState.scrollToItem(0, 0)
                }
            }
        )
    }
}
