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
import androidx.compose.material.icons.filled.Edit
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
                                .size(8.dp)
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
                            .size(10.dp)
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
                            onCreateTag(trimmed, null)
                            newTagName = ""
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
 * 标签编辑/重命名对话框
 */
@Composable
fun TagEditDialog(
    initialName: String,
    initialColorHex: String,
    title: String = "编辑标签",
    onConfirm: (newName: String, newColorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("标签名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "主题色彩",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 颜色选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TAG_PRESET_COLORS.forEach { colorHex ->
                        val isPicked = selectedColorHex.equals(colorHex, ignoreCase = true)
                        val color = parseTagColor(colorHex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isPicked) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        errorText = "名称不能为空"
                    } else {
                        onConfirm(trimmed, selectedColorHex)
                    }
                }
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
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
 * 标签删除确认对话框
 */
@Composable
fun TagDeleteConfirmDialog(
    tagName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "删除标签", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "确定要删除标签“$tagName”吗？\n删除后该标签将从所有已标记的章节和试题中解除关联，章节和试题本身不会受到任何影响。",
                style = MaterialTheme.typography.bodyMedium
            )
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
    val isTooLong = trimmed.length > 10
    val existingMatch = remember(trimmed, allExistingTags) {
        if (trimmed.isEmpty()) null
        else allExistingTags.find { it.name.equals(trimmed, ignoreCase = true) }
    }
    val isDuplicate = existingMatch != null
    val isAlreadyAttached = existingMatch != null && attachedTagIds.contains(existingMatch.id)

    // 实时建议标签 Chips 行：找出尚未附加到本章的标签，支持按输入前缀/子串实时过滤
    val suggestionTags = remember(trimmed, allExistingTags, attachedTagIds) {
        val available = allExistingTags.filter { !attachedTagIds.contains(it.id) }
        if (trimmed.isEmpty()) {
            available
        } else {
            available.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "添加标签",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. TextField
                Column {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                tagName = input
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("输入标签名称（10字以内）") },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isDuplicate) {
                                    Text(
                                        text = if (isAlreadyAttached) "本章已添加该标签" else "已存在该标签，可点下方建议添加",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "不得与已有标签重复",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${trimmed.length}/10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        isError = isDuplicate || isTooLong,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. 可能存在的 chips 建议行
                if (suggestionTags.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "已有标签建议（点击直接添加）：",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
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
                }

                // 3. 选择图标或颜色行
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "选择代表图标或颜色（可选）：",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = id,
                                        modifier = Modifier.size(17.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 分隔线
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

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
                                border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.size(26.dp)
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isBlank && !isTooLong && !isDuplicate) {
                        onCreateNewTag(trimmed, selectedColor, selectedIcon)
                        onDismissRequest()
                    }
                },
                enabled = !isBlank && !isTooLong && !isDuplicate
            ) {
                Text("确认", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

