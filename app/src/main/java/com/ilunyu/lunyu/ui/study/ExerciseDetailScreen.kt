package com.ilunyu.lunyu.ui.study

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.ExerciseBlock
import com.ilunyu.lunyu.data.model.ExerciseFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    isFavorite: Boolean,
    scrollIndex: Int = 0,
    scrollOffset: Int = 0,
    onSaveScroll: (Int, Int) -> Unit = { _, _ -> },
    onToggleFavorite: () -> Unit,
    defaultAnswerExpanded: Boolean = true,
    onOpenChapterSourceId: ((Int) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollIndex,
        initialFirstVisibleItemScrollOffset = scrollOffset
    )
    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        onSaveScroll(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
    }

    val scrollState = rememberLunyuTopBarScrollState()
    val isScrolledUnder by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    var isAnswerExpanded by remember(exercise.id, defaultAnswerExpanded) {
        mutableStateOf(defaultAnswerExpanded)
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isAnswerExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "answerArrowRotation"
    )

    var previousExerciseId by remember { mutableStateOf(exercise.id) }
    // 切换试题时重置滚动位置并完全展开顶栏
    LaunchedEffect(exercise.id) {
        if (exercise.id != previousExerciseId) {
            previousExerciseId = exercise.id
            scrollState.expand()
            lazyListState.scrollToItem(0)
        }
    }

    // 列表滚动回最顶部时，保证顶栏完全展开
    LaunchedEffect(isScrolledUnder) {
        if (!isScrolledUnder && !scrollState.isExpanded) {
            scrollState.expand()
        }
    }

    LunyuCollapsibleTopBarLayout(
        scrollState = scrollState,
        modifier = modifier.fillMaxSize(),
        topBar = {
            LunyuTopBar(
                showDivider = isScrolledUnder,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回试题库"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏试题",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 标题与元数据（对齐 Flutter _ExerciseHeader）
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (exercise.monthLabel.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = exercise.monthLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "第${exercise.number}题·满分${exercise.score}分",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // 2. 题目各 Block（材料卡片 _MaterialCard、题干等）
            itemsIndexed(exercise.question) { idx, block ->
                val isLast = idx == exercise.question.size - 1
                val nextBlock = if (!isLast) exercise.question[idx + 1] else null
                val bottomPadding = when {
                    isLast -> 0.dp
                    block.isMaterial && nextBlock?.isMaterial == true -> 8.dp
                    else -> 16.dp
                }
                ExerciseBlockItem(
                    block = block,
                    onOpenChapterSourceId = onOpenChapterSourceId,
                    topPadding = if (idx == 0) 4.dp else 0.dp,
                    bottomPadding = bottomPadding
                )
            }

            // 原文与答案之间留出 48dp 留白（36dp Spacer + 12dp 行内上边距 = 48dp）
            item {
                Spacer(modifier = Modifier.height(36.dp))
            }

            // 3. 答案与解析标题（整行全宽矩形水波纹可点按；单独点按右侧图标触发局部圆形水波纹）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAnswerExpanded = !isAnswerExpanded }
                        .padding(start = 24.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "答案与解析",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isAnswerExpanded = !isAnswerExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAnswerExpanded) "收起答案与解析" else "展开答案与解析",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }

            // 4. 答案与解析具体内容（滑入/滑出展开收起动画）
            item {
                AnimatedVisibility(
                    visible = isAnswerExpanded,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    ) + slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        exercise.answer.forEachIndexed { idx, block ->
                            val isLast = idx == exercise.answer.size - 1
                            val nextBlock = if (!isLast) exercise.answer[idx + 1] else null
                            val bottomPadding = when {
                                isLast -> 0.dp
                                block.isMaterial && nextBlock?.isMaterial == true -> 8.dp
                                else -> 16.dp
                            }
                            ExerciseBlockItem(
                                block = block,
                                onOpenChapterSourceId = onOpenChapterSourceId,
                                topPadding = if (idx == 0) 4.dp else 0.dp,
                                bottomPadding = bottomPadding
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ExerciseBlockItem(
    block: ExerciseBlock,
    onOpenChapterSourceId: ((Int) -> Unit)?,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 16.dp
) {
    if (block.isMaterial) {
        // 材料卡片（完全对齐 Flutter _MaterialCard）
        val tappable = block.sourceid != null && block.sourceid > 0 && onOpenChapterSourceId != null
        val cardShape = RoundedCornerShape(16.dp)
        val cardColors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        val cardModifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = bottomPadding)

        val cardContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (block.blocktitle.isNotBlank()) {
                    Text(
                        text = block.blocktitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (block.paragraphs.isNotEmpty()) {
                    block.paragraphs.forEachIndexed { idx, p ->
                        if (idx > 0) Spacer(modifier = Modifier.height(8.dp))
                        ExerciseFormattedText(
                            text = p.text,
                            format = p.format ?: block.format,
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (block.text.isNotBlank()) {
                    ExerciseFormattedText(
                        text = block.text,
                        format = block.format,
                        fontSize = 16.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (block.sourcename.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = block.sourcename,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (tappable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (tappable) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "查看原章",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (tappable) {
            Card(
                onClick = { onOpenChapterSourceId?.invoke(block.sourceid!!) },
                modifier = cardModifier.clip(cardShape),
                shape = cardShape,
                colors = cardColors,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                cardContent()
            }
        } else {
            Card(
                modifier = cardModifier,
                shape = cardShape,
                colors = cardColors,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                cardContent()
            }
        }
    } else {
        val isNote = block.isNote
        val contentFontSize = if (isNote) 14.sp else 16.sp
        val contentLineHeight = if (isNote) 24.sp else 28.sp
        val contentColor = if (isNote) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        // 普通题干、小问说明或答案解析/评分标准
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = bottomPadding)
        ) {
            if (block.blocktitle.isNotBlank()) {
                Text(
                    text = block.blocktitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = contentFontSize,
                        fontWeight = if (isNote) FontWeight.Normal else FontWeight.SemiBold,
                        color = contentColor
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (block.paragraphs.isNotEmpty()) {
                block.paragraphs.forEachIndexed { idx, p ->
                    if (idx > 0) Spacer(modifier = Modifier.height(6.dp))
                    ExerciseFormattedText(
                        text = p.text,
                        format = p.format ?: block.format,
                        fontSize = contentFontSize,
                        lineHeight = contentLineHeight,
                        color = contentColor
                    )
                }
            } else if (block.text.isNotBlank()) {
                ExerciseFormattedText(
                    text = block.text,
                    format = block.format,
                    fontSize = contentFontSize,
                    lineHeight = contentLineHeight,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * 试题富文本渲染组件：
 * 支持首行缩进（firstLine）、悬挂缩进（hanging）、段落整段缩进（paragraph）、
 * 下划线（underline）及着重点（emphasis）。
 */
@Composable
private fun ExerciseFormattedText(
    text: String,
    format: ExerciseFormat?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 28.sp,
    fontWeight: FontWeight? = null
) {
    if (text.isBlank()) return

    val indent = format?.indent
    val indentLevel = indent?.level ?: 0
    val indentKind = indent?.kind.orEmpty()

    val textIndent = when {
        indentLevel > 0 && indentKind == "firstLine" -> {
            TextIndent(firstLine = (fontSize.value * indentLevel).sp, restLine = 0.sp)
        }
        indentLevel > 0 && indentKind == "hanging" -> {
            TextIndent(firstLine = 0.sp, restLine = (fontSize.value * indentLevel).sp)
        }
        else -> null
    }

    val paragraphStartPadding = if (indentLevel > 0 && indentKind == "paragraph") {
        (fontSize.value * indentLevel).dp
    } else {
        0.dp
    }

    val underlineRanges = format?.marks?.underline.orEmpty()
    val emphasisRanges = format?.marks?.emphasis.orEmpty()

    val annotatedText = remember(text, underlineRanges) {
        if (underlineRanges.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                underlineRanges.forEach { range ->
                    val start = range.start.coerceIn(0, text.length)
                    val end = range.end.coerceIn(start, text.length)
                    if (start < end) {
                        addStyle(
                            style = SpanStyle(textDecoration = TextDecoration.Underline),
                            start = start,
                            end = end
                        )
                    }
                }
            }
        }
    }

    var textLayoutResult by remember(text, format) { mutableStateOf<TextLayoutResult?>(null) }

    val dotModifier = if (emphasisRanges.isNotEmpty()) {
        Modifier
            .padding(bottom = 3.dp)
            .drawBehind {
                val layout = textLayoutResult ?: return@drawBehind
                val textLength = layout.layoutInput.text.length
                val dotRadius = if (fontSize <= 14.sp) 1.4.dp.toPx() else 1.6.dp.toPx()
                val dotYOffset = if (fontSize <= 14.sp) 4.0.dp.toPx() else 4.8.dp.toPx()
                emphasisRanges.forEach { range ->
                    val start = range.start.coerceIn(0, textLength)
                    val end = range.end.coerceIn(start, textLength)
                    for (i in start until end) {
                        if (i < textLength && layout.layoutInput.text[i].isWhitespace()) continue
                        val line = layout.getLineForOffset(i)
                        val baseline = layout.getLineBaseline(line)
                        val box = layout.getBoundingBox(i)
                        val centerX = (box.left + box.right) / 2f
                        val centerY = baseline + dotYOffset
                        drawCircle(
                            color = color,
                            radius = dotRadius,
                            center = Offset(centerX, centerY)
                        )
                    }
                }
            }
    } else {
        Modifier
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = color,
            fontWeight = fontWeight ?: FontWeight.Normal,
            textIndent = textIndent
        ),
        onTextLayout = { textLayoutResult = it },
        modifier = modifier
            .then(if (paragraphStartPadding > 0.dp) Modifier.padding(start = paragraphStartPadding) else Modifier)
            .then(dotModifier)
    )
}
