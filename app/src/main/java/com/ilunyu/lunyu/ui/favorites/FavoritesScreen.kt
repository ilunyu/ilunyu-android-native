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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import com.ilunyu.lunyu.ui.tag.getTagImageVector
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
    selectedTagId: String? = null,
    onSelectTag: (String?) -> Unit = {},
    getTagChapterIds: (String) -> Flow<List<String>> = { emptyFlow() },
    getTagExerciseIds: (String) -> Flow<List<String>> = { emptyFlow() },
    onCreateTag: (String, String?, (TagEntity) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateTag: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
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
        initialPage = selectedTab.coerceIn(0, 1),
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    var tagToEdit by remember { mutableStateOf<TagEntity?>(null) }
    var tagToDelete by remember { mutableStateOf<TagEntity?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        onTabChange(pagerState.currentPage)
    }

    // 当前选中的标签对象
    val activeTagWithCount = remember(selectedTagId, allTags) {
        allTags.find { it.tag.id == selectedTagId }
    }
    val activeTag = activeTagWithCount?.tag

    // 如果选中的标签不存在（例如已被删除），自动回退到收藏
    LaunchedEffect(selectedTagId, allTags) {
        if (selectedTagId != null && allTags.none { it.tag.id == selectedTagId }) {
            onSelectTag(null)
        }
    }

    // 选中标签时关联的章节和试题 ID 流
    val tagChapterIds by remember(selectedTagId) {
        if (selectedTagId != null) getTagChapterIds(selectedTagId) else emptyFlow()
    }.collectAsState(emptyList())

    val tagExerciseIds by remember(selectedTagId) {
        if (selectedTagId != null) getTagExerciseIds(selectedTagId) else emptyFlow()
    }.collectAsState(emptyList())

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

    // 章节数据（根据收藏或当前选中标签动态计算并排序）
    val displayedChapters = remember(selectedTagId, favoriteChapterIds, tagChapterIds, allPians, sortMode) {
        val targetIds = if (selectedTagId == null) favoriteChapterIds else tagChapterIds.toSet()
        val list = mutableListOf<Pair<Pian, Chapter>>()
        for (pian in allPians) {
            for (chapter in pian.chapters) {
                if (targetIds.contains(chapter.id)) {
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

    // 试题基础数据（未加四个维度筛选前）
    val allTargetExercises = remember(selectedTagId, favoriteExerciseIds, tagExerciseIds, allExercises) {
        val targetIds = if (selectedTagId == null) favoriteExerciseIds else tagExerciseIds.toSet()
        allExercises.filter { targetIds.contains(it.id) }
    }

    // 试题过滤与排序
    val displayedExercises = remember(allTargetExercises, sortMode, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        val list = allTargetExercises
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

    // 切换标签或收藏模式时重置滚动到顶部
    LaunchedEffect(selectedTagId) {
        chapterListState.scrollToItem(0, 0)
        exerciseListState.scrollToItem(0, 0)
    }

    val scrollState = rememberLunyuTopBarScrollState()

    LunyuCollapsibleTabLayout(
        modifier = modifier,
        scrollState = scrollState,
        topBar = {
            LunyuTopBar(
                showDivider = false,
                title = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    val configuration = LocalConfiguration.current
                    val maxMenuHeight = minOf(360.dp, (configuration.screenHeightDp - 64).dp)
                    val density = LocalDensity.current
                    val menuScrollState = rememberScrollState()
                    LaunchedEffect(menuExpanded) {
                        if (menuExpanded) {
                            if (selectedTagId == null) {
                                menuScrollState.scrollTo(0)
                            } else {
                                val tagIndex = allTags.indexOfFirst { it.tag.id == selectedTagId }
                                if (tagIndex >= 0) {
                                    val itemHeightPx = with(density) { 56.dp.roundToPx() }
                                    val headerHeightPx = with(density) { 65.dp.roundToPx() }
                                    val menuHeightPx = with(density) { maxMenuHeight.roundToPx() }
                                    val itemCenterY = headerHeightPx + tagIndex * itemHeightPx + itemHeightPx / 2
                                    val targetScroll = maxOf(0, itemCenterY - menuHeightPx / 2)
                                    menuScrollState.scrollTo(targetScroll)
                                }
                            }
                        }
                    }

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { menuExpanded = true }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            if (activeTag != null) {
                                val iconVector = getTagImageVector(activeTag.icon)
                                val tagColor = parseTagColor(activeTag.colorHex)
                                if (iconVector != null) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else if (!activeTag.colorHex.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(tagColor, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = activeTag.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "收藏",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (menuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "切换标签",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            scrollState = menuScrollState,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .widthIn(min = 200.dp, max = 280.dp)
                                .heightIn(max = maxMenuHeight)
                                .layout { measurable, constraints ->
                                    val paddingPx = 8.dp.roundToPx()
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            maxHeight = if (constraints.hasBoundedHeight) {
                                                constraints.maxHeight + paddingPx * 2
                                            } else {
                                                constraints.maxHeight
                                            }
                                        )
                                    )
                                    layout(placeable.width, (placeable.height - paddingPx * 2).coerceAtLeast(0)) {
                                        placeable.place(0, -paddingPx)
                                    }
                                }
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // 1. 全部收藏项
                            val isFavSelected = selectedTagId == null
                            DropdownMenuItem(
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = if (isFavSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "收藏",
                                            fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isFavSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${favoriteChapterIds.size} 章 · ${favoriteExerciseIds.size} 题",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (isFavSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "当前选中",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    if (selectedTagId != null) {
                                        onSelectTag(null)
                                        onUpdateExerciseFilters(emptySet(), emptySet(), emptySet(), emptySet())
                                    }
                                    menuExpanded = false
                                }
                            )

                            HorizontalDivider()

                            // 2. 自定义标签列表
                            if (allTags.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "暂无自定义标签",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    enabled = false,
                                    onClick = {}
                                )
                            } else {
                                allTags.forEach { tagWithCount ->
                                    val tagItem = tagWithCount.tag
                                    val isTagSelected = selectedTagId == tagItem.id
                                    val iconVector = getTagImageVector(tagItem.icon)
                                    val tagColor = parseTagColor(tagItem.colorHex)

                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier.size(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (iconVector != null) {
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = null,
                                                        tint = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else if (!tagItem.colorHex.isNullOrBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .background(tagColor, shape = CircleShape)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.Label,
                                                        contentDescription = null,
                                                        tint = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = tagItem.name,
                                                    fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${tagWithCount.chapterCount} 章 · ${tagWithCount.exerciseCount} 题",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isTagSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "当前选中",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (selectedTagId != tagItem.id) {
                                                onSelectTag(tagItem.id)
                                                onUpdateExerciseFilters(emptySet(), emptySet(), emptySet(), emptySet())
                                            }
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                },
                actions = {
                    if (activeTag != null) {
                        var moreMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { moreMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "更多操作",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = { Text("编辑标签") },
                                    onClick = {
                                        moreMenuExpanded = false
                                        tagToEdit = activeTag
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "删除标签",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        moreMenuExpanded = false
                                        tagToDelete = activeTag
                                    }
                                )
                            }
                        }
                    }
                    // 排序按钮
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
                    // 章节列表
                    if (displayedChapters.isEmpty()) {
                        val emptyText = if (selectedTagId == null) "暂无收藏的章节" else "该标签下暂无关联章节"
                        ExerciseTotalEmptyState(text = emptyText)
                    } else {
                        LazyColumn(
                            state = chapterListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "共 ${displayedChapters.size} 章",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                )
                            }

                            itemsIndexed(
                                items = displayedChapters,
                                key = { _, pair -> pair.second.id }
                            ) { index, (pian, chapter) ->
                                val isFav = favoriteChapterIds.contains(chapter.id)
                                PianChapterRow(
                                    chapter = chapter,
                                    isFavorite = isFav,
                                    onToggleFavorite = { onToggleChapterFavorite(chapter.id) },
                                    onClick = { onNavigateToChapter(pian.slug, chapter.number) },
                                    showDivider = index < displayedChapters.size - 1
                                )
                            }

                            item { Spacer(modifier = Modifier.height(48.dp)) }
                        }
                    }
                }
                1 -> {
                    // 试题列表
                    if (allTargetExercises.isEmpty()) {
                        val emptyText = if (selectedTagId == null) "暂无收藏的试题" else "该标签下暂无关联试题"
                        ExerciseTotalEmptyState(text = emptyText)
                    } else {
                        val isFiltered = selectedYears.isNotEmpty() || selectedSources.isNotEmpty() || selectedGrades.isNotEmpty() || selectedTypes.isNotEmpty()
                        val countText = formatExerciseCountText(
                            filteredCount = displayedExercises.size,
                            totalCount = allTargetExercises.size,
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

                            if (displayedExercises.isEmpty()) {
                                // 筛选出 0 题时，保留统计数据和筛选器行，在下方展示统一提示
                                item(key = "empty_filtered") {
                                    ExerciseFilteredEmptyState(text = "暂无符合筛选条件的试题")
                                }
                            } else {
                                itemsIndexed(
                                    items = displayedExercises,
                                    key = { _, exercise -> exercise.id }
                                ) { index, exercise ->
                                    val isFav = favoriteExerciseIds.contains(exercise.id)
                                    ExerciseListRow(
                                        exercise = exercise,
                                        isFavorite = isFav,
                                        onToggleFavorite = { onToggleExerciseFavorite(exercise.id) },
                                        onClick = { onNavigateToExercise(exercise.id) },
                                        showDivider = index < displayedExercises.size - 1
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


    tagToEdit?.let { tag ->
        TagEditDialog(
            initialName = tag.name,
            initialColorHex = tag.colorHex,
            initialIcon = tag.icon,
            allExistingTags = allTags.map { it.tag },
            title = "编辑标签",
            onConfirm = { name, colorHex, icon ->
                onUpdateTag(tag.id, name, colorHex, icon)
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
                if (selectedTagId == tag.id) {
                    onSelectTag(null)
                }
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
