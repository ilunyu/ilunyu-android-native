package com.ilunyu.lunyu.ui.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TagWithCounts
import com.ilunyu.lunyu.data.repository.TAG_PRESET_COLORS

/**
 * 辅助方法：解析 16 进制颜色字符串为 Compose Color
 */
fun parseTagColor(hex: String?, fallback: Color = Color(0xFF008080)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        fallback
    }
}

/**
 * 辅助方法：根据图标标识获取 ImageVector
 */
fun getTagImageVector(icon: String?): ImageVector? {
    return when (icon?.lowercase()) {
        "star" -> Icons.Outlined.Star
        "person" -> Icons.Outlined.Person
        "lightbulb" -> Icons.Outlined.Lightbulb
        "question_answer" -> Icons.Outlined.QuestionAnswer
        else -> null
    }
}

/**
 * 标签徽标 (TagChip)
 * 严格对齐题目列表同款 MD3 FilterChip 规范（尺寸、颜色、边框、间距一致）
 * 规范：
 * 1. 使用 Material Design 3 标准 FilterChip
 * 2. 仅显示左侧可能存在的图标/颜色圆，以及标签名字（彻底去除 #，彻底去除 x）
 * 3. 图标与色彩二选一；若用户均未设置，则不加颜色和图标，直接作为普通文本 label 展示
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String? = null,
    icon: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cleanName = name.trim().removePrefix("#").trim()
    val iconVector = getTagImageVector(icon)
    val hasColor = !colorHex.isNullOrBlank()

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FilterChip(
            selected = false,
            onClick = { onClick?.invoke() },
            label = {
                Text(
                    text = cleanName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            },
            leadingIcon = if (iconVector != null || hasColor) {
                {
                    if (iconVector != null) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (hasColor) {
                        val tagColor = parseTagColor(colorHex)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(color = tagColor, shape = CircleShape)
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(8.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .height(32.dp)
                .then(modifier)
        )
    }
}

/**
 * 添加标签的引导 Chip（呈现 Material Symbol New 的 New Label 图标）
 * 严格对齐题目列表同款 MD3 FilterChip 规范（尺寸、颜色、边框完全一致）
 */
@Composable
fun AddTagChip(
    label: String = "添加标签",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FilterChip(
            selected = false,
            onClick = onClick,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.NewLabel,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .height(32.dp)
                .then(modifier)
        )
    }
}

/**
 * 点击已有标签 Chip 弹出的操作选项对话框：
 * 允许用户直接查看该标签关联内容，或从当前章节中解除关联
 */
@Composable
fun TagActionDialog(
    tagName: String,
    colorHex: String? = null,
    icon: String? = null,
    onNavigateToTag: () -> Unit,
    onRemoveFromItem: () -> Unit,
    onDismiss: () -> Unit
) {
    val cleanName = tagName.trim().removePrefix("#").trim()
    val iconVector = getTagImageVector(icon)
    val hasColor = !colorHex.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (hasColor) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color = parseTagColor(colorHex), shape = CircleShape)
                    )
                }
                Text(
                    text = cleanName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToTag()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QuestionAnswer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "查看该标签下的所有内容",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onRemoveFromItem()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "从本章移除此标签",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 为章节或试题打标签的 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagSelectionBottomSheet(
    targetTitle: String,
    allTags: List<TagWithCounts>,
    attachedTagIds: Set<String>,
    onToggleTag: (tagId: String) -> Unit,
    onCreateTag: (name: String, colorHex: String?) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var newTagName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "添加标签",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = targetTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 新建标签快速输入行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = {
                        newTagName = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = { Text("新建标签名称…", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val trimmed = newTagName.trim()
                        if (trimmed.isNotBlank()) {
                            if (trimmed == "收藏" || trimmed == "全部收藏") {
                                errorMessage = "不能使用系统预留名称“$trimmed”"
                            } else {
                                onCreateTag(trimmed, null)
                                newTagName = ""
                            }
                        } else {
                            errorMessage = "标签名不能为空"
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加", fontWeight = FontWeight.SemiBold)
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 标签列表流式布局
            Text(
                text = "可选标签",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (allTags.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无标签，在上方输入名称创建第一个标签",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tagWithCount ->
                            val tag = tagWithCount.tag
                            val isSelected = attachedTagIds.contains(tag.id)
                            val tagColor = parseTagColor(tag.colorHex)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) tagColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, tagColor)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleTag(tag.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = tagColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(tagColor, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 标签编辑/重命名对话框：
 * - 样式完全对齐 AddTagDialog（20dp 圆角 Surface 容器、12dp OutlinedTextField、前置图标、实时字数计数与校验、4 种图标 + 9 种颜色横向滚动单选）
 * - 智能防重名校验（排除自身当前名称，允许保留原名；改名且与其他已有标签重名时拦截）
 * - 支持同时编辑/切换图标与颜色（互斥单选）
 */
@Composable
fun TagEditDialog(
    initialName: String,
    initialColorHex: String?,
    initialIcon: String? = null,
    allExistingTags: List<TagEntity> = emptyList(),
    title: String = "编辑标签",
    onConfirm: (newName: String, newColorHex: String?, newIcon: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColorHex) }

    val trimmed = tagName.trim().removePrefix("#").trim()
    val isBlank = trimmed.isBlank()
    val isTooLong = trimmed.length > 8
    val isReserved = trimmed == "收藏" || trimmed == "全部收藏"
    val existingMatch = remember(trimmed, allExistingTags) {
        if (trimmed.isEmpty()) null
        else allExistingTags.find { it.name.equals(trimmed, ignoreCase = true) }
    }
    val isDuplicate = existingMatch != null && !existingMatch.name.equals(initialName.trim(), ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // 1. TextField
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { input ->
                        if (input.length <= 8) {
                            tagName = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                    label = { Text("标签名称") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isReserved) {
                                Text(
                                    text = "不能使用系统预留名称“$trimmed”",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (isDuplicate) {
                                Text(
                                    text = "已存在同名标签",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "不超过 8 字，不得与其他标签重复",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${trimmed.length}/8",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = isDuplicate || isTooLong || isReserved,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 2. 选择图标或颜色行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconOptions = listOf(
                        "star" to Icons.Outlined.Star,
                        "person" to Icons.Outlined.Person,
                        "lightbulb" to Icons.Outlined.Lightbulb,
                        "question_answer" to Icons.Outlined.QuestionAnswer
                    )
                    iconOptions.forEach { (id, vector) ->
                        val isSelected = selectedIcon == id
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    selectedIcon = null
                                } else {
                                    selectedIcon = id
                                    selectedColor = null
                                }
                            },
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = id,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 颜色实心圆选项
                    TAG_PRESET_COLORS.forEach { hex ->
                        val color = parseTagColor(hex)
                        val isSelected = selectedColor?.equals(hex, ignoreCase = true) == true
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    selectedColor = null
                                } else {
                                    selectedColor = hex
                                    selectedIcon = null
                                }
                            },
                            shape = CircleShape,
                            color = color,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 底部取消与保存按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (!isBlank && !isTooLong && !isDuplicate && !isReserved) {
                                onConfirm(trimmed, selectedColor, selectedIcon)
                            }
                        },
                        enabled = !isBlank && !isTooLong && !isDuplicate && !isReserved
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 标签删除确认对话框：
 * - 遵循 MD3 破坏性操作规范：顶部加入 Hero 警告/删除图标
 * - 信息层级清晰化：主提示加粗突出，副说明交代解除关联与数据安全性
 * - 按钮危险级强调：使用红色警示色
 */
@Composable
fun TagDeleteConfirmDialog(
    tagName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = "删除标签",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "确定要删除标签“$tagName”吗？",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "删除后该标签将从所有已标记的章节和试题中解除关联，章节和试题本身不会受到任何影响。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 创建/添加标签 Dialog（完全遵照设计规范）：
 * - 标题：添加标签
 * - TextField：10字符以内，防重名校验
 * - 建议 Chips 行：实时根据匹配情况给出未附加标签建议，点按直接添加并关闭 dialog
 * - 选择代表图标或颜色行：4种图标（星星、人物、灯泡、对话）与 9 种国学配色实心圆，点按可自选（亦可不选）
 * - 底部：取消与确认文字按钮
 */
@Composable
fun AddTagDialog(
    allExistingTags: List<TagEntity>,
    attachedTagIds: Set<String>,
    onSelectExistingTag: (TagEntity) -> Unit,
    onCreateNewTag: (name: String, colorHex: String?, icon: String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    val trimmed = tagName.trim().removePrefix("#").trim()
    val isBlank = trimmed.isBlank()
    val isTooLong = trimmed.length > 8
    val isReserved = trimmed == "收藏" || trimmed == "全部收藏"
    val existingMatch = remember(trimmed, allExistingTags) {
        if (trimmed.isEmpty()) null
        else allExistingTags.find { it.name.equals(trimmed, ignoreCase = true) }
    }
    val isDuplicate = existingMatch != null
    val isAlreadyAttached = existingMatch != null && attachedTagIds.contains(existingMatch.id)

    // 建议标签 Chips 行：仅当用户输入内容之后才出现，只出现匹配了的 label（按子串模糊匹配）
    val suggestionTags = remember(trimmed, allExistingTags, attachedTagIds) {
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            val available = allExistingTags.filter { !attachedTagIds.contains(it.id) }
            available.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 280.dp, max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = "添加标签",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // 1. TextField（MD3 Outlined 样式，灰色 Leading Icon，Primary 主题色聚焦）
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { input ->
                        if (input.length <= 8) {
                            tagName = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                    label = { Text("标签名称") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NewLabel,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isReserved) {
                                Text(
                                    text = "不能使用系统预留名称“$trimmed”",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (isDuplicate) {
                                Text(
                                    text = if (isAlreadyAttached) "本章已添加该标签" else "已存在该标签，可点下方建议添加",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "不超过 8 字，不得与已有标签重复",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${trimmed.length}/8",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    isError = isDuplicate || isTooLong || isReserved,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // 2. 建议 Chips 行（仅当用户输入内容之后且有匹配建议时出现；滑动边缘贴齐 dialog 边缘，滑动边界与 24dp 内容对齐）
                if (suggestionTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        suggestionTags.forEach { tag ->
                            TagChip(
                                name = tag.name,
                                colorHex = tag.colorHex,
                                icon = tag.icon,
                                onClick = {
                                    onSelectExistingTag(tag)
                                    onDismissRequest()
                                }
                            )
                        }
                    }
                }

                // 3. 选择图标或颜色行（滑动边缘贴齐 dialog 边缘，滑动边界与 24dp 内容对齐）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 4 种图标：星星 Star, 人物 Person, 概念灯泡 Lightbulb, 对话 QuestionAnswer
                    val iconOptions = listOf(
                        "star" to Icons.Outlined.Star,
                        "person" to Icons.Outlined.Person,
                        "lightbulb" to Icons.Outlined.Lightbulb,
                        "question_answer" to Icons.Outlined.QuestionAnswer
                    )
                    iconOptions.forEach { (id, vector) ->
                        val isSelected = selectedIcon == id
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    selectedIcon = null
                                } else {
                                    selectedIcon = id
                                    selectedColor = null
                                }
                            },
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = id,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 颜色实心圆选项
                    TAG_PRESET_COLORS.forEach { hex ->
                        val color = parseTagColor(hex)
                        val isSelected = selectedColor == hex
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    selectedColor = null
                                } else {
                                    selectedColor = hex
                                    selectedIcon = null
                                }
                            },
                            shape = CircleShape,
                            color = color,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 底部取消与确认按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (!isBlank && !isTooLong && !isDuplicate && !isReserved) {
                                onCreateNewTag(trimmed, selectedColor, selectedIcon)
                                onDismissRequest()
                            }
                        },
                        enabled = !isBlank && !isTooLong && !isDuplicate && !isReserved
                    ) {
                        Text("确认", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

