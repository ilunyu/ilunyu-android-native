package com.ilunyu.lunyu.ui

import androidx.activity.compose.BackHandler
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
    data object Search : ScreenDestination
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
    val library by viewModel.library.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activePianSlug by remember { mutableStateOf<String?>(null) }
    var currentDestination by remember { mutableStateOf<ScreenDestination>(ScreenDestination.Tab(0)) }
    val coroutineScope = rememberCoroutineScope()

    // 硬件返回键处理
    BackHandler(enabled = currentDestination !is ScreenDestination.Tab) {
        when (currentDestination) {
            is ScreenDestination.Search -> {
                currentDestination = ScreenDestination.Tab(selectedTab)
            }
            is ScreenDestination.ChapterDetail -> {
                // 返回阅读 Tab（保持在当前篇）
                currentDestination = ScreenDestination.Tab(0)
            }
            is ScreenDestination.ExerciseDetail -> {
                currentDestination = ScreenDestination.Tab(selectedTab)
            }
            else -> {
                currentDestination = ScreenDestination.Tab(selectedTab)
            }
        }
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
                        selected = selectedTab == 0 && currentDestination !is ScreenDestination.ExerciseDetail,
                        onClick = {
                            if (selectedTab == 0 && (activePianSlug != null || currentDestination is ScreenDestination.ChapterDetail)) {
                                // 在阅读页再次点击阅读 Tab：重置回篇目总览网格（完全对齐 Flutter _openCatalogPage）
                                activePianSlug = null
                            }
                            selectedTab = 0
                            currentDestination = ScreenDestination.Tab(0)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0 && currentDestination !is ScreenDestination.ExerciseDetail) {
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
                        selected = selectedTab == 1 && currentDestination !is ScreenDestination.ChapterDetail,
                        onClick = {
                            selectedTab = 1
                            currentDestination = ScreenDestination.Tab(1)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1 && currentDestination !is ScreenDestination.ChapterDetail) {
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
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            currentDestination = ScreenDestination.Tab(2)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "收藏"
                            )
                        },
                        label = { Text("收藏") }
                    )

                    // 4. 设置（按要求：底栏“设置”图标保持不变，使用 Settings 图标）
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            currentDestination = ScreenDestination.Tab(3)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
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
            when (val dest = currentDestination) {
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
                                currentDestination = ScreenDestination.ChapterDetail(slug, number)
                            },
                            onNavigateToSearch = {
                                currentDestination = ScreenDestination.Search
                            }
                        )
                        1 -> StudyScreen(
                            exercises = exercises,
                            favoriteExerciseIds = favoriteExercises,
                            onToggleExerciseFavorite = { viewModel.toggleExerciseFavorite(it) },
                            onNavigateToExercise = { exerciseId ->
                                currentDestination = ScreenDestination.ExerciseDetail(exerciseId)
                            },
                            onNavigateToSearch = {
                                currentDestination = ScreenDestination.Search
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
                                currentDestination = ScreenDestination.ChapterDetail(slug, number)
                            },
                            onNavigateToExercise = { exerciseId ->
                                currentDestination = ScreenDestination.ExerciseDetail(exerciseId)
                            },
                            onNavigateToSearch = {
                                currentDestination = ScreenDestination.Search
                            }
                        )
                        3 -> SettingsScreen(
                            currentThemeMode = themeMode,
                            currentFontPreference = fontPreference,
                            onThemeModeChanged = { viewModel.setThemeMode(it) },
                            onFontPreferenceChanged = { viewModel.setFontPreference(it) }
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
                                currentDestination = ScreenDestination.ChapterDetail(slug, number)
                            },
                            onNavigateToExercise = { exerciseId ->
                                currentDestination = ScreenDestination.ExerciseDetail(exerciseId)
                            },
                            onBack = {
                                currentDestination = ScreenDestination.Tab(selectedTab)
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
                            onOpenChapterSourceId = { sourceId ->
                                coroutineScope.launch {
                                    val target = viewModel.getChapterById("$sourceId")
                                    if (target != null) {
                                        activePianSlug = target.first.slug
                                        currentDestination = ScreenDestination.ChapterDetail(target.first.slug, target.second.number)
                                    }
                                }
                            },
                            onBack = {
                                currentDestination = ScreenDestination.Tab(selectedTab)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ScreenDestination.Search -> {
                    SearchScreen(
                        library = library,
                        exercises = exercises,
                        onNavigateToChapter = { slug, number ->
                            activePianSlug = slug
                            currentDestination = ScreenDestination.ChapterDetail(slug, number)
                        },
                        onNavigateToExercise = { exerciseId ->
                            currentDestination = ScreenDestination.ExerciseDetail(exerciseId)
                        },
                        onBack = {
                            currentDestination = ScreenDestination.Tab(selectedTab)
                        }
                    )
                }
            }
        }
    }
}
