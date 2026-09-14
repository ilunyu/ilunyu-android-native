package com.ilunyu.lunyu.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    exercises: List<Exercise>,
    favoriteExerciseIds: Set<String>,
    onToggleExerciseFavorite: (String) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedYears by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGrades by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // 筛选按钮（最左侧）
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "筛选",
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    // 搜索按钮（最右侧）
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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

                itemsIndexed(filteredExercises) { index, exercise ->
                    val isFavorite = favoriteExerciseIds.contains(exercise.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToExercise(exercise.id) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (exercise.year.isNotBlank()) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(exercise.year, fontSize = 11.sp) },
                                        modifier = Modifier.height(24.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        border = null
                                    )
                                }
                                if (exercise.source.isNotBlank()) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(exercise.source, fontSize = 11.sp) },
                                        modifier = Modifier.height(24.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        border = null
                                    )
                                }
                                if (exercise.type.isNotBlank()) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(exercise.type, fontSize = 11.sp) },
                                        modifier = Modifier.height(24.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { onToggleExerciseFavorite(exercise.id) }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < filteredExercises.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var tempYears by remember { mutableStateOf(selectedYears) }
        var tempSources by remember { mutableStateOf(selectedSources) }
        var tempGrades by remember { mutableStateOf(selectedGrades) }
        var tempTypes by remember { mutableStateOf(selectedTypes) }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "筛选题目",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    TextButton(onClick = {
                        tempYears = emptySet()
                        tempSources = emptySet()
                        tempGrades = emptySet()
                        tempTypes = emptySet()
                    }) {
                        Text("清除全部")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 学年
                if (availableYears.isNotEmpty()) {
                    Text(text = "学年", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OptFilterFlowRow(
                        items = availableYears,
                        selected = tempYears,
                        onToggle = { y ->
                            tempYears = if (tempYears.contains(y)) tempYears - y else tempYears + y
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 地区
                if (availableSources.isNotEmpty()) {
                    Text(text = "地区", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OptFilterFlowRow(
                        items = availableSources,
                        selected = tempSources,
                        onToggle = { s ->
                            tempSources = if (tempSources.contains(s)) tempSources - s else tempSources + s
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 年级
                Text(text = "年级", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "高一", 2 to "高二", 3 to "高三").forEach { (g, label) ->
                        FilterChip(
                            selected = tempGrades.contains(g),
                            onClick = {
                                tempGrades = if (tempGrades.contains(g)) tempGrades - g else tempGrades + g
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 题型
                if (availableTypes.isNotEmpty()) {
                    Text(text = "题型", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OptFilterFlowRow(
                        items = availableTypes,
                        selected = tempTypes,
                        onToggle = { t ->
                            tempTypes = if (tempTypes.contains(t)) tempTypes - t else tempTypes + t
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            selectedYears = tempYears
                            selectedSources = tempSources
                            selectedGrades = tempGrades
                            selectedTypes = tempTypes
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("应用")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptFilterFlowRow(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(
                selected = selected.contains(item),
                onClick = { onToggle(item) },
                label = { Text(item) }
            )
        }
    }
}
