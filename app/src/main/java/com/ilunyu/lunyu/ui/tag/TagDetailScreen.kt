package com.ilunyu.lunyu.ui.tag

import com.ilunyu.lunyu.data.repository.TAG_PRESET_COLORS
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.MainViewModel
import com.ilunyu.lunyu.ui.common.LunyuFixedTabRow
import com.ilunyu.lunyu.ui.reading.PianChapterRow
import com.ilunyu.lunyu.ui.study.ExerciseFilterBottomSheet
import com.ilunyu.lunyu.ui.study.ExerciseFilterChipsRow
import com.ilunyu.lunyu.ui.study.ExerciseFilterDimension
import com.ilunyu.lunyu.ui.study.ExerciseFilteredEmptyState
import com.ilunyu.lunyu.ui.study.ExerciseListRow
import com.ilunyu.lunyu.ui.study.formatExerciseCountText
import com.ilunyu.lunyu.ui.study.DISTRICT_ORDER
import com.ilunyu.lunyu.ui.study.TYPE_ORDER
import com.ilunyu.lunyu.ui.study.sortSources
import com.ilunyu.lunyu.ui.study.sortTypes
import kotlinx.coroutines.launch

/**
 * 标签专属聚合浏览页：
 * - 集中展示属于某一标签的全部章节与全部试题；
 * - 沿用系统标准双 Tab（相关章节 / 相关试题），试题列表同样支持 4 维度筛选；
 * - 支持直接重命名标签、更改主题色或删除标签。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tagId: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToChapter: (pianSlug: String, chapterNumber: Int) -> Unit,
    onNavigateToExercise: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tagsWithCounts by viewModel.tagsWithCounts.collectAsState()
    val currentTagItem = tagsWithCounts.find { it.tag.id == tagId }

    if (currentTagItem == null) {
        // 标签若已被删除，自动退回上一层
        onBackClick()
        return
    }

    val tag = currentTagItem.tag
    val tagColor = parseTagColor(tag.colorHex)

    val chapterIds by viewModel.getChapterIdsForTag(tagId).collectAsState(emptyList())
    val exerciseIds by viewModel.getExerciseIdsForTag(tagId).collectAsState(emptyList())

    val library by viewModel.library.collectAsState()
    val allExercises by viewModel.allInstalledExercises.collectAsState()
    val favoriteChapters by viewModel.favoriteChapters.collectAsState()
    val favoriteExercises by viewModel.favoriteExercises.collectAsState()

    // 匹配章节
    val matchingChapters = remember(chapterIds, library) {
        val lib = library ?: return@remember emptyList<Pair<Pian, Chapter>>()
        val idSet = chapterIds.toSet()
        val result = mutableListOf<Pair<Pian, Chapter>>()
        for (pian in lib.pians) {
            for (chapter in pian.chapters) {
                if (idSet.contains(chapter.id)) {
                    result.add(pian to chapter)
                }
            }
        }
        result
    }

    // 匹配试题
    val matchingExercises = remember(exerciseIds, allExercises) {
        val idSet = exerciseIds.toSet()
        allExercises.filter { idSet.contains(it.id) }
    }

    // 试题筛选状态
    var selectedYears by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGrades by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activeFilterDimension by remember { mutableStateOf<ExerciseFilterDimension?>(null) }

    val availableYears = remember(matchingExercises) {
        matchingExercises.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val availableSources = remember(matchingExercises) {
        val raw = matchingExercises.map { it.source }.filter { it.isNotBlank() }.distinct()
        sortSources((DISTRICT_ORDER + raw).distinct())
    }
    val availableTypes = remember(matchingExercises) {
        val raw = matchingExercises.map { it.type }.filter { it.isNotBlank() }.distinct()
        sortTypes((TYPE_ORDER + raw).distinct())
    }

    val filteredExercises = remember(matchingExercises, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        matchingExercises.filter { exercise ->
            val matchYear = selectedYears.isEmpty() || selectedYears.contains(exercise.year)
            val matchSource = selectedSources.isEmpty() || selectedSources.contains(exercise.source)
            val matchGrade = selectedGrades.isEmpty() || selectedGrades.contains(exercise.grade)
            val matchType = selectedTypes.isEmpty() || selectedTypes.contains(exercise.type)
            matchYear && matchSource && matchGrade && matchType
        }
    }

    val allExistingTags = remember(tagsWithCounts) { tagsWithCounts.map { it.tag } }

    // 标签编辑/删除对话框及菜单状态
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "相关章节 (${matchingChapters.size})",
        "相关试题 (${matchingExercises.size})"
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // 顶栏操作行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconVector = getTagImageVector(tag.icon)
                        if (iconVector != null) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (!tag.colorHex.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(tagColor, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "更多操作",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
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
                                    menuExpanded = false
                                    showEditDialog = true
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
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                // 统一的固定双 Tab
                LunyuFixedTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    tabs = tabs,
                    pagerState = pagerState,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> {
                    // 相关章节列表
                    if (matchingChapters.isEmpty()) {
                        ExerciseFilteredEmptyState(text = "该标签下暂无关联章节")
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "chapter_count") {
                                Text(
                                    text = "共 ${matchingChapters.size} 章",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                )
                            }

                            itemsIndexed(
                                items = matchingChapters,
                                key = { _, (_, ch) -> ch.id }
                            ) { index, (pian, chapter) ->
                                val isFav = favoriteChapters.contains(chapter.id)
                                PianChapterRow(
                                    chapter = chapter,
                                    isFavorite = isFav,
                                    onToggleFavorite = { viewModel.toggleChapterFavorite(chapter.id) },
                                    onClick = { onNavigateToChapter(pian.slug, chapter.number) },
                                    showDivider = index < matchingChapters.size - 1
                                )
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }
                    }
                }
                1 -> {
                    // 相关试题列表
                    if (matchingExercises.isEmpty()) {
                        ExerciseFilteredEmptyState(text = "该标签下暂无关联试题")
                    } else {
                        val isFiltered = selectedYears.isNotEmpty() || selectedSources.isNotEmpty() || selectedGrades.isNotEmpty() || selectedTypes.isNotEmpty()
                        val countText = formatExerciseCountText(
                            filteredCount = filteredExercises.size,
                            totalCount = matchingExercises.size,
                            isFiltered = isFiltered
                        )

                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "exercise_count") {
                                Text(
                                    text = countText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp)
                                )
                            }

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

                            if (filteredExercises.isEmpty()) {
                                item(key = "empty_filtered") {
                                    ExerciseFilteredEmptyState(text = "暂无符合筛选条件的试题")
                                }
                            } else {
                                itemsIndexed(
                                    items = filteredExercises,
                                    key = { _, exercise -> exercise.id }
                                ) { index, exercise ->
                                    val isFav = favoriteExercises.contains(exercise.id)
                                    ExerciseListRow(
                                        exercise = exercise,
                                        isFavorite = isFav,
                                        onToggleFavorite = { viewModel.toggleExerciseFavorite(exercise.id) },
                                        onClick = { onNavigateToExercise(exercise.id) },
                                        showDivider = index < filteredExercises.size - 1
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

    // 试题筛选抽屉
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
                selectedYears = years
                selectedSources = sources
                selectedGrades = grades
                selectedTypes = types
                activeFilterDimension = null
            }
        )
    }

    // 编辑标签对话框
    if (showEditDialog) {
        TagEditDialog(
            initialName = tag.name,
            initialColorHex = tag.colorHex,
            initialIcon = tag.icon,
            allExistingTags = allExistingTags,
            onConfirm = { newName, newColorHex, newIcon ->
                viewModel.updateTag(tag.id, newName, newColorHex, newIcon)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // 删除标签确认对话框
    if (showDeleteDialog) {
        TagDeleteConfirmDialog(
            tagName = tag.name,
            onConfirm = {
                viewModel.deleteTag(tag.id)
                showDeleteDialog = false
                onBackClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
