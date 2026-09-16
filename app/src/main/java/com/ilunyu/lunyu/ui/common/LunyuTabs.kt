package com.ilunyu.lunyu.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 可滚动 Tab 栏：完全对齐 Flutter M3 TabBar（isScrollable = true, tabAlignment = start）
 * - 解除 Compose 默认 90dp 强制宽度，按文字内容自适应排版
 * - 首项文字距屏幕左边界精准对齐 24dp（8dp edgePadding + 16dp labelPadding）
 * - 指示条 1:1 实时跟随手指滑动手势位移（连续插值），绝不卡顿或延后
 * - 指示条宽度贴合文字标签（TabBarIndicatorSize.label），高度 3dp，带 3dp 顶部圆角
 */
@Composable
fun LunyuScrollableTabRow(
    pagerState: PagerState,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 8.dp,
    labelHorizontalPadding: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val tabLabelBounds = remember { mutableStateMapOf<Int, Rect>() }

    // 实时计算当前手势滑动进度下的指示条边界
    val indicatorRect by remember(pagerState) {
        derivedStateOf {
            val count = tabs.size
            if (count == 0 || tabLabelBounds.isEmpty()) {
                Rect.Zero
            } else {
                val currentPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, (count - 1).toFloat())
                val basePage = currentPosition.toInt()
                val nextPage = (basePage + 1).coerceAtMost(count - 1)
                val fraction = currentPosition - basePage

                val baseRect = tabLabelBounds[basePage]
                val nextRect = tabLabelBounds[nextPage]

                if (baseRect != null && nextRect != null) {
                    val left = baseRect.left + (nextRect.left - baseRect.left) * fraction
                    val right = baseRect.right + (nextRect.right - baseRect.right) * fraction
                    Rect(left = left, top = 0f, right = right, bottom = 0f)
                } else baseRect ?: nextRect ?: Rect.Zero
            }
        }
    }

    // 随着手指拖拽页面，自动平滑滚动 Tab 栏使得当前 Tab 始终处于可见/居中区域
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        if (!scrollState.isScrollInProgress && indicatorRect != Rect.Zero && scrollState.maxValue > 0) {
            val viewportWidth = scrollState.viewportSize
            val targetScroll = (indicatorRect.center.x - viewportWidth / 2f).toInt()
                .coerceIn(0, scrollState.maxValue)
            if (targetScroll != scrollState.value) {
                scrollState.scrollTo(targetScroll)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(containerColor)
    ) {
        // 底部全宽分割线 (1dp)
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = dividerColor
        )

        // 可横向滚动的 Tab 容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .horizontalScroll(scrollState)
                .onGloballyPositioned { containerCoordinates = it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = edgePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true)
                            ) {
                                onTabSelected(index)
                            }
                            .padding(horizontal = labelHorizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) selectedContentColor else unselectedContentColor
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.onGloballyPositioned { textCoords ->
                                val parent = containerCoordinates
                                if (parent != null && parent.isAttached && textCoords.isAttached) {
                                    val parentWindowPos = parent.positionInWindow()
                                    val textWindowPos = textCoords.positionInWindow()
                                    val relX = textWindowPos.x - parentWindowPos.x
                                    val relY = textWindowPos.y - parentWindowPos.y
                                    tabLabelBounds[index] = Rect(
                                        left = relX,
                                        top = relY,
                                        right = relX + textCoords.size.width,
                                        bottom = relY + textCoords.size.height
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 实时跟手滑动的高亮指示条（3dp 高度，带 3dp 顶部圆角，贴合 label 宽度）
            if (indicatorRect != Rect.Zero) {
                val indicatorHeightPx = with(density) { 3.dp.toPx() }
                val cornerRadiusPx = with(density) { 3.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val y = size.height - indicatorHeightPx
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = indicatorRect.left,
                                top = y,
                                right = indicatorRect.right,
                                bottom = size.height,
                                topLeftCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                topRightCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                bottomLeftCornerRadius = CornerRadius.Zero,
                                bottomRightCornerRadius = CornerRadius.Zero
                            )
                        )
                    }
                    drawPath(path = path, color = selectedContentColor)
                }
            }
        }
    }
}

/**
 * 等宽固定 Tab 栏：完全对齐 Flutter M3 TabBar（isScrollable = false, indicatorSize = TabBarIndicatorSize.tab）
 * - 用于收藏页（章节/试题）及搜索页（章节/试题）
 * - 支持 PagerState 实时手势跟手滑动，也支持离散 Tab 点击平滑补间动画
 * - 指示条宽度铺满 Tab，高度 3dp，带 3dp 顶部圆角
 */
@Composable
fun LunyuFixedTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val density = LocalDensity.current

    // 计算当前连续位置（0.0 ~ (tabs.size - 1)）
    val continuousPosition: Float = if (pagerState != null) {
        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
            .coerceIn(0f, (tabs.size - 1).toFloat())
    } else {
        val animatedPos by animateFloatAsState(
            targetValue = selectedTabIndex.toFloat(),
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "tabIndicatorPosition"
        )
        animatedPos
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // 底部全宽分割线 (1dp)
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = dividerColor
        )

        // 等宽 Tab 布局
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true)
                        ) {
                            onTabSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) selectedContentColor else unselectedContentColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 实时跟手滑动的高亮指示条（铺满整个 Tab 宽，高度 3dp，带 3dp 顶部圆角）
        if (tabs.isNotEmpty()) {
            val indicatorHeightPx = with(density) { 3.dp.toPx() }
            val cornerRadiusPx = with(density) { 3.dp.toPx() }

            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val tabWidth = size.width / tabs.size
                val left = continuousPosition * tabWidth
                val right = left + tabWidth
                val y = size.height - indicatorHeightPx

                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = y,
                            right = right,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            topRightCornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path = path, color = selectedContentColor)
            }
        }
    }
}
