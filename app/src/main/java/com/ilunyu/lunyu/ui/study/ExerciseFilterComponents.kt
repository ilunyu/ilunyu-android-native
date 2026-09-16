package com.ilunyu.lunyu.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 试题筛选地区官方排序规定：
 * 北京、东城、西城、海淀、朝阳、丰台、石景山、通州、大兴、昌平、顺义、房山、门头沟、平谷、怀柔、密云、延庆
 */
val DISTRICT_ORDER = listOf(
    "北京", "东城", "西城", "海淀", "朝阳", "丰台", "石景山",
    "通州", "大兴", "昌平", "顺义", "房山", "门头沟", "平谷",
    "怀柔", "密云", "延庆"
)

/**
 * 试题筛选类别官方排序规定：
 * 真题、一模、二模、期中、期末、其他
 */
val TYPE_ORDER = listOf(
    "真题", "一模", "二模", "期中", "期末", "其他"
)

/**
 * 按指定顺序对地区进行排序
 */
fun sortSources(sources: Collection<String>): List<String> {
    val orderMap = DISTRICT_ORDER.withIndex().associate { it.value to it.index }
    return sources.sortedWith(
        compareBy<String> { orderMap[it] ?: (DISTRICT_ORDER.size + 1) }
            .thenBy { it }
    )
}

/**
 * 按指定顺序对试卷类别进行排序
 */
fun sortTypes(types: Collection<String>): List<String> {
    val orderMap = TYPE_ORDER.withIndex().associate { it.value to it.index }
    return types.sortedWith(
        compareBy<String> { orderMap[it] ?: (TYPE_ORDER.size + 1) }
            .thenBy { it }
    )
}

/**
 * 试题筛选维度定义
 */
enum class ExerciseFilterDimension(val title: String) {
    YEAR("学年"),
    SOURCE("地区"),
    GRADE("年级"),
    TYPE("类别")
}

/**
 * 计算学年 Chip 文案：
 * - 未筛选：显示默认提示“学年”；
 * - 筛选 1 个：显示筛选内容（如“2024”）；
 * - 筛选多个：显示个数（如“2个学年”）。
 */
fun getYearChipLabel(selected: Set<String>): String = when {
    selected.isEmpty() -> "学年"
    selected.size == 1 -> selected.first()
    else -> "${selected.size}个学年"
}

/**
 * 计算地区 Chip 文案：
 * - 未筛选：显示默认提示“地区”；
 * - 筛选 1 个：显示筛选内容（如“东城”）；
 * - 筛选多个：显示个数（如“3个地区”）。
 */
fun getSourceChipLabel(selected: Set<String>): String = when {
    selected.isEmpty() -> "地区"
    selected.size == 1 -> selected.first()
    else -> "${selected.size}个地区"
}

/**
 * 计算年级 Chip 文案：
 * - 未筛选：显示默认提示“年级”；
 * - 筛选 1 个：显示年级名称（如“高一”、“高二”、“高三”）；
 * - 筛选 3 个全选：显示“全部年级”；
 * - 筛选 2 个：显示“2个年级”。
 */
fun getGradeChipLabel(selected: Set<Int>): String = when {
    selected.isEmpty() -> "年级"
    selected.size == 1 -> when (selected.first()) {
        1 -> "高一"
        2 -> "高二"
        3 -> "高三"
        else -> "高${selected.first()}"
    }
    selected.size == 3 -> "全部年级"
    else -> "${selected.size}个年级"
}

/**
 * 计算类别 Chip 文案：
 * - 未筛选：显示默认提示“类别”；
 * - 筛选 1 个：显示筛选内容（如“期末”）；
 * - 筛选多个：显示个数（如“4类试卷”）。
 */
fun getTypeChipLabel(selected: Set<String>): String = when {
    selected.isEmpty() -> "类别"
    selected.size == 1 -> selected.first()
    else -> "${selected.size}类试卷"
}

/**
 * 题目列表上方的四个 Filter Chips 横向滚动组件：
 * - 依次为 学年、地区、年级、类别；
 * - 未筛选时：左侧无图标，右侧为向下展开小三角；
 * - 筛选激活时：背景色高亮，左侧打勾，右侧为向下展开小三角；
 * - 宽度超出页面时支持向右横向滑动延伸。
 */
@Composable
fun ExerciseFilterChipsRow(
    selectedYears: Set<String>,
    selectedSources: Set<String>,
    selectedGrades: Set<Int>,
    selectedTypes: Set<String>,
    onChipClick: (ExerciseFilterDimension) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 学年
        FilterDimensionChip(
            label = getYearChipLabel(selectedYears),
            isActive = selectedYears.isNotEmpty(),
            onClick = { onChipClick(ExerciseFilterDimension.YEAR) }
        )

        // 2. 地区
        FilterDimensionChip(
            label = getSourceChipLabel(selectedSources),
            isActive = selectedSources.isNotEmpty(),
            onClick = { onChipClick(ExerciseFilterDimension.SOURCE) }
        )

        // 3. 年级
        FilterDimensionChip(
            label = getGradeChipLabel(selectedGrades),
            isActive = selectedGrades.isNotEmpty(),
            onClick = { onChipClick(ExerciseFilterDimension.GRADE) }
        )

        // 4. 类别
        FilterDimensionChip(
            label = getTypeChipLabel(selectedTypes),
            isActive = selectedTypes.isNotEmpty(),
            onClick = { onChipClick(ExerciseFilterDimension.TYPE) }
        )
    }
}

/**
 * 单个 Filter Chip 实现，严格对齐 Material Design 3 FilterChip 规范
 */
@Composable
private fun FilterDimensionChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1
            )
        },
        leadingIcon = if (isActive) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isActive,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

/**
 * 试题多选筛选底部 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFilterBottomSheet(
    dimension: ExerciseFilterDimension,
    availableYears: List<String>,
    availableSources: List<String>,
    availableTypes: List<String>,
    selectedYears: Set<String>,
    selectedSources: Set<String>,
    selectedGrades: Set<Int>,
    selectedTypes: Set<String>,
    onDismiss: () -> Unit,
    onApply: (years: Set<String>, sources: Set<String>, grades: Set<Int>, types: Set<String>) -> Unit
) {
    var tempYears by remember(selectedYears) { mutableStateOf(selectedYears) }
    var tempSources by remember(selectedSources) { mutableStateOf(selectedSources) }
    var tempGrades by remember(selectedGrades) { mutableStateOf(selectedGrades) }
    var tempTypes by remember(selectedTypes) { mutableStateOf(selectedTypes) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentHasSelection = when (dimension) {
        ExerciseFilterDimension.YEAR -> tempYears.isNotEmpty()
        ExerciseFilterDimension.SOURCE -> tempSources.isNotEmpty()
        ExerciseFilterDimension.GRADE -> tempGrades.isNotEmpty()
        ExerciseFilterDimension.TYPE -> tempTypes.isNotEmpty()
    }

    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.70f).dp

    val sortedSources = remember(availableSources) { sortSources(availableSources) }
    val sortedTypes = remember(availableTypes) { sortTypes(availableTypes) }

    val cardItems: List<FilterCardItem> = when (dimension) {
        ExerciseFilterDimension.YEAR -> availableYears.map { year ->
            FilterCardItem(
                id = year,
                label = year,
                isSelected = tempYears.contains(year),
                onToggle = {
                    tempYears = if (tempYears.contains(year)) tempYears - year else tempYears + year
                }
            )
        }
        ExerciseFilterDimension.SOURCE -> sortedSources.map { source ->
            FilterCardItem(
                id = source,
                label = source,
                isSelected = tempSources.contains(source),
                onToggle = {
                    tempSources = if (tempSources.contains(source)) tempSources - source else tempSources + source
                }
            )
        }
        ExerciseFilterDimension.GRADE -> listOf(1, 2, 3).map { grade ->
            val gradeLabel = when (grade) {
                1 -> "高一"
                2 -> "高二"
                3 -> "高三"
                else -> "高$grade"
            }
            FilterCardItem(
                id = grade.toString(),
                label = gradeLabel,
                isSelected = tempGrades.contains(grade),
                onToggle = {
                    tempGrades = if (tempGrades.contains(grade)) tempGrades - grade else tempGrades + grade
                }
            )
        }
        ExerciseFilterDimension.TYPE -> sortedTypes.map { type ->
            FilterCardItem(
                id = type,
                label = type,
                isSelected = tempTypes.contains(type),
                onToggle = {
                    tempTypes = if (tempTypes.contains(type)) tempTypes - type else tempTypes + type
                }
            )
        }
    }

    val rows = remember(cardItems) { cardItems.chunked(3) }

    ModalBottomSheet(
        onDismissRequest = {
            onApply(tempYears, tempSources, tempGrades, tempTypes)
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(bottom = 12.dp)
        ) {
            // 顶栏：标题与重置按钮（小标题字号对齐章阅读/题目小标题 18sp SemiBold）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选${dimension.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                TextButton(
                    onClick = {
                        when (dimension) {
                            ExerciseFilterDimension.YEAR -> tempYears = emptySet()
                            ExerciseFilterDimension.SOURCE -> tempSources = emptySet()
                            ExerciseFilterDimension.GRADE -> tempGrades = emptySet()
                            ExerciseFilterDimension.TYPE -> tempTypes = emptySet()
                        }
                    },
                    enabled = currentHasSelection
                ) {
                    Text(
                        text = "重置",
                        color = if (currentHasSelection) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 选项卡片网格（一行三个、圆角 8px 矩形卡片）
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            FilterGridCard(
                                label = item.label,
                                isSelected = item.isSelected,
                                onToggle = item.onToggle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 底部“确定”按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Button(
                    onClick = {
                        onApply(tempYears, tempSources, tempGrades, tempTypes)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "确定",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

private data class FilterCardItem(
    val id: String,
    val label: String,
    val isSelected: Boolean,
    val onToggle: () -> Unit
)

/**
 * 筛选卡片组件：
 * 一行三个、圆角为 8px 的圆角矩形卡片；
 * 未选中为默认表面颜色，点按有水波纹（Surface 自带交互状态水波纹）；
 * 选中为青绿色加粗且左侧出现对勾 icon。
 */
@Composable
private fun FilterGridCard(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (label.length > 5) 12.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
