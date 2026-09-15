package com.ilunyu.lunyu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.favorites.FavoritesScreen
import com.ilunyu.lunyu.ui.reading.ChapterScreen
import com.ilunyu.lunyu.ui.reading.ReadingScreen
import com.ilunyu.lunyu.ui.search.SearchScreen
import com.ilunyu.lunyu.ui.settings.SettingsScreen
import com.ilunyu.lunyu.ui.study.ExerciseDetailScreen
import com.ilunyu.lunyu.ui.study.StudyScreen
import kotlinx.coroutines.launch

sealed interface ScreenDestination {
    data class Tab(val index: Int) : ScreenDestination
    data class ChapterDetail(val pianSlug: String, val chapterNumber: Int) : ScreenDestination
    data class ExerciseDetail(val exerciseId: String) : ScreenDestination
    data class Search(val initialTab: Int = 0) : ScreenDestination
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val fontPreference by viewModel.fontPreference.collectAsState()
    val favoriteChapters by viewModel.favoriteChapters.collectAsState()
    val favoriteExercises by viewModel.favoriteExercises.collectAsState()
    val defaultAnswerExpanded by viewModel.defaultAnswerExpanded.collectAsState()
    val library by viewModel.library.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    var activePianSlug by remember { mutableStateOf<String?>(null) }
    var destinationStack by remember { mutableStateOf(listOf<ScreenDestination>(ScreenDestination.Tab(0))) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    val currentDestination = destinationStack.last()
    val currentTab = when (currentDestination) {
        is ScreenDestination.Tab -> currentDestination.index
        is ScreenDestination.ChapterDetail -> 0
        is ScreenDestination.ExerciseDetail -> 1
        is ScreenDestination.Search -> -1
    }

    fun navigateTo(dest: ScreenDestination) {
        isNavigatingBack = false
        destinationStack = destinationStack + dest
    }

    fun replaceTop(dest: ScreenDestination) {
        isNavigatingBack = false
        destinationStack = destinationStack.dropLast(1) + dest
    }

    fun navigateBack() {
        if (destinationStack.size > 1) {
            isNavigatingBack = true
            destinationStack = destinationStack.dropLast(1)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // 硬件返回键处理
    BackHandler(enabled = destinationStack.size > 1) {
        navigateBack()
    }

    val showBottomBar = currentDestination !is ScreenDestination.Search

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    // 1. 阅读
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = {
                            if (currentTab == 0 && (activePianSlug != null || currentDestination is ScreenDestination.ChapterDetail)) {
                                // 在阅读页再次点击阅读 Tab：重置回篇目总览网格（完全对齐 Flutter _openCatalogPage）
                                activePianSlug = null
                            }
                            isNavigatingBack = false
                            destinationStack = listOf(ScreenDestination.Tab(0))
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) {
                                    Icons.AutoMirrored.Filled.MenuBook
                                } else {
                                    Icons.AutoMirrored.Outlined.MenuBook
                                },
                                contentDescription = "阅读"
                            )
                        },
                        label = { Text("阅读") }
                    )

                    // 2. 学习
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = {
                            isNavigatingBack = false
                            destinationStack = listOf(ScreenDestination.Tab(1))
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) {
                                    Icons.Default.School
                                } else {
                                    Icons.Outlined.School
                                },
                                contentDescription = "学习"
                            )
                        },
                        label = { Text("学习") }
                    )

                    // 3. 收藏
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = {
                            isNavigatingBack = false
                            destinationStack = listOf(ScreenDestination.Tab(2))
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "收藏"
                            )
                        },
                        label = { Text("收藏") }
                    )

                    // 4. 设置（按要求：底栏“设置”图标保持不变，使用 Settings 图标）
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = {
                            isNavigatingBack = false
                            destinationStack = listOf(ScreenDestination.Tab(3))
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = "设置"
                            )
                        },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val initial = initialState
                    val target = targetState
                    when {
                        // 1. 同级章节横向切章（ChapterDetail -> ChapterDetail）
                        initial is ScreenDestination.ChapterDetail && target is ScreenDestination.ChapterDetail -> {
                            val pians = library?.pians ?: emptyList()
                            val initPianIdx = pians.indexOfFirst { it.slug == initial.pianSlug }
                            val targetPianIdx = pians.indexOfFirst { it.slug == target.pianSlug }
                            val isForward = if (targetPianIdx != initPianIdx) {
                                targetPianIdx > initPianIdx
                            } else {
                                target.chapterNumber > initial.chapterNumber
                            }
                            if (isForward) {
                                (slideInHorizontally(
                                    initialOffsetX = { (it * 0.35f).toInt() },
                                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(200, easing = LinearOutSlowInEasing)
                                )) togetherWith (
                                    slideOutHorizontally(
                                        targetOffsetX = { -(it * 0.30f).toInt() },
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                                    )
                                )
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { -(it * 0.35f).toInt() },
                                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                                ) + fadeIn(
                                    animationSpec = tween(200, easing = LinearOutSlowInEasing)
                                )) togetherWith (
                                    slideOutHorizontally(
                                        targetOffsetX = { (it * 0.30f).toInt() },
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                                    )
                                )
                            }
                        }

                        // 2. 返回上一级页面（如详情页返回 Tab，或详情页返回 Search）
                        isNavigatingBack -> {
                            (slideInHorizontally(
                                initialOffsetX = { -(it * 0.20f).toInt() },
                                animationSpec = tween(240, easing = FastOutSlowInEasing)
                            ) + fadeIn(
                                animationSpec = tween(220, easing = LinearOutSlowInEasing)
                            )) togetherWith (
                                slideOutHorizontally(
                                    targetOffsetX = { (it * 0.30f).toInt() },
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                                )
                            )
                        }

                        // 3. 底部导航栏 Tab 间切换采用 MD3 Fade Through
                        target is ScreenDestination.Tab -> {
                            (fadeIn(
                                animationSpec = tween(200, delayMillis = 40, easing = LinearOutSlowInEasing)
                            ) + scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(200, delayMillis = 40, easing = FastOutSlowInEasing)
                            )) togetherWith (
                                fadeOut(
                                    animationSpec = tween(140, easing = FastOutLinearInEasing)
                                )
                            )
                        }

                        // 4. 前进进入下一级（如 Tab -> ChapterDetail / ExerciseDetail / Search，或 ChapterDetail -> ExerciseDetail）
                        else -> {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.30f).toInt() },
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            ) + fadeIn(
                                animationSpec = tween(220, easing = LinearOutSlowInEasing)
                            )) togetherWith (
                                slideOutHorizontally(
                                    targetOffsetX = { -(it * 0.20f).toInt() },
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                                )
                            )
                        }
                    }
                },
                label = "MainScreenDestinationTransition",
                modifier = Modifier.fillMaxSize()
            ) { dest ->
                when (dest) {
                    is ScreenDestination.Tab -> {
                        when (dest.index) {
                            0 -> ReadingScreen(
                                library = library,
                                activePianSlug = activePianSlug,
                                onActivePianChanged = { activePianSlug = it },
                                favoriteChapterIds = favoriteChapters,
                                onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) },
                                onNavigateToChapter = { slug, number ->
                                    activePianSlug = slug
                                    navigateTo(ScreenDestination.ChapterDetail(slug, number))
                                },
                                onNavigateToSearch = {
                                    navigateTo(ScreenDestination.Search(0))
                                }
                            )
                            1 -> StudyScreen(
                                exercises = exercises,
                                favoriteExerciseIds = favoriteExercises,
                                onToggleExerciseFavorite = { viewModel.toggleExerciseFavorite(it) },
                                onNavigateToExercise = { exerciseId ->
                                    navigateTo(ScreenDestination.ExerciseDetail(exerciseId))
                                },
                                onNavigateToSearch = {
                                    navigateTo(ScreenDestination.Search(1))
                                }
                            )
                            2 -> FavoritesScreen(
                                favoriteChapterIds = favoriteChapters,
                                favoriteExerciseIds = favoriteExercises,
                                allPians = library?.pians ?: emptyList(),
                                allExercises = exercises,
                                onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) },
                                onToggleExerciseFavorite = { viewModel.toggleExerciseFavorite(it) },
                                onNavigateToChapter = { slug, number ->
                                    activePianSlug = slug
                                    navigateTo(ScreenDestination.ChapterDetail(slug, number))
                                },
                                onNavigateToExercise = { exerciseId ->
                                    navigateTo(ScreenDestination.ExerciseDetail(exerciseId))
                                },
                                onNavigateToSearch = {
                                    navigateTo(ScreenDestination.Search(0))
                                }
                            )
                            3 -> SettingsScreen(
                                currentThemeMode = themeMode,
                                currentFontPreference = fontPreference,
                                onThemeModeChanged = { viewModel.setThemeMode(it) },
                                onFontPreferenceChanged = { viewModel.setFontPreference(it) },
                                defaultAnswerExpanded = defaultAnswerExpanded,
                                onDefaultAnswerExpandedChanged = { viewModel.setDefaultAnswerExpanded(it) }
                            )
                        }
                    }
                    is ScreenDestination.ChapterDetail -> {
                        val chapterData by produceState<Pair<Pian, Chapter>?>(initialValue = null, dest.pianSlug, dest.chapterNumber) {
                            value = viewModel.getChapterDetail(dest.pianSlug, dest.chapterNumber)
                        }
                        val adjacentChapters by produceState<Pair<Pair<Pian, Chapter>?, Pair<Pian, Chapter>?>>(initialValue = null to null, dest.pianSlug, dest.chapterNumber) {
                            value = viewModel.getAdjacentChapters(dest.pianSlug, dest.chapterNumber)
                        }

                        val data = chapterData
                        if (data != null) {
                            val (pian, chapter) = data
                            ChapterScreen(
                                pian = pian,
                                chapter = chapter,
                                prevChapter = adjacentChapters.first,
                                nextChapter = adjacentChapters.second,
                                isFavorite = favoriteChapters.contains(chapter.id),
                                onToggleFavorite = { viewModel.toggleChapterFavorite(chapter.id) },
                                onNavigateToChapter = { slug, number ->
                                    activePianSlug = slug
                                    replaceTop(ScreenDestination.ChapterDetail(slug, number))
                                },
                                onNavigateToExercise = { exerciseId ->
                                    navigateTo(ScreenDestination.ExerciseDetail(exerciseId))
                                },
                                onBack = {
                                    navigateBack()
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is ScreenDestination.ExerciseDetail -> {
                        val exerciseData by produceState<Exercise?>(initialValue = null, dest.exerciseId) {
                            value = viewModel.getExerciseDetail(dest.exerciseId)
                        }
                        val exercise = exerciseData
                        if (exercise != null) {
                            ExerciseDetailScreen(
                                exercise = exercise,
                                isFavorite = favoriteExercises.contains(exercise.id),
                                onToggleFavorite = { viewModel.toggleExerciseFavorite(exercise.id) },
                                defaultAnswerExpanded = defaultAnswerExpanded,
                                onOpenChapterSourceId = { sourceId ->
                                    coroutineScope.launch {
                                        val target = viewModel.getChapterById("$sourceId")
                                        if (target != null) {
                                            activePianSlug = target.first.slug
                                            navigateTo(ScreenDestination.ChapterDetail(target.first.slug, target.second.number))
                                        }
                                    }
                                },
                                onBack = {
                                    navigateBack()
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is ScreenDestination.Search -> {
                        val searchExercises by viewModel.searchExercises.collectAsState()
                        SearchScreen(
                            library = library,
                            exercises = searchExercises,
                            initialTab = dest.initialTab,
                            favoriteChapterIds = favoriteChapters,
                            favoriteExerciseIds = favoriteExercises,
                            onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) },
                            onToggleExerciseFavorite = { viewModel.toggleExerciseFavorite(it) },
                            onNavigateToChapter = { slug, number ->
                                activePianSlug = slug
                                navigateTo(ScreenDestination.ChapterDetail(slug, number))
                            },
                            onNavigateToExercise = { exerciseId ->
                                navigateTo(ScreenDestination.ExerciseDetail(exerciseId))
                            },
                            onBack = {
                                navigateBack()
                            }
                        )
                    }
                }
            }
        }
    }
}
