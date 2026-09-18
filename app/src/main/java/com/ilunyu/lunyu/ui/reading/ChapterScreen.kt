package com.ilunyu.lunyu.ui.reading

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Job
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.roundToInt
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TagWithCounts
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Pian
import com.ilunyu.lunyu.ui.tag.AddTagChip
import com.ilunyu.lunyu.ui.tag.AddTagDialog
import com.ilunyu.lunyu.ui.tag.TagChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 拖拽排序标签时，末尾展示的红色垃圾桶移除插槽（尺寸与添加标签 Chip 完全固定一致，悬停时不形变）
 */
@Composable
private fun DeleteDropTagChip(
    isHovering: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isHovering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(durationMillis = 150),
        label = "delete_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isHovering) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(durationMillis = 150),
        label = "delete_chip_content"
    )

    FilterChip(
        selected = false,
        onClick = {},
        label = {
            Text(
                text = "移除",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isHovering) Icons.Filled.Delete else Icons.Outlined.Delete,
                contentDescription = "移除标签",
                modifier = Modifier.size(16.dp)
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            iconColor = contentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = if (isHovering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    pian: Pian,
    chapter: Chapter,
    prevChapter: Pair<Pian, Chapter>?,
    nextChapter: Pair<Pian, Chapter>?,
    isFavorite: Boolean,
    attachedTags: List<TagEntity> = emptyList(),
    allTags: List<TagWithCounts> = emptyList(),
    onToggleTag: (String) -> Unit = {},
    onCreateTag: (String, String?, String?) -> Unit = { _, _, _ -> },
    onNavigateToTag: (String) -> Unit = {},
    onReorderTags: (List<String>) -> Unit = {},
    scrollIndex: Int = 0,
    scrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onToggleFavorite: () -> Unit,
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var showAddTagDialog by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var highlightedAnnotationIndex by remember(chapter.id) { mutableStateOf<Int?>(null) }
    val highlightProgress = remember(chapter.id) { Animatable(0f) }
    var highlightJob by remember(chapter.id) { mutableStateOf<Job?>(null) }
    var scrollJob by remember(chapter.id) { mutableStateOf<Job?>(null) }

    // 标签行内拖拽排序与删除状态（基于 LazyRow）
    var draggingTagId by remember { mutableStateOf<String?>(null) }
    var dragDistanceX by remember { mutableFloatStateOf(0f) }
    var isHoveringDeleteZone by remember { mutableStateOf(false) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    // 本章节标签在内存中的实时可重排列表
    val currentTags = remember(attachedTags) {
        mutableStateListOf<TagEntity>().apply { addAll(attachedTags) }
    }
    LaunchedEffect(attachedTags) {
        if (draggingTagId == null) {
            currentTags.clear()
            currentTags.addAll(attachedTags)
        }
    }

    val tagsListState = rememberLazyListState()

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    val scrollState = rememberScrollState(initial = scrollOffset)
    LaunchedEffect(scrollState.value) {
        onSaveScroll(0, scrollState.value)
    }

    val topBarScrollState = rememberLunyuTopBarScrollState()
    val isScrolledUnder by remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    var previousChapterId by remember { mutableStateOf(chapter.id) }
    // 切换章节时重置滚动位置并完全展开顶栏
    LaunchedEffect(chapter.id) {
        highlightJob?.cancel()
        highlightJob = null
        scrollJob?.cancel()
        scrollJob = null
        highlightedAnnotationIndex = null
        highlightProgress.snapTo(0f)
        if (chapter.id != previousChapterId) {
            previousChapterId = chapter.id
            topBarScrollState.expand()
            scrollState.scrollTo(0)
        }
    }

    // 列表滚动回最顶部时，保证顶栏完全展开
    LaunchedEffect(isScrolledUnder) {
        if (!isScrolledUnder && !topBarScrollState.isExpanded) {
            topBarScrollState.expand()
        }
    }

    val containerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val originalTextCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textLayoutResult by remember(chapter.id) { mutableStateOf<TextLayoutResult?>(null) }
    val annotationCoordinates = remember(chapter.id) { mutableMapOf<Int, LayoutCoordinates>() }
    val annotationCharOffsets = remember(chapter.id) { mutableMapOf<Int, Int>() }

    Box(modifier = modifier.fillMaxSize()) {
        LunyuCollapsibleTopBarLayout(
            scrollState = topBarScrollState,
            modifier = Modifier.fillMaxSize(),
            topBar = {
            LunyuTopBar(
                showDivider = isScrolledUnder,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回本篇"
                        )
                    }
                },
                actions = {
                    // 复制原文按钮
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Chapter Text", chapter.plainText)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "已复制原文", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "复制原文"
                        )
                    }
                    // 收藏按钮
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏本章",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { containerCoordinates.value = it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 1. 原文部分（完全对齐 Flutter _ChapterReadingContentHeader）
                // 段首带有 primary 色的 displayId（例如 "4·1 "），行内注释使用圆形角标 ①, ②...
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                ) {
                    val rawOrPlain = if (chapter.text.isNotBlank()) chapter.text else chapter.plainText
                    val originalAnnotated = formatChapterOriginalText(
                        displayId = chapter.displayId,
                        rawText = rawOrPlain,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        onAnnotationClick = { noteNum ->
                            highlightJob?.cancel()
                            highlightJob = coroutineScope.launch {
                                highlightedAnnotationIndex = noteNum
                                highlightProgress.snapTo(1f)

                                scrollJob?.cancel()
                                scrollJob = coroutineScope.launch {
                                    val containerCoords = containerCoordinates.value
                                    val itemCoords = annotationCoordinates[noteNum]
                                    val viewportHeight = containerCoords?.size?.height ?: 0

                                    if (containerCoords != null && itemCoords != null && itemCoords.isAttached && containerCoords.isAttached && viewportHeight > 0) {
                                        val itemPosInContainer = containerCoords.localPositionOf(itemCoords, Offset.Zero)
                                        val itemCenterYInContainer = itemPosInContainer.y + itemCoords.size.height / 2f
                                        val delta = itemCenterYInContainer - (viewportHeight / 2f)
                                        val targetScrollY = (scrollState.value + delta).roundToInt().coerceIn(0, scrollState.maxValue)
                                        scrollState.animateScrollTo(
                                            targetScrollY,
                                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }

                                delay(500)
                                highlightProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                                )
                                highlightedAnnotationIndex = null
                            }
                        },
                        onAnnotationOffsetRecorded = { noteNum, charOffset ->
                            annotationCharOffsets[noteNum] = charOffset
                        }
                    )
                    Text(
                        text = originalAnnotated,
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier.onGloballyPositioned { originalTextCoordinates.value = it },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 标签 Chips 行（基于 LazyRow 实现高性能、零布局反馈循环的平滑重排）
                if (currentTags.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        AddTagChip(
                            label = "添加一个标签",
                            onClick = { showAddTagDialog = true }
                        )
                    }
                } else {
                    val spacingPx = with(density) { 8.dp.toPx() }
                    val hysteresisPx = with(density) { 8.dp.toPx() }
                    val edgeThresholdPx = with(density) { 48.dp.toPx() }

                    // 检查并执行相邻槽位交换的统一函数
                    val checkAndPerformSwap: () -> Unit = {
                        val key = draggingTagId
                        if (key != null) {
                            val layoutInfo = tagsListState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val currentItemInfo = visibleItems.firstOrNull { it.key == key }
                            if (currentItemInfo != null) {
                                val currentCenter = currentItemInfo.offset + currentItemInfo.size / 2f + dragDistanceX

                                // 1. 检查是否悬停在末尾固定操作槽上
                                val trailingItemInfo = visibleItems.firstOrNull { it.key == "trailing_action" }
                                val inDelete = if (trailingItemInfo != null) {
                                    currentCenter >= (trailingItemInfo.offset - spacingPx)
                                } else false

                                if (inDelete != isHoveringDeleteZone) {
                                    isHoveringDeleteZone = inDelete
                                    if (inDelete) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }

                                // 2. 若未悬停在删除槽上，检测相邻项交换
                                if (!inDelete) {
                                    val curIdx = currentTags.indexOfFirst { it.id == key }
                                    if (curIdx != -1) {
                                        // 向左拖拽（与前一项交换）
                                        if (curIdx > 0) {
                                            val prevTag = currentTags[curIdx - 1]
                                            val prevItemInfo = visibleItems.firstOrNull { it.key == prevTag.id }
                                            if (prevItemInfo != null) {
                                                val prevCenter = prevItemInfo.offset + prevItemInfo.size / 2f
                                                if (currentCenter < prevCenter - hysteresisPx) {
                                                    val shiftDistance = prevItemInfo.size + spacingPx
                                                    dragDistanceX += shiftDistance
                                                    val item = currentTags.removeAt(curIdx)
                                                    currentTags.add(curIdx - 1, item)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                        // 向右拖拽（与后一项交换）
                                        else if (curIdx < currentTags.size - 1) {
                                            val nextTag = currentTags[curIdx + 1]
                                            val nextItemInfo = visibleItems.firstOrNull { it.key == nextTag.id }
                                            if (nextItemInfo != null) {
                                                val nextCenter = nextItemInfo.offset + nextItemInfo.size / 2f
                                                if (currentCenter > nextCenter + hysteresisPx) {
                                                    val shiftDistance = nextItemInfo.size + spacingPx
                                                    dragDistanceX -= shiftDistance
                                                    val item = currentTags.removeAt(curIdx)
                                                    currentTags.add(curIdx + 1, item)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 统一自动滚动触发函数（单协程逐帧循环）
                    val updateAutoScroll: () -> Unit = {
                        val key = draggingTagId
                        if (key != null) {
                            val layoutInfo = tagsListState.layoutInfo
                            val viewportWidth = layoutInfo.viewportSize.width.toFloat()
                            val currentItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
                            if (currentItemInfo != null && viewportWidth > 0f) {
                                val currentCenter = currentItemInfo.offset + currentItemInfo.size / 2f + dragDistanceX
                                val needScrollRight = currentCenter > viewportWidth - edgeThresholdPx && tagsListState.canScrollForward
                                val needScrollLeft = currentCenter < edgeThresholdPx && tagsListState.canScrollBackward

                                if (needScrollRight || needScrollLeft) {
                                    if (autoScrollJob == null || autoScrollJob?.isActive == false) {
                                        autoScrollJob = coroutineScope.launch {
                                            while (isActive) {
                                                val lInfo = tagsListState.layoutInfo
                                                val vWidth = lInfo.viewportSize.width.toFloat()
                                                val cInfo = lInfo.visibleItemsInfo.firstOrNull { it.key == draggingTagId }
                                                if (cInfo == null || vWidth <= 0f) break

                                                val center = cInfo.offset + cInfo.size / 2f + dragDistanceX
                                                val speed = if (center > vWidth - edgeThresholdPx && tagsListState.canScrollForward) {
                                                    val ratio = ((center - (vWidth - edgeThresholdPx)) / edgeThresholdPx).coerceIn(0f, 1f)
                                                    (ratio * with(density) { 14.dp.toPx() }).coerceAtLeast(4f)
                                                } else if (center < edgeThresholdPx && tagsListState.canScrollBackward) {
                                                    val ratio = ((edgeThresholdPx - center) / edgeThresholdPx).coerceIn(0f, 1f)
                                                    -(ratio * with(density) { 14.dp.toPx() }).coerceAtLeast(4f)
                                                } else {
                                                    break
                                                }

                                                val consumed = tagsListState.scrollBy(speed)
                                                if (consumed == 0f) break
                                                dragDistanceX += consumed
                                                checkAndPerformSwap()
                                                withFrameMillis { }
                                            }
                                        }
                                    }
                                } else {
                                    autoScrollJob?.cancel()
                                    autoScrollJob = null
                                }
                            }
                        }
                    }

                    LazyRow(
                        state = tagsListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(
                            items = currentTags,
                            key = { it.id }
                        ) { tag ->
                            val isDragging = tag.id == draggingTagId

                            Box(
                                modifier = Modifier
                                    .then(
                                        if (isDragging) {
                                            Modifier
                                                .zIndex(10f)
                                                .graphicsLayer {
                                                    translationX = dragDistanceX
                                                    scaleX = 1.05f
                                                    scaleY = 1.05f
                                                    shadowElevation = 6.dp.toPx()
                                                    alpha = if (isHoveringDeleteZone) 0.5f else 1.0f
                                                }
                                        } else {
                                            Modifier
                                                .zIndex(1f)
                                                .animateItem(
                                                    fadeInSpec = null,
                                                    fadeOutSpec = null,
                                                    placementSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                        }
                                    )
                                    .pointerInput(tag.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { _ ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                draggingTagId = tag.id
                                                dragDistanceX = 0f
                                                isHoveringDeleteZone = false
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDistanceX += dragAmount.x
                                                checkAndPerformSwap()
                                                updateAutoScroll()
                                            },
                                            onDragEnd = {
                                                autoScrollJob?.cancel()
                                                autoScrollJob = null
                                                val draggedId = draggingTagId
                                                if (draggedId != null) {
                                                    if (isHoveringDeleteZone) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onToggleTag(draggedId)
                                                    } else {
                                                        val newIds = currentTags.map { it.id }
                                                        val oldIds = attachedTags.map { it.id }
                                                        if (newIds != oldIds) {
                                                            onReorderTags(newIds)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }
                                                }
                                                draggingTagId = null
                                                dragDistanceX = 0f
                                                isHoveringDeleteZone = false
                                            },
                                            onDragCancel = {
                                                autoScrollJob?.cancel()
                                                autoScrollJob = null
                                                draggingTagId = null
                                                dragDistanceX = 0f
                                                isHoveringDeleteZone = false
                                                currentTags.clear()
                                                currentTags.addAll(attachedTags)
                                            }
                                        )
                                    }
                            ) {
                                TagChip(
                                    name = tag.name,
                                    colorHex = tag.colorHex,
                                    icon = tag.icon,
                                    onClick = {
                                        if (draggingTagId == null) {
                                            onNavigateToTag(tag.id)
                                        }
                                    }
                                )
                            }
                        }

                        // 末尾固定尺寸操作项（添加 / 移除）
                        item(key = "trailing_action") {
                            Box(
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                            ) {
                                if (draggingTagId != null) {
                                    DeleteDropTagChip(
                                        isHovering = isHoveringDeleteZone
                                    )
                                } else {
                                    AddTagChip(
                                        label = "添加",
                                        onClick = { showAddTagDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 2. 翻译板块
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "翻译",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = chapter.translation.ifBlank { "本版本暂未提供翻译。" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }

                // 3. 注释板块
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "注释",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (chapter.annotations.isEmpty()) {
                        Text(
                            text = "本章暂无注释。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                if (chapter.annotations.isNotEmpty()) {
                    chapter.annotations.forEachIndexed { idx, note ->
                        val isCurrentTarget = highlightedAnnotationIndex == note.index
                        val progress = if (isCurrentTarget) highlightProgress.value else 0f
                        val badgeBgColor = lerp(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.primary,
                            progress
                        )
                        val badgeTextColor = lerp(
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            MaterialTheme.colorScheme.onPrimary,
                            progress
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    annotationCoordinates[note.index] = coords
                                }
                                .padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                    bottom = if (idx == chapter.annotations.size - 1) 48.dp else 20.dp
                                ),
                            verticalAlignment = Alignment.Top
                        ) {
                            // 圆形序号徽标：点击返回原文，无需加高亮，水波纹严格呈圆形
                            Surface(
                                onClick = {
                                    highlightJob?.cancel()
                                    highlightJob = null
                                    highlightedAnnotationIndex = null
                                    scrollJob?.cancel()
                                    scrollJob = coroutineScope.launch {
                                        highlightProgress.snapTo(0f)

                                        val charOffset = annotationCharOffsets[note.index]
                                        val layout = textLayoutResult
                                        val textCoords = originalTextCoordinates.value
                                        val containerCoords = containerCoordinates.value
                                        val viewportHeight = containerCoords?.size?.height ?: 0

                                        if (charOffset != null && layout != null && textCoords != null && containerCoords != null && textCoords.isAttached && containerCoords.isAttached && viewportHeight > 0) {
                                            val charBounds = layout.getBoundingBox(charOffset)
                                            val charCenterInText = Offset(
                                                (charBounds.left + charBounds.right) / 2f,
                                                (charBounds.top + charBounds.bottom) / 2f
                                            )
                                            val charPosInContainer = containerCoords.localPositionOf(textCoords, charCenterInText)
                                            val delta = charPosInContainer.y - (viewportHeight / 2f)
                                            val targetScrollY = (scrollState.value + delta).roundToInt().coerceIn(0, scrollState.maxValue)
                                            scrollState.animateScrollTo(
                                                targetScrollY,
                                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            )
                                        } else {
                                            scrollState.animateScrollTo(0)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = badgeBgColor,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${note.index}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            val noteAnnotated = buildAnnotatedString {
                                if (note.label.isNotBlank()) {
                                    withStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        append("${note.label}　")
                                    }
                                }
                                append(note.text)
                            }

                            Text(
                                text = noteAnnotated,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. 关联题目板块
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "关联题目",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (chapter.relatedQuestions.isEmpty()) {
                        Text(
                            text = "暂未发现与本章直接关联的题目。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        chapter.relatedQuestions.forEachIndexed { qIdx, q ->
                            Card(
                                onClick = { onNavigateToExercise(q.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (qIdx == chapter.relatedQuestions.size - 1) 0.dp else 8.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Quiz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = q.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }

                // 5. 胶囊切章导航（完全对齐 Flutter _AndroidChapterSequenceNavigation）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prevChapter != null) {
                        val (pPian, pChapter) = prevChapter
                        OutlinedButton(
                            onClick = { onNavigateToChapter(pPian.slug, pChapter.number) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pPian.shortTitle} ${pChapter.displayId}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (nextChapter != null) {
                        val (nPian, nChapter) = nextChapter
                        OutlinedButton(
                            onClick = { onNavigateToChapter(nPian.slug, nChapter.number) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${nPian.shortTitle} ${nChapter.displayId}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (showAddTagDialog) {
            AddTagDialog(
                allExistingTags = allTags.map { it.tag },
                attachedTagIds = attachedTags.map { it.id }.toSet(),
                onSelectExistingTag = { existingTag ->
                    onToggleTag(existingTag.id)
                },
                onCreateNewTag = { name, color, icon ->
                    onCreateTag(name, color, icon)
                },
                onDismissRequest = { showAddTagDialog = false }
            )
        }
    }
}
}
