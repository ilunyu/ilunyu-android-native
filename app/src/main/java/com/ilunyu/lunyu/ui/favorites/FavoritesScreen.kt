package com.ilunyu.lunyu.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTabLayout
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.reading.PianChapterRow
import com.ilunyu.lunyu.ui.study.ExerciseFilterDimension
import com.ilunyu.lunyu.ui.study.ExerciseFilterChipsRow
import com.ilunyu.lunyu.ui.study.ExerciseFilterBottomSheet
import com.ilunyu.lunyu.ui.study.DISTRICT_ORDER
import com.ilunyu.lunyu.ui.study.TYPE_ORDER
import com.ilunyu.lunyu.ui.study.sortSources
import com.ilunyu.lunyu.ui.study.sortTypes
import com.ilunyu.lunyu.ui.study.ExerciseListRow
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
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    sortMode: FavoritesSortMode = FavoritesSortMode.DEFAULT,
    onUpdateSortMode: (FavoritesSortMode) -> Unit = {},
    selectedYears: Set<String> = emptySet(),
    selectedSources: Set<String> = emptySet(),
    selectedGrades: Set<Int> = emptySet(),
    selectedTypes: Set<String> = emptySet(),
    onUpdateExerciseFilters: (Set<String>, Set<String>, Set<Int>, Set<String>) -> Unit = { _, _, _, _ -> },
    chapterScrollIndex: Int = 0,
    chapterScrollOffset: Int = 0,
    exerciseScrollIndex: Int = 0,
    exerciseScrollOffset: Int = 0,
    onSaveScroll: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    onToggleChapterFavorite: (String) -> Unit,
    onToggleExerciseFavorite: (String) -> Unit,
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = selectedTab,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onTabChange(pagerState.currentPage)
    }

    // 试题筛选状态
    var activeFilterDimension by remember { mutableStateOf<ExerciseFilterDimension?>(null) }

    val availableYears = remember(allExercises) {
        allExercises.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val availableSources = remember(allExercises) {
        val raw = allExercises.map { it.source }.filter { it.isNotBlank() }.distinct()
        sortSources((DISTRICT_ORDER + raw).distinct())
    }
    val availableTypes = remember(allExercises) {
        val raw = allExercises.map { it.type }.filter { it.isNotBlank() }.distinct()
        sortTypes((TYPE_ORDER + raw).distinct())
    }

    // 收藏章节数据过滤与排序
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

    // 收藏试题数据过滤与排序
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

    val chapterListState = rememberLazyListState(
        initialFirstVisibleItemIndex = chapterScrollIndex,
        initialFirstVisibleItemScrollOffset = chapterScrollOffset
    )
    val exerciseListState = rememberLazyListState(
        initialFirstVisibleItemIndex = exerciseScrollIndex,
        initialFirstVisibleItemScrollOffset = exerciseScrollOffset
    )

    LaunchedEffect(chapterListState.firstVisibleItemIndex, chapterListState.firstVisibleItemScrollOffset) {
        onSaveScroll(false, chapterListState.firstVisibleItemIndex, chapterListState.firstVisibleItemScrollOffset)
    }

    LaunchedEffect(exerciseListState.firstVisibleItemIndex, exerciseListState.firstVisibleItemScrollOffset) {
        onSaveScroll(true, exerciseListState.firstVisibleItemIndex, exerciseListState.firstVisibleItemScrollOffset)
    }

    val scrollState = rememberLunyuTopBarScrollState()

    LunyuCollapsibleTabLayout(
        modifier = modifier,
        scrollState = scrollState,
        topBar = {
            LunyuTopBar(
                showDivider = false,
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // 排序按钮（最左侧）
                        IconButton(
                            onClick = {
                                val nextMode = when (sortMode) {
                                    FavoritesSortMode.DEFAULT -> FavoritesSortMode.NEWEST_FIRST
                                    FavoritesSortMode.NEWEST_FIRST -> FavoritesSortMode.OLDEST_FIRST
                                    FavoritesSortMode.OLDEST_FIRST -> FavoritesSortMode.DEFAULT
                                }
                                onUpdateSortMode(nextMode)
                                coroutineScope.launch {
                                    chapterListState.scrollToItem(0, 0)
                                    exerciseListState.scrollToItem(0, 0)
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
                                    FavoritesSortMode.DEFAULT -> "默认排序（点击切换为最近收藏）"
                                    FavoritesSortMode.NEWEST_FIRST -> "最近收藏（点击切换为最早收藏）"
                                    FavoritesSortMode.OLDEST_FIRST -> "最早收藏（点击切换为默认排序）"
                                }
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
                }
            )
        },
        tabBar = {
            com.ilunyu.lunyu.ui.common.LunyuFixedTabRow(
                selectedTabIndex = pagerState.currentPage,
                pagerState = pagerState,
                tabs = listOf("章节", "试题"),
                onTabSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // 章节收藏列表
                    if (favoriteChapters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无收藏的章节。",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            state = chapterListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "共 ${favoriteChapters.size} 章",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                )
                            }

                            itemsIndexed(
                                items = favoriteChapters,
                                key = { _, pair -> pair.second.id }
                            ) { index, (pian, chapter) ->
                                PianChapterRow(
                                    chapter = chapter,
                                    isFavorite = true,
                                    onToggleFavorite = { onToggleChapterFavorite(chapter.id) },
                                    onClick = { onNavigateToChapter(pian.slug, chapter.number) },
                                    showDivider = index < favoriteChapters.size - 1
                                )
                            }

                            item { Spacer(modifier = Modifier.height(48.dp)) }
                        }
                    }
                }
                1 -> {
                    // 试题收藏列表
                    val hasAnyFavorites = favoriteExerciseIds.isNotEmpty()
                    if (!hasAnyFavorites) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无收藏的试题。",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            state = exerciseListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 列表顶部的四个 Chips（学年、地区、年级、类别）
                            item(key = "filter_chips") {
                                ExerciseFilterChipsRow(
                                    selectedYears = selectedYears,
                                    selectedSources = selectedSources,
                                    selectedGrades = selectedGrades,
                                    selectedTypes = selectedTypes,
                                    onChipClick = { dimension ->
                                        activeFilterDimension = dimension
                                    },
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            item(key = "count_header") {
                                Text(
                                    text = "共 ${favoriteExercises.size} 题",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
                                )
                            }

                            if (favoriteExercises.isEmpty()) {
                                item(key = "empty_filtered") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "暂无符合筛选条件的收藏试题",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = favoriteExercises,
                                    key = { _, exercise -> exercise.id }
                                ) { index, exercise ->
                                    val isFav = favoriteExerciseIds.contains(exercise.id)
                                    ExerciseListRow(
                                        exercise = exercise,
                                        isFavorite = isFav,
                                        onToggleFavorite = { onToggleExerciseFavorite(exercise.id) },
                                        onClick = { onNavigateToExercise(exercise.id) },
                                        showDivider = index < favoriteExercises.size - 1
                                    )
                                }
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }

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
                onUpdateExerciseFilters(years, sources, grades, types)
                activeFilterDimension = null
                coroutineScope.launch {
                    exerciseListState.scrollToItem(0, 0)
                }
            }
        )
    }
}
