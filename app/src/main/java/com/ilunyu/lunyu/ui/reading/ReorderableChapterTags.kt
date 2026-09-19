package com.ilunyu.lunyu.ui.reading

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.ui.tag.AddTagChip
import com.ilunyu.lunyu.ui.tag.TagChip
import kotlinx.coroutines.isActive

private class TagDrag(
    val tag: TagEntity,
    val tags: List<TagEntity>,
    val sizes: List<IntSize>,
    val starts: List<Float>,
    val from: Int,
    val initialPointerX: Float,
    val initialLeft: Float,
    val deleteLeft: Float,
) {
    var pointerX by mutableFloatStateOf(initialPointerX)
    var target by mutableIntStateOf(from)
    var deleting by mutableStateOf(false)
    val viewportLeft get() = initialLeft + pointerX - initialPointerX
}

/** Stable layout slots and a viewport-level drag preview keep pointer and layout independent. */
@Composable
internal fun ReorderableChapterTags(
    chapterId: String,
    tags: List<TagEntity>,
    onAdd: () -> Unit,
    onNavigate: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val padding = with(density) { 24.dp.toPx() }
    val gap = with(density) { 8.dp.toPx() }
    val hysteresis = with(density) { 8.dp.toPx() }
    val edge = with(density) { 48.dp.toPx() }
    val maxSpeed = with(density) { 420.dp.toPx() }
    val scroll = rememberScrollState()
    val sizes = remember(chapterId) { mutableMapOf<String, IntSize>() }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var localTags by remember(chapterId) { mutableStateOf(tags) }
    var drag by remember(chapterId) { mutableStateOf<TagDrag?>(null) }
    val latestTags by rememberUpdatedState(tags)
    val latestRemove by rememberUpdatedState(onRemove)
    val latestReorder by rememberUpdatedState(onReorder)
    // External updates must not replace the snapshot used by an active gesture.
    LaunchedEffect(tags) {
        if (drag == null) localTags = tags
    }
    LaunchedEffect(chapterId) { scroll.scrollTo(0) }

    fun updateTarget(session: TagDrag) {
        val center = session.viewportLeft + session.sizes[session.from].width / 2f + scroll.value
        val deleteThreshold = session.deleteLeft + if (session.deleting) -hysteresis else 0f
        val deleting = center >= deleteThreshold
        if (deleting != session.deleting) {
            session.deleting = deleting
            if (deleting) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (deleting) return
        var target = session.target
        // Thresholds are immutable for this gesture; neighbor animations never feed back here.
        while (target < session.tags.lastIndex &&
            center > session.starts[target + 1] + session.sizes[target + 1].width / 2f + hysteresis
        ) target++
        while (target > 0 &&
            center < session.starts[target - 1] + session.sizes[target - 1].width / 2f - hysteresis
        ) target--
        if (target != session.target) {
            session.target = target
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Exactly one frame loop per drag. Scroll speed is measured per second, not per frame.
    LaunchedEffect(drag) {
        val session = drag ?: return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
            previous = now
            val x = session.pointerX
            val ratio = when {
                x < edge -> -((edge - x) / edge).coerceIn(0f, 1f)
                x > viewport.width - edge -> ((x - viewport.width + edge) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (ratio != 0f) scroll.scrollBy(ratio * maxSpeed * seconds)
            updateTarget(session)
        }
    }

    Box(
        Modifier.fillMaxWidth()
            .onSizeChanged { viewport = it }
            .pointerInput(chapterId, density) {
                detectDragGesturesAfterLongPress(
                    onDragStart = start@{ position ->
                        val snapshot = localTags.toList()
                        val measured = snapshot.map { sizes[it.id] ?: IntSize.Zero }
                        if (measured.any { it.width <= 0 }) return@start
                        var cursor = padding
                        val starts = measured.map { size -> cursor.also { cursor += size.width + gap } }
                        val contentX = position.x + scroll.value
                        val index = starts.indices.firstOrNull {
                            contentX >= starts[it] && contentX < starts[it] + measured[it].width &&
                                position.y >= 0 && position.y <= measured[it].height
                        }
                        if (index != null) {
                            drag = TagDrag(snapshot[index], snapshot, measured, starts, index,
                                position.x, starts[index] - scroll.value, cursor)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, _ ->
                        drag?.let { session ->
                            change.consume()
                            session.pointerX = change.position.x
                            updateTarget(session)
                        }
                    },
                    onDragEnd = {
                        drag?.let { session ->
                            updateTarget(session)
                            val latest = latestTags
                            val existing = latest.associateBy { it.id }
                            val reordered = session.tags.toMutableList().apply {
                                add(session.target, removeAt(session.from))
                            }.mapNotNull { existing[it.id] }.toMutableList()
                            val known = reordered.map { it.id }.toSet()
                            reordered.addAll(latest.filter { it.id !in known })
                            if (session.deleting) {
                                // Explicitly remove only if the association still exists.
                                localTags = latest.filterNot { it.id == session.tag.id }
                                if (session.tag.id in existing) latestRemove(session.tag.id)
                            } else {
                                localTags = reordered
                                val ids = reordered.map { it.id }
                                if (ids != latest.map { it.id }) latestReorder(ids)
                            }
                        }
                        drag = null
                    },
                    onDragCancel = {
                        drag = null
                        localTags = latestTags
                    }
                )
            }
    ) {
        val session = drag
        val displayed = session?.tags ?: localTags
        Row(
            Modifier.horizontalScroll(scroll, enabled = session == null)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayed.forEachIndexed { index, tag ->
                key(tag.id) {
                    val shift = if (session == null) 0f else {
                        val distance = session.sizes[session.from].width + gap
                        when {
                            session.deleting -> if (index > session.from) -distance else 0f
                            index > session.from && index <= session.target -> -distance
                            index < session.from && index >= session.target -> distance
                            else -> 0f
                        }
                    }
                    // After committing, the Row has the new order already: discard old offsets.
                    val animatedShift by key(session) {
                        animateFloatAsState(shift, tween(140), label = "tag_position")
                    }
                    Box(Modifier.onSizeChanged { sizes[tag.id] = it }) {
                        TagChip(
                            name = tag.name, colorHex = tag.colorHex, icon = tag.icon,
                            modifier = Modifier.graphicsLayer {
                                translationX = animatedShift
                                alpha = if (session?.tag?.id == tag.id) 0f else 1f
                            },
                            onClick = { if (drag == null) onNavigate(tag.id) }
                        )
                    }
                }
            }
            Box(Modifier.width(if (displayed.isEmpty()) 160.dp else 88.dp)) {
                if (session == null) {
                    AddTagChip(if (displayed.isEmpty()) "添加一个标签" else "添加", onAdd)
                } else {
                    FilterChip(
                        selected = false, onClick = {},
                        shape = RoundedCornerShape(8.dp),
                        label = { Text("移除") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, "移除标签", Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (session.deleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                            labelColor = if (session.deleting) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                            iconColor = if (session.deleting) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }
        }
        if (session != null) {
            TagChip(
                name = session.tag.name, colorHex = session.tag.colorHex, icon = session.tag.icon,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(
                        with(density) { session.sizes[session.from].width.toDp() },
                        with(density) { session.sizes[session.from].height.toDp() }
                    )
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = session.viewportLeft
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(8.dp)
                        clip = false
                        alpha = if (session.deleting) 0.5f else 1f
                    }
            )
        }
    }
}
