package com.ilunyu.lunyu.ui.reading

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Pian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    pian: Pian,
    chapter: Chapter,
    prevChapter: Pair<Pian, Chapter>?,
    nextChapter: Pair<Pian, Chapter>?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onNavigateToChapter: (String, Int) -> Unit,
    onNavigateToExercise: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var highlightedAnnotationIndex by remember(chapter.id) { mutableStateOf<Int?>(null) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    val lazyListState = rememberLazyListState()
    val scrollState = rememberLunyuTopBarScrollState()
    val isScrolledUnder by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    // 切换章节时重置滚动位置并完全展开顶栏
    LaunchedEffect(chapter.id) {
        scrollState.expand()
        lazyListState.scrollToItem(0)
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
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 原文部分（完全对齐 Flutter _ChapterReadingContentHeader）
            // 段首带有 primary 色的 displayId（例如 "4·1 "），行内注释使用圆形角标 ①, ②...
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 48.dp)
                ) {
                    val rawOrPlain = if (chapter.text.isNotBlank()) chapter.text else chapter.plainText
                    val originalAnnotated = formatChapterOriginalText(
                        displayId = chapter.displayId,
                        rawText = rawOrPlain,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        onAnnotationClick = { noteNum ->
                            highlightedAnnotationIndex = noteNum
                            val noteIdx = chapter.annotations.indexOfFirst { it.index == noteNum }
                            if (noteIdx >= 0) {
                                coroutineScope.launch {
                                    delay(80)
                                    lazyListState.animateScrollToItem(3 + noteIdx)
                                }
                            }
                        }
                    )
                    Text(
                        text = originalAnnotated,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // 2. 翻译板块
            item {
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
            }

            // 3. 注释板块
            item {
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
            }

            if (chapter.annotations.isNotEmpty()) {
                itemsIndexed(chapter.annotations, key = { _, note -> "note_${note.index}" }) { idx, note ->
                    val isHighlighted = highlightedAnnotationIndex == note.index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                bottom = if (idx == chapter.annotations.size - 1) 48.dp else 20.dp
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 圆形序号徽标：点击返回原文，高亮时显式突出，水波纹严格呈圆形
                        Surface(
                            onClick = {
                                highlightedAnnotationIndex = note.index
                                coroutineScope.launch {
                                    delay(100)
                                    lazyListState.animateScrollToItem(0)
                                }
                            },
                            shape = CircleShape,
                            color = if (isHighlighted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${note.index}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHighlighted) {
                                             MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
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
            item {
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
            }

            // 5. 胶囊切章导航（完全对齐 Flutter _AndroidChapterSequenceNavigation）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
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

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
