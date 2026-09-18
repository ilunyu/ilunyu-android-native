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
import com.ilunyu.lunyu.ui.study.ExerciseFilteredEmptyState
import com.ilunyu.lunyu.ui.study.ExerciseTotalEmptyState
import com.ilunyu.lunyu.ui.study.formatExerciseCountText
import com.ilunyu.lunyu.ui.study.DISTRICT_ORDER
import com.ilunyu.lunyu.ui.study.TYPE_ORDER
import com.ilunyu.lunyu.ui.study.sortSources
import com.ilunyu.lunyu.ui.study.sortTypes
import com.ilunyu.lunyu.ui.study.ExerciseListRow
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TagWithCounts
import com.ilunyu.lunyu.data.repository.TAG_PRESET_COLORS
import com.ilunyu.lunyu.ui.tag.TagDeleteConfirmDialog
import com.ilunyu.lunyu.ui.tag.TagEditDialog
import com.ilunyu.lunyu.ui.tag.parseTagColor

// -------------------------------------------------------------
// ...
// -------------------------------------------------------------

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
    allTags: List<TagWithCounts> = emptyList(),
    onCreateTag: (String, String?) -> Unit = { _, _ -> },
    onUpdateTag: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteTag: (String) -> Unit = {},
    onNavigateToTag: (String) -> Unit = {},
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
        initialPage = selectedTab.coerceIn(0, 2),
        pageCount = { 3 }
    )
    val coroutineScope = rememberCoroutineScope()

    var showCreateTagDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<TagEntity?>(null) }
    var tagToDelete by remember { mutableStateOf<TagEntity?>(null) }

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

    // 收藏试题基础数据（未加四个维度筛选前）
    val allFavoriteExercises = remember(favoriteExerciseIds, allExercises) {
        allExercises.filter { favoriteExerciseIds.contains(it.id) }
    }

    // 收藏试题数据过滤与排序
    val favoriteExercises = remember(allFavoriteExercises, sortMode, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        val list = allFavoriteExercises
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
                tabs = listOf("章节", "试题", "标签"),
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
                        ExerciseTotalEmptyState(text = "暂无收藏的章节")
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
                    if (allFavoriteExercises.isEmpty()) {
                        // 总题目数为 0 时，全屏居中统一提示，不显示统计数据和筛选器行
                        ExerciseTotalEmptyState(text = "暂无收藏的试题")
                    } else {
                        val isFiltered = selectedYears.isNotEmpty() || selectedSources.isNotEmpty() || selectedGrades.isNotEmpty() || selectedTypes.isNotEmpty()
                        val countText = formatExerciseCountText(
                            filteredCount = favoriteExercises.size,
                            totalCount = allFavoriteExercises.size,
                            isFiltered = isFiltered
                        )

                        LazyColumn(
                            state = exerciseListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. 统计数据行（未筛选为“共 xx 题”，筛选后为“筛选出 xx 题·共 xx 题”）
                            item(key = "count_header") {
                                Text(
                                    text = countText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp)
                                )
                            }

                            // 2. 筛选器行：挪到“共 xx 题”下方，依次为学年、地区、年级、类别
                            item(key = "filter_chips") {
                                ExerciseFilterChipsRow(
                                    selectedYears = selectedYears,
                                    selectedSources = selectedSources,
                                    selectedGrades = selectedGrades,
                                    selectedTypes = selectedTypes,
                                    onChipClick = { dimension ->
                                        activeFilterDimension = dimension
                                    },
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                                )
                            }

                            if (favoriteExercises.isEmpty()) {
                                // 筛选出 0 题时，保留统计数据和筛选器行，在下方展示统一提示
                                item(key = "empty_filtered") {
                                    ExerciseFilteredEmptyState(text = "暂无符合筛选条件的试题")
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
                2 -> {
                    // 标签聚合视图
                    if (allTags.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "暂无自定义标签",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "在章阅读或试题详情页点击标签图标，即可创建标签并归纳内容",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showCreateTagDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("新建第一个标签", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item(key = "tag_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "共 ${allTags.size} 个标签",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    TextButton(onClick = { showCreateTagDialog = true }) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("新建标签", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            itemsIndexed(
                                items = allTags,
                                key = { _, tagWithCount -> tagWithCount.tag.id }
                            ) { _, tagWithCount ->
                                val tag = tagWithCount.tag
                                val tagColor = parseTagColor(tag.colorHex)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToTag(tag.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(tagColor, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "# ${tag.name}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 16.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${tagWithCount.chapterCount} 章 · ${tagWithCount.exerciseCount} 题",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        IconButton(onClick = { tagToEdit = tag }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "编辑标签",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(onClick = { tagToDelete = tag }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "删除标签",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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

    if (showCreateTagDialog) {
        TagEditDialog(
            initialName = "",
            initialColorHex = TAG_PRESET_COLORS.first(),
            title = "新建标签",
            onConfirm = { name, colorHex ->
                onCreateTag(name, colorHex)
                showCreateTagDialog = false
            },
            onDismiss = { showCreateTagDialog = false }
        )
    }

    tagToEdit?.let { tag ->
        TagEditDialog(
            initialName = tag.name,
            initialColorHex = tag.colorHex,
            title = "编辑标签",
            onConfirm = { name, colorHex ->
                onUpdateTag(tag.id, name, colorHex)
                tagToEdit = null
            },
            onDismiss = { tagToEdit = null }
        )
    }

    tagToDelete?.let { tag ->
        TagDeleteConfirmDialog(
            tagName = tag.name,
            onConfirm = {
                onDeleteTag(tag.id)
                tagToDelete = null
            },
            onDismiss = { tagToDelete = null }
        )
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
