package com.ilunyu.lunyu.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一的章节/试题统计数据栏。
 *
 * 采用固定的高度并在垂直方向居中排列子元素，消除不同字体（Sans/Serif/Kai）
 * 以及有无尾部操作按钮（如排序器）对统计栏总高度及文字基线带来的差异，保证全局一致性。
 *
 * @param countText 统计说明文本（如 "共 16 章" 或 "筛选出 10 题·共 20 题"）
 * @param modifier 外部 Modifier
 * @param height 统计栏固定高度，默认为 54.dp
 * @param action 尾部操作项（如排序切换器），为 null 时仅展示统计说明文本
 */
@Composable
fun SectionCountBar(
    countText: String,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(
                start = 24.dp,
                end = if (action != null) 12.dp else 24.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = countText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        if (action != null) {
            action()
        }
    }
}
