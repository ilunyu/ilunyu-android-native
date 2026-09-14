package com.ilunyu.lunyu.ui.favorites

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import kotlinx.coroutines.launch

enum class FavoritesSortMode {
    DEFAULT,
    NEWEST_FIRST,
    OLDEST_FIRST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteChapterIds: Set<String>,
    favoriteExerciseIds: Set<String>,
    allPians: List<Pian>,
    allExercises: List<Exercise>,
    onToggleChapterFavorite: (String) -> Unit,
    onToggleExerciseFavorite: (String) -> Unit,
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var sortMode by remember { mutableStateOf(FavoritesSortMode.DEFAULT) }

    // Exercise filters
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedYears by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGrades by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }

    val hasActiveExerciseFilters = selectedYears.isNotEmpty() ||
            selectedSources.isNotEmpty() ||
            selectedGrades.isNotEmpty() ||
            selectedTypes.isNotEmpty()

    val availableYears = remember(allExercises) {
        allExercises.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val availableSources = remember(allExercises) {
        allExercises.map { it.source }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableTypes = remember(allExercises) {
        allExercises.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Resolve favorite chapters
    val favoriteChapters = remember(favoriteChapterIds, allPians, sortMode) {
        val list = mutableListOf<Pair<Pian, Chapter>>()
        for (pian in allPians) {
            for (chapter in pian.chapters) {
                if (favoriteChapterIds.contains(chapter.id)) {
                    list.add(pian to chapter)
                }
            }
        }
        when (sortMode) {
            FavoritesSortMode.DEFAULT -> list.sortedBy { it.second.id.toIntOrNull() ?: 0 }
            FavoritesSortMode.NEWEST_FIRST -> list.reversed()
            FavoritesSortMode.OLDEST_FIRST -> list
        }
    }

    // Resolve favorite exercises
    val favoriteExercises = remember(favoriteExerciseIds, allExercises, sortMode, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        val list = allExercises.filter { favoriteExerciseIds.contains(it.id) }
            .filter { selectedYears.isEmpty() || selectedYears.contains(it.year) }
            .filter { selectedSources.isEmpty() || selectedSources.contains(it.source) }
            .filter { selectedGrades.isEmpty() || selectedGrades.contains(it.grade) }
            .filter { selectedTypes.isEmpty() || selectedTypes.contains(it.type) }
        when (sortMode) {
            FavoritesSortMode.DEFAULT -> list.sortedWith(
                compareByDescending<Exercise> { it.month }
                    .thenByDescending { it.year }
                    .thenBy { it.id }
            )
            FavoritesSortMode.NEWEST_FIRST -> list.reversed()
            FavoritesSortMode.OLDEST_FIRST -> list
        }
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
                        // 排序按钮（最左侧）
                        IconButton(
                            onClick = {
                                sortMode = when (sortMode) {
                                    FavoritesSortMode.DEFAULT -> FavoritesSortMode.NEWEST_FIRST
                                    FavoritesSortMode.NEWEST_FIRST -> FavoritesSortMode.OLDEST_FIRST
                                    FavoritesSortMode.OLDEST_FIRST -> FavoritesSortMode.DEFAULT
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (sortMode) {
                                    FavoritesSortMode.DEFAULT -> Icons.AutoMirrored.Filled.Sort
                                    FavoritesSortMode.NEWEST_FIRST -> Icons.Default.ArrowDownward
                                    FavoritesSortMode.OLDEST_FIRST -> Icons.Default.ArrowUpward
                                },
                                contentDescription = when (sortMode) {
                                    FavoritesSortMode.DEFAULT -> "默认排序"
                                    FavoritesSortMode.NEWEST_FIRST -> "最近收藏"
                                    FavoritesSortMode.OLDEST_FIRST -> "最早收藏"
                                }
                            )
                        }
                        // 筛选按钮（仅在试题 tab 展示，位于排序按钮右侧）
                        if (pagerState.currentPage == 1) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "筛选",
                                    tint = if (hasActiveExerciseFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                divider = {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text("章节") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text("试题") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // 章节收藏列表
                    if (favoriteChapters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "还没有收藏章节",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = "共 ${favoriteChapters.size} 章",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                )
                            }
                            itemsIndexed(favoriteChapters) { index, item ->
                                val (pian, chapter) = item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToChapter(pian.slug, chapter.number) }
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${pian.shortTitle} ${chapter.displayId}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = chapter.plainText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { onToggleChapterFavorite(chapter.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "取消收藏",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (index < favoriteChapters.size - 1) {
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
                } else {
                    // 试题收藏列表
                    if (favoriteExercises.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "还没有收藏试题",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = "共 ${favoriteExercises.size} 题",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                )
                            }
                            itemsIndexed(favoriteExercises) { index, exercise ->
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
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "取消收藏",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (index < favoriteExercises.size - 1) {
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
