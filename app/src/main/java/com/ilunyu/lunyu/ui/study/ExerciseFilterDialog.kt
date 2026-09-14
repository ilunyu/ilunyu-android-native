package com.ilunyu.lunyu.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 试题筛选对话框：完全对齐 Flutter 版本的 showDialog<bool> AlertDialog。
 * 包含学年、地区、年级、题型 4 个多选维度，
 * 底部提供“清除”、“取消”和胶囊状“应用”按钮。
 */
@Composable
fun ExerciseFilterDialog(
    availableYears: List<String>,
    availableSources: List<String>,
    availableTypes: List<String>,
    initialYears: Set<String>,
    initialSources: Set<String>,
    initialGrades: Set<Int>,
    initialTypes: Set<String>,
    onDismiss: () -> Unit,
    onApply: (years: Set<String>, sources: Set<String>, grades: Set<Int>, types: Set<String>) -> Unit
) {
    var tempYears by remember { mutableStateOf(initialYears) }
    var tempSources by remember { mutableStateOf(initialSources) }
    var tempGrades by remember { mutableStateOf(initialGrades) }
    var tempTypes by remember { mutableStateOf(initialTypes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "筛选题目",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 学年
                if (availableYears.isNotEmpty()) {
                    FilterGroup(
                        title = "学年",
                        items = availableYears,
                        selected = tempYears,
                        itemLabel = { it },
                        onToggle = { year ->
                            tempYears = if (tempYears.contains(year)) tempYears - year else tempYears + year
                        }
                    )
                }

                // 地区
                if (availableSources.isNotEmpty()) {
                    FilterGroup(
                        title = "地区",
                        items = availableSources,
                        selected = tempSources,
                        itemLabel = { it },
                        onToggle = { source ->
                            tempSources = if (tempSources.contains(source)) tempSources - source else tempSources + source
                        }
                    )
                }

                // 年级 (1: 高一, 2: 高二, 3: 高三)
                FilterGroup(
                    title = "年级",
                    items = listOf(1, 2, 3),
                    selected = tempGrades,
                    itemLabel = { grade ->
                        when (grade) {
                            1 -> "高一"
                            2 -> "高二"
                            3 -> "高三"
                            else -> "高$grade"
                        }
                    },
                    onToggle = { grade ->
                        tempGrades = if (tempGrades.contains(grade)) tempGrades - grade else tempGrades + grade
                    }
                )

                // 题型
                if (availableTypes.isNotEmpty()) {
                    FilterGroup(
                        title = "题型",
                        items = availableTypes,
                        selected = tempTypes,
                        itemLabel = { it },
                        onToggle = { type ->
                            tempTypes = if (tempTypes.contains(type)) tempTypes - type else tempTypes + type
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(tempYears, tempSources, tempGrades, tempTypes)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "应用", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        tempYears = emptySet()
                        tempSources = emptySet()
                        tempGrades = emptySet()
                        tempTypes = emptySet()
                    }
                ) {
                    Text(
                        text = "清除",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
private fun <T> FilterGroup(
    title: String,
    items: List<T>,
    selected: Set<T>,
    itemLabel: (T) -> String,
    onToggle: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        items.forEach { item ->
            val isChecked = selected.contains(item)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(item) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = itemLabel(item),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle(item) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
