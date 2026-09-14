package com.ilunyu.lunyu.ui.reading

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Pian

import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles

/**
 * 篇目总览卡片：双列网格中的单个篇目卡片。
 * 背景为 surfaceContainerLow，圆角 16dp，高度 80dp。
 * 点击水波纹严格限制在 16dp 圆角范围内。
 */
@Composable
fun PianCatalogCard(
    pian: Pian,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightTerms: List<String> = emptyList()
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            val titleText = if (highlightTerms.isEmpty()) {
                AnnotatedString(pian.title)
            } else {
                highlightAnnotatedString(pian.title, highlightTerms, MaterialTheme.colorScheme.tertiary)
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val count = if (pian.chapterCount > 0) pian.chapterCount else pian.chapters.size
            Text(
                text = "共 $count 章",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * 篇目总览双列网格：完全对齐 Flutter PianCatalogGrid。
 */
@Composable
fun PianCatalogGrid(
    pians: List<Pian>,
    onSelectPian: (Pian) -> Unit,
    modifier: Modifier = Modifier,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(pians.size) { index ->
            val pian = pians[index]
            PianCatalogCard(
                pian = pian,
                onClick = { onSelectPian(pian) }
            )
        }
    }
}

/**
 * 格式化章节原文：
 * 1. 在段首插入大号章节号（例如 "4·1 "），颜色为 primary。
 * 2. 将文本中类似 "[1]", "[2]" 的注释标记替换为带圈数字 ①, ②, ③...，颜色为 primary。
 * 3. 支持点击带圈数字跳转到注释。
 */
fun formatChapterOriginalText(
    displayId: String,
    rawText: String,
    primaryColor: androidx.compose.ui.graphics.Color,
    onAnnotationClick: ((Int) -> Unit)? = null
): AnnotatedString {
    return buildAnnotatedString {
        // 章节编号
        pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium))
        append("$displayId ")
        pop()

        val regex = Regex("""\[(\d+)]""")
        var cursor = 0
        for (match in regex.findAll(rawText)) {
            if (match.range.first > cursor) {
                append(rawText.substring(cursor, match.range.first))
            }
            val num = match.groupValues[1].toIntOrNull() ?: 0
            val circled = if (num in 1..20) {
                "${(0x245F + num).toChar()}"
            } else if (num in 21..35) {
                "${(0x3250 + num - 20).toChar()}"
            } else {
                "[$num]"
            }
            if (onAnnotationClick != null) {
                val link = LinkAnnotation.Clickable(
                    tag = "annotation_$num",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)
                    ),
                    linkInteractionListener = {
                        onAnnotationClick(num)
                    }
                )
                pushLink(link)
                append(circled)
                pop()
            } else {
                pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Normal))
                append(circled)
                pop()
            }
            cursor = match.range.last + 1
        }
        if (cursor < rawText.length) {
            append(rawText.substring(cursor))
        }
    }
}


/**
 * 章节列表行：左侧独立一列展示朱红色 (tertiary) 编号，
 * 右侧为正文与译文，右侧为收藏书签，底部为全宽分割线。
 */
@Composable
fun PianChapterRow(
    chapter: Chapter,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightTerms: List<String> = emptyList(),
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 24.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧独立的章节编号（如 1·1），朱红色 12sp，右对齐
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = chapter.displayId,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 正文与译文列
            Column(modifier = Modifier.weight(1f)) {
                val plainTextAnnotated = if (highlightTerms.isEmpty()) {
                    AnnotatedString(chapter.plainText)
                } else {
                    highlightAnnotatedString(chapter.plainText, highlightTerms, MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    text = plainTextAnnotated,
                    style = MaterialTheme.typography.titleMedium.copy(
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (chapter.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val transAnnotated = if (highlightTerms.isEmpty()) {
                        AnnotatedString(chapter.translation)
                    } else {
                        highlightAnnotatedString(chapter.translation, highlightTerms, MaterialTheme.colorScheme.tertiary)
                    }
                    Text(
                        text = transAnnotated,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 收藏书签按钮 (40x40 触摸区，20dp 图标)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏本章",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * 篇末胶囊状上一篇/下一篇切换按钮
 */
@Composable
fun PianSequenceNavigation(
    prevPian: Pian?,
    nextPian: Pian?,
    onNavigateToPian: (Pian) -> Unit,
    modifier: Modifier = Modifier
) {
    if (prevPian == null && nextPian == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prevPian != null) {
            OutlinedButton(
                onClick = { onNavigateToPian(prevPian) },
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
                    text = prevPian.title,
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

        Spacer(modifier = Modifier.width(12.dp))

        if (nextPian != null) {
            OutlinedButton(
                onClick = { onNavigateToPian(nextPian) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = nextPian.title,
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

/**
 * 辅助函数：将搜索匹配项用高亮颜色渲染
 */
fun highlightAnnotatedString(
    text: String,
    terms: List<String>,
    highlightColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    if (terms.isEmpty() || text.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            var nearestIndex = -1
            var matchedTerm = ""
            for (term in terms) {
                if (term.isEmpty()) continue
                val idx = text.indexOf(term, cursor, ignoreCase = true)
                if (idx != -1 && (nearestIndex == -1 || idx < nearestIndex)) {
                    nearestIndex = idx
                    matchedTerm = term
                }
            }
            if (nearestIndex == -1) {
                append(text.substring(cursor))
                break
            }
            if (nearestIndex > cursor) {
                append(text.substring(cursor, nearestIndex))
            }
            val matchEnd = nearestIndex + matchedTerm.length
            val matchedSubstring = text.substring(nearestIndex, matchEnd)
            pushStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold))
            append(matchedSubstring)
            pop()
            cursor = matchEnd
        }
    }
}
