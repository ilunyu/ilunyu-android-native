package com.ilunyu.lunyu.ui.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTabLayout
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.AnalectsLibrary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    library: AnalectsLibrary?,
    activePianSlug: String?,
    onActivePianChanged: (String?) -> Unit,
    favoriteChapterIds: Set<String>,
    pianScrollMap: Map<String, Pair<Int, Int>> = emptyMap(),
    onSavePianScroll: (String, Int, Int) -> Unit = { _, _, _ -> },
    catalogScrollIndex: Int = 0,
    catalogScrollOffset: Int = 0,
    onSaveCatalogScroll: (Int, Int) -> Unit = { _, _ -> },
    onToggleChapterFavorite: (String) -> Unit,
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (library == null || library.pians.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pians = library.pians

    // 拦截返回键：如果处于篇阅读，返回到篇目总览网格
    BackHandler(enabled = activePianSlug != null) {
        onActivePianChanged(null)
    }

    AnimatedContent(
        targetState = activePianSlug == null,
        transitionSpec = {
            if (!targetState) {
                // 进入篇阅读视图（总览 -> 篇阅读）
                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                 scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                (fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                 scaleOut(targetScale = 1.02f, animationSpec = tween(160, easing = FastOutLinearInEasing)))
            } else {
                // 返回篇目总览（篇阅读 -> 总览）
                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                 scaleIn(initialScale = 1.02f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                (fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                 scaleOut(targetScale = 0.96f, animationSpec = tween(160, easing = FastOutLinearInEasing)))
            }
        },
        label = "ReadingOverviewPagerTransition",
        modifier = modifier.fillMaxSize()
    ) { isOverview ->
        if (isOverview) {
            // 1. 篇目总览网格视图（完全对齐 Flutter _CatalogPage）
            val catalogGridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = catalogScrollIndex,
                initialFirstVisibleItemScrollOffset = catalogScrollOffset
            )
            LaunchedEffect(catalogGridState.firstVisibleItemIndex, catalogGridState.firstVisibleItemScrollOffset) {
                onSaveCatalogScroll(catalogGridState.firstVisibleItemIndex, catalogGridState.firstVisibleItemScrollOffset)
            }
            val isCatalogScrolledUnder by remember {
                derivedStateOf {
                    catalogGridState.firstVisibleItemIndex > 0 || catalogGridState.firstVisibleItemScrollOffset > 0
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                LunyuTopBar(
                    showDivider = isCatalogScrolledUnder,
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PianCatalogGrid(
                        pians = pians,
                        gridState = catalogGridState,
                        onSelectPian = { selected ->
                            onActivePianChanged(selected.slug)
                        }
                    )
                }
            }
        } else {
            // 2. 篇阅读视图（完全对齐 Flutter PianReadingPage）
            val initialIndex = pians.indexOfFirst { it.slug == activePianSlug }.coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { pians.size }
            )
            val coroutineScope = rememberCoroutineScope()
            val readingScrollState = rememberLunyuTopBarScrollState()

            // 同步 activePianSlug 变化与 pager
            LaunchedEffect(activePianSlug) {
                val idx = pians.indexOfFirst { it.slug == activePianSlug }
                if (idx >= 0 && idx != pagerState.currentPage) {
                    pagerState.scrollToPage(idx)
                }
            }

            // 同步 pager 滑动到 activePianSlug
            LaunchedEffect(pagerState.currentPage) {
                val slugAtPage = pians[pagerState.currentPage].slug
                if (slugAtPage != activePianSlug) {
                    onActivePianChanged(slugAtPage)
                }
            }

            LunyuCollapsibleTabLayout(
                modifier = Modifier.fillMaxSize(),
                scrollState = readingScrollState,
                topBar = {
                    LunyuTopBar(
                        title = {
                            Text(
                                text = library.source.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { onActivePianChanged(null) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回篇目总览"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    )
                },
                tabBar = {
                    com.ilunyu.lunyu.ui.common.LunyuScrollableTabRow(
                        pagerState = pagerState,
                        tabs = pians.map { it.title },
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
                    val currentPian = pians[page]
                    val prevPian = if (page > 0) pians[page - 1] else null
                    val nextPian = if (page < pians.size - 1) pians[page + 1] else null

                    val pianScroll = pianScrollMap[currentPian.slug]
                    val lazyListState = rememberLazyListState(
                        initialFirstVisibleItemIndex = pianScroll?.first ?: 0,
                        initialFirstVisibleItemScrollOffset = pianScroll?.second ?: 0
                    )
                    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
                        onSavePianScroll(currentPian.slug, lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                    top = 20.dp,
                                    bottom = 20.dp
                                )
                            ) {
                                Text(
                                    text = "共 ${currentPian.chapters.size} 章",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                if (currentPian.comment.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentPian.comment,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = currentPian.chapters,
                            key = { _, chapter -> chapter.id }
                        ) { index, chapter ->
                            val isFav = favoriteChapterIds.contains(chapter.id)
                            PianChapterRow(
                                chapter = chapter,
                                isFavorite = isFav,
                                onToggleFavorite = { onToggleChapterFavorite(chapter.id) },
                                onClick = { onNavigateToChapter(currentPian.slug, chapter.number) },
                                showDivider = index < currentPian.chapters.size - 1
                            )
                        }

                        item {
                            PianSequenceNavigation(
                                prevPian = prevPian,
                                nextPian = nextPian,
                                onNavigateToPian = { target ->
                                    val targetIdx = pians.indexOf(target)
                                    if (targetIdx >= 0) {
                                        coroutineScope.launch {
                                            delay(80)
                                            pagerState.animateScrollToPage(targetIdx)
                                        }
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}
