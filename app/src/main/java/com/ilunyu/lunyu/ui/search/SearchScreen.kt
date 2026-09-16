package com.ilunyu.lunyu.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.common.LunyuFixedTabRow
import com.ilunyu.lunyu.ui.common.LunyuTopBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    library: AnalectsLibrary?,
    exercises: List<Exercise>,
    query: String,
    onQueryChange: (String) -> Unit,
    searchTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    chapterScrollIndex: Int = 0,
    chapterScrollOffset: Int = 0,
    exerciseScrollIndex: Int = 0,
    exerciseScrollOffset: Int = 0,
    onSaveScroll: (isExercise: Boolean, index: Int, offset: Int) -> Unit = { _, _, _ -> },
    favoriteChapterIds: Set<String> = emptySet(),
    favoriteExerciseIds: Set<String> = emptySet(),
    onToggleChapterFavorite: (String) -> Unit = {},
    onToggleExerciseFavorite: (String) -> Unit = {},
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = searchTab.coerceIn(0, 1), pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val chapterListState = rememberLazyListState(
        initialFirstVisibleItemIndex = chapterScrollIndex,
        initialFirstVisibleItemScrollOffset = chapterScrollOffset
    )
    val exerciseListState = rememberLazyListState(
        initialFirstVisibleItemIndex = exerciseScrollIndex,
        initialFirstVisibleItemScrollOffset = exerciseScrollOffset
    )

    LaunchedEffect(pagerState.currentPage) {
        onTabChange(pagerState.currentPage)
    }

    LaunchedEffect(Unit) {
        if (query.isBlank()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(chapterListState.firstVisibleItemIndex, chapterListState.firstVisibleItemScrollOffset) {
        onSaveScroll(false, chapterListState.firstVisibleItemIndex, chapterListState.firstVisibleItemScrollOffset)
    }
    LaunchedEffect(exerciseListState.firstVisibleItemIndex, exerciseListState.firstVisibleItemScrollOffset) {
        onSaveScroll(true, exerciseListState.firstVisibleItemIndex, exerciseListState.firstVisibleItemScrollOffset)
    }

    var previousQuery by rememberSaveable { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != previousQuery) {
            previousQuery = query
            chapterListState.scrollToItem(0, 0)
            exerciseListState.scrollToItem(0, 0)
        }
    }

    val queryTrimmed = query.trim()
    val terms = remember(queryTrimmed) {
        if (queryTrimmed.isBlank()) emptyList()
        else queryTrimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }
    val highlightTerms = terms

    val matchingChapters = remember(terms, library) {
        if (terms.isEmpty() || library == null) emptyList()
        else {
            val list = mutableListOf<Pair<Pian, Chapter>>()
            for (pian in library.pians) {
                val normalizedPianTitle = pian.title.lowercase()
                val normalizedPianShort = pian.shortTitle.lowercase()
                for (chapter in pian.chapters) {
                    val searchable = "${chapter.plainText}\n${chapter.text}\n${chapter.translation}\n${chapter.displayId}\n${chapter.id}\n${pian.title}\n${pian.shortTitle}"
                    val matches = terms.all { term ->
                        val termLower = term.lowercase()
                        val normPian = normalizePianTerm(termLower)
                        searchable.contains(termLower, ignoreCase = true) ||
                            (normPian.isNotEmpty() && (normalizedPianShort.contains(normPian) || normalizedPianTitle.contains(normPian)))
                    }
                    if (matches) {
                        list.add(pian to chapter)
                    }
                }
            }
            list.sortBy { (_, chapter) -> chapter.id.toIntOrNull() ?: Int.MAX_VALUE }
            list
        }
    }

    val matchingExercises = remember(terms, exercises) {
        if (terms.isEmpty()) emptyList()
        else {
            exercises
                .filter { exercise ->
                    val searchable = exercise.toSearchableText()
                    terms.all { term ->
                        searchable.contains(term, ignoreCase = true)
                    }
                }
                .sortedWith(
                    compareBy<Exercise> { if (it.type == "真题") 0 else 1 }
                        .thenByDescending { it.month }
                        .thenBy { it.id }
                )
        }
    }

    var selectedYears by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedSources by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedGrades by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var selectedTypes by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var activeFilterDimension by remember { mutableStateOf<ExerciseFilterDimension?>(null) }

    val availableYears = remember(exercises) {
        exercises.map { it.year }.filter { it.isNotBlank() }.distinct().sortedDescending()
    }
    val availableSources = remember(exercises) {
        val raw = exercises.map { it.source }.filter { it.isNotBlank() }.distinct()
        sortSources((DISTRICT_ORDER + raw).distinct())
    }
    val availableTypes = remember(exercises) {
        val raw = exercises.map { it.type }.filter { it.isNotBlank() }.distinct()
        sortTypes((TYPE_ORDER + raw).distinct())
    }

    val filteredMatchingExercises = remember(matchingExercises, selectedYears, selectedSources, selectedGrades, selectedTypes) {
        matchingExercises.filter {
            (selectedYears.isEmpty() || selectedYears.contains(it.year)) &&
            (selectedSources.isEmpty() || selectedSources.contains(it.source)) &&
            (selectedGrades.isEmpty() || selectedGrades.contains(it.grade)) &&
            (selectedTypes.isEmpty() || selectedTypes.contains(it.type))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                LunyuTopBar(
                    modifier = Modifier.statusBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    title = {
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "搜索论语章节或试题...",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    },
                    actions = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                onQueryChange("")
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LunyuFixedTabRow(
                selectedTabIndex = pagerState.currentPage,
                pagerState = pagerState,
                tabs = listOf("章节", "试题"),
                onTabSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        if (queryTrimmed.isBlank()) {
                            ExerciseTotalEmptyState(text = "输入关键词搜索论语章节与试题")
                        } else if (matchingChapters.isEmpty()) {
                            ExerciseTotalEmptyState(text = "没有找到匹配的章节")
                        } else {
                            LazyColumn(
                                state = chapterListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = "共 ${matchingChapters.size} 章",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                                    )
                                }

                                itemsIndexed(matchingChapters, key = { _, pair -> pair.second.id }) { index, (pian, chapter) ->
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

                                item { Spacer(modifier = Modifier.height(32.dp)) }
                            }
                        }
                    }
                    1 -> {
                        if (queryTrimmed.isBlank()) {
                            ExerciseTotalEmptyState(text = "输入关键词搜索论语章节与试题")
                        } else if (matchingExercises.isEmpty()) {
                            // 搜索无匹配题目（总数为 0 时），全屏居中统一提示，不显示统计数据和筛选器行
                            ExerciseTotalEmptyState(text = "没有找到匹配的试题")
                        } else {
                            val isFiltered = selectedYears.isNotEmpty() || selectedSources.isNotEmpty() || selectedGrades.isNotEmpty() || selectedTypes.isNotEmpty()
                            val countText = formatExerciseCountText(
                                filteredCount = filteredMatchingExercises.size,
                                totalCount = matchingExercises.size,
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

                                if (filteredMatchingExercises.isEmpty()) {
                                    // 筛选出 0 题时，保留统计数据和筛选器行，在下方展示统一提示
                                    item(key = "empty_result") {
                                        ExerciseFilteredEmptyState(text = "暂无符合筛选条件的试题")
                                    }
                                } else {
                                    itemsIndexed(filteredMatchingExercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                                        val isFav = favoriteExerciseIds.contains(exercise.id)
                                        ExerciseListRow(
                                            exercise = exercise,
                                            isFavorite = isFav,
                                            onToggleFavorite = { onToggleExerciseFavorite(exercise.id) },
                                            onClick = { onNavigateToExercise(exercise.id) },
                                            highlightTerms = emptyList(),
                                            showDivider = index < filteredMatchingExercises.size - 1
                                        )
                                    }
                                }

                                item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(32.dp)) }
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
                selectedYears = years
                selectedSources = sources
                selectedGrades = grades
                selectedTypes = types
                activeFilterDimension = null
                coroutineScope.launch {
                    exerciseListState.scrollToItem(0, 0)
                }
            }
        )
    }
}

private fun normalizePianTerm(term: String): String {
    return term
        .replace(Regex("篇?第[〇零一二三四五六七八九十百千万两0-9]+"), "")
        .replace("篇", "")
        .replace("第", "")
}

private fun Exercise.toSearchableText(): String {
    val sb = StringBuilder(512)
    for (block in question) {
        if (block.text.isNotEmpty()) {
            sb.append(block.text).append('\n')
        }
        if (block.blocktitle.isNotEmpty()) {
            sb.append(block.blocktitle).append('\n')
        }
        if (block.sourcename.isNotEmpty()) {
            sb.append(block.sourcename).append('\n')
        }
        for (paragraph in block.paragraphs) {
            if (paragraph.text.isNotEmpty()) {
                sb.append(paragraph.text).append('\n')
            }
        }
    }
    return sb.toString()
}
