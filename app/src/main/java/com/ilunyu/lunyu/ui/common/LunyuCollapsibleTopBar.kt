package com.ilunyu.lunyu.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 论语原生顶栏滑动状态控制器 (对齐 Material Design 3 / Files by Google AppBarLayout 物理联动规范)
 *
 * 核心特性：
 * 1. 物理 1:1 跟手位移：手指移动多少像素，TopBar、TabBar 和内容区整体在屏幕上绝对同步移动多少像素；
 * 2. 抬手/松手平滑吸附：根据抬手初速度方向或位移是否过半，平滑吸附至完全展开 (0) 或完全收起 (-barHeightPx)；
 * 3. 触摸打断机制：用户手指再次接触屏幕时立即取消吸附动画，手指无缝接管 1:1 控制；
 * 4. 惯性滚动穿透：列表自身的 Fling 不会与顶栏的 Settle 互相冲突打架，列表滑到顶部时若有剩余速度可将顶栏自然顶开展开。
 */
@Stable
class LunyuTopBarScrollState(
    initialBarHeightPx: Float = 0f,
    private val coroutineScope: CoroutineScope
) {
    var barHeightPx by mutableFloatStateOf(initialBarHeightPx)
        private set

    /**
     * 顶栏垂直平移量，范围 [-barHeightPx, 0f]
     * 0f 为完全展开，-barHeightPx 为完全收起
     */
    var offset by mutableFloatStateOf(0f)
        private set

    var isAnimating by mutableStateOf(false)
        private set

    private val animatableOffset = Animatable(0f)
    private var settleJob: Job? = null

    val collapsedFraction: Float
        get() = if (barHeightPx > 0f) (-offset / barHeightPx).coerceIn(0f, 1f) else 0f

    val isCollapsed: Boolean
        get() = offset <= -barHeightPx + 0.5f

    val isExpanded: Boolean
        get() = offset >= -0.5f

    fun updateBarHeightPx(newHeight: Float) {
        if (newHeight > 0f && barHeightPx != newHeight) {
            barHeightPx = newHeight
            offset = offset.coerceIn(-newHeight, 0f)
        }
    }

    fun setOffsetDirectly(newOffset: Float) {
        stopAnimation()
        offset = if (barHeightPx > 0f) newOffset.coerceIn(-barHeightPx, 0f) else 0f
    }

    fun stopAnimation() {
        if (isAnimating || settleJob?.isActive == true) {
            settleJob?.cancel()
            settleJob = null
            isAnimating = false
        }
    }

    fun animateTo(
        targetOffset: Float,
        spec: AnimationSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) {
        val targetClamped = if (barHeightPx > 0f) targetOffset.coerceIn(-barHeightPx, 0f) else 0f
        stopAnimation()
        isAnimating = true
        settleJob = coroutineScope.launch {
            try {
                animatableOffset.snapTo(offset)
                animatableOffset.animateTo(
                    targetValue = targetClamped,
                    animationSpec = spec
                ) {
                    offset = this.value
                }
            } finally {
                offset = animatableOffset.value
                isAnimating = false
            }
        }
    }

    fun expand() {
        animateTo(0f)
    }

    fun collapse() {
        animateTo(-barHeightPx)
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (barHeightPx <= 0f) return Offset.Zero

            // 仅在用户手指触碰滑动时执行 1:1 跟手拦截
            if (source == NestedScrollSource.UserInput) {
                if (isAnimating) {
                    stopAnimation()
                }

                val delta = available.y
                if (delta < 0f) {
                    // 手指向上滑：顶栏向上收起
                    if (offset > -barHeightPx) {
                        val prev = offset
                        val target = (prev + delta).coerceIn(-barHeightPx, 0f)
                        val consumed = target - prev
                        offset = target
                        return Offset(0f, consumed)
                    }
                } else if (delta > 0f) {
                    // 手指向下滑：顶栏向下拉出展开
                    if (offset < 0f) {
                        val prev = offset
                        val target = (prev + delta).coerceIn(-barHeightPx, 0f)
                        val consumed = target - prev
                        offset = target
                        return Offset(0f, consumed)
                    }
                }
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (barHeightPx <= 0f) return Offset.Zero

            // 当列表滑动到最顶部且有剩余向下的滑动位移时，展开顶栏
            if (available.y > 0f && offset < 0f) {
                val prev = offset
                val target = (prev + available.y).coerceIn(-barHeightPx, 0f)
                val consumedDelta = target - prev
                offset = target
                return Offset(0f, consumedDelta)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (barHeightPx <= 0f) return Velocity.Zero

            // 若顶栏已经处于极限状态（完全展开或完全收起），不消费初速度，全部留给列表惯性滑动
            if (offset <= -barHeightPx || offset >= 0f) {
                return Velocity.Zero
            }

            val velocityY = available.y
            val targetOffset = when {
                velocityY > 300f -> 0f              // 快速下拉：吸附完全展开
                velocityY < -300f -> -barHeightPx    // 快速上推：吸附完全收起
                else -> if (offset > -barHeightPx / 2f) 0f else -barHeightPx // 慢拖释放：露出过半则展开，否则收起
            }

            animateTo(targetOffset)
            return available // 顶栏吸附动画接管，消费初速度避免双重动力学叠加
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (barHeightPx <= 0f) return Velocity.Zero

            // 列表惯性滚动到最顶部后若仍有向下的剩余速度，触发顶栏平滑展开
            if (available.y > 100f && offset < 0f) {
                animateTo(0f)
                return available
            }
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberLunyuTopBarScrollState(barHeight: Dp = 56.dp): LunyuTopBarScrollState {
    val density = LocalDensity.current
    val barHeightPx = with(density) { barHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()
    return remember(barHeightPx) {
        LunyuTopBarScrollState(barHeightPx, coroutineScope)
    }
}

/**
 * 论语原生统一顶栏
 *
 * 规范：
 * - 高度标准 56dp，不挤压压缩；
 * - 底部 1px 分割线，当 showDivider 为 true 时呈现 outlineVariant，为 false 时透明；
 * - 页面不在顶部时显示分割线，对齐 Flutter 与 Material Design 3 规范。
 */
@Composable
fun LunyuTopBar(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    showDivider: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (title != null) {
                Spacer(modifier = Modifier.width(20.dp))
            }

            if (title != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (navigationIcon != null) 8.dp else 0.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    title()
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = if (showDivider) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                Color.Transparent
            }
        )
    }
}

/**
 * 论语可折叠 Tab 页面脚手架布局（对齐 Files by Google 的 CoordinatorLayout + AppBarLayout 联动机制）
 *
 * 布局物理架构：
 * - 顶栏 (TopBar 56dp) + Tab 栏 (TabBar 48dp) 组成刚体联动整体 Header；
 * - 下方主内容 (Content) 顶边缘物理贴紧 TabBar 底部；
 * - 向上滑动时，TopBar 向上平移移出屏幕，TabBar 吸顶固定在状态栏下方，Content 同步平移上移，三者位移 1:1 绝对一致；
 * - 向下滑动时，TopBar 从顶部落下，TabBar 同步下移，Content 同步下移，三者位移 1:1 绝对一致；
 * - 采用 placeWithLayer 进行纯 GPU 硬件图层平移，滚动过程中绝不触发全局重新测量 (Re-measure)，彻底消除重排抖动。
 */
@Composable
fun LunyuCollapsibleTabLayout(
    scrollState: LunyuTopBarScrollState,
    topBar: @Composable () -> Unit,
    tabBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = {
            // Child 0: TopBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                topBar()
            }
            // Child 1: TabBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                tabBar()
            }
            // Child 2: Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                content()
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .nestedScroll(scrollState.nestedScrollConnection)
            .clipToBounds()
    ) { measurables, constraints ->
        val topBarPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
        val tabBarPlaceable = measurables[1].measure(constraints.copy(minHeight = 0))
        val topBarHeight = topBarPlaceable.height
        val tabBarHeight = tabBarPlaceable.height

        scrollState.updateBarHeightPx(topBarHeight.toFloat())

        val contentHeight = (constraints.maxHeight - tabBarHeight).coerceAtLeast(0)
        val contentPlaceable = measurables[2].measure(
            constraints.copy(minHeight = contentHeight, maxHeight = contentHeight)
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            val currentOffset = scrollState.offset.roundToInt() // [-topBarHeight, 0]

            // Child 2: Content 先放置，使其在 Z 轴上处于底层，内容绝不上浮遮挡顶栏或 TabBar
            contentPlaceable.placeWithLayer(0, topBarHeight + tabBarHeight + currentOffset)

            // Child 0: TopBar 刚体位移（覆盖在内容上方）
            topBarPlaceable.placeWithLayer(0, currentOffset)

            // Child 1: TabBar 紧随 TopBar 底部吸顶（覆盖在内容上方）
            tabBarPlaceable.placeWithLayer(0, topBarHeight + currentOffset)
        }
    }
}

/**
 * 兼容旧接口的顶栏脚手架
 */
@Composable
fun LunyuCollapsibleTopBarLayout(
    scrollState: LunyuTopBarScrollState,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 56.dp,
    tabBar: (@Composable () -> Unit)? = null,
    applyStatusBarsPadding: Boolean = true,
    content: @Composable () -> Unit
) {
    if (tabBar != null) {
        LunyuCollapsibleTabLayout(
            scrollState = scrollState,
            topBar = topBar,
            tabBar = tabBar,
            modifier = modifier,
            content = content
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .then(if (applyStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
        ) {
            topBar()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }
    }
}
