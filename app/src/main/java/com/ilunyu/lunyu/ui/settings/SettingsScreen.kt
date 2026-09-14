package com.ilunyu.lunyu.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ilunyu.lunyu.ui.common.LunyuCollapsibleTopBarLayout
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import com.ilunyu.lunyu.ui.common.rememberLunyuTopBarScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.model.AppFontPreference
import com.ilunyu.lunyu.data.model.AppThemeMode
import com.ilunyu.lunyu.ui.theme.getFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: AppThemeMode,
    currentFontPreference: AppFontPreference,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onFontPreferenceChanged: (AppFontPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val isScrolledUnder by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        LunyuTopBar(
            showDivider = isScrolledUnder,
            title = {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
            // 视觉小标题
            item {
                Text(
                    text = "视觉",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                )
            }

            // 1. 显示模式标题
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "显示模式",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // 显示模式 3 选 1 卡片
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                        icon = Icons.Outlined.BrightnessAuto,
                        label = "跟随设备",
                        onClick = { onThemeModeChanged(AppThemeMode.SYSTEM) }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentThemeMode == AppThemeMode.LIGHT,
                        icon = Icons.Outlined.LightMode,
                        label = "浅色模式",
                        onClick = { onThemeModeChanged(AppThemeMode.LIGHT) }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentThemeMode == AppThemeMode.DARK,
                        icon = Icons.Outlined.DarkMode,
                        label = "深色模式",
                        onClick = { onThemeModeChanged(AppThemeMode.DARK) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 阅读字体标题
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FontDownload,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "阅读字体",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // 阅读字体 3 选 1 卡片
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FontOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentFontPreference == AppFontPreference.SANS,
                        fontFamily = getFontFamily(AppFontPreference.SANS, context.assets),
                        label = "思源黑体",
                        onClick = { onFontPreferenceChanged(AppFontPreference.SANS) }
                    )
                    FontOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentFontPreference == AppFontPreference.SERIF,
                        fontFamily = getFontFamily(AppFontPreference.SERIF, context.assets),
                        label = "思源宋体",
                        onClick = { onFontPreferenceChanged(AppFontPreference.SERIF) }
                    )
                    FontOptionCard(
                        modifier = Modifier.weight(1f),
                        isSelected = currentFontPreference == AppFontPreference.SYSTEM,
                        fontFamily = FontFamily.Default, // 强制系统默认字体
                        label = "系统字体",
                        onClick = { onFontPreferenceChanged(AppFontPreference.SYSTEM) }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 全宽分割线
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 关于小标题
            item {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
                )
            }

            // 3. 更多资源
            item {
                SettingListItem(
                    icon = Icons.Outlined.AutoStories,
                    title = "更多资源",
                    subtitle = "在 ilunyu 主页获取更多试题与版本",
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = {
                        openUrl(context, "https://github.com/ilunyu")
                    }
                )
            }

            // 4. 开源仓库
            item {
                SettingListItem(
                    icon = Icons.Outlined.Code,
                    title = "开源仓库",
                    subtitle = "github.com/ilunyu/ilunyu",
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = {
                        openUrl(context, "https://github.com/ilunyu/ilunyu")
                    }
                )
            }

            // 5. 意见反馈
            item {
                SettingListItem(
                    icon = Icons.Outlined.Mail,
                    title = "意见反馈",
                    subtitle = "liuct05@foxmail.com",
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    onClick = {
                        sendFeedback(context)
                    }
                )
                Spacer(modifier = Modifier.height(36.dp))
            }

            // 底部居中软件信息（无图标）
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "论语",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "以 Material Design 3 呈现的论语研读与学习工具",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}
}

@Composable
private fun ThemeOptionCard(
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = textColor
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FontOptionCard(
    isSelected: Boolean,
    fontFamily: FontFamily,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val visualColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "文",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    color = visualColor
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = textColor
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "无法打开链接: $url", Toast.LENGTH_SHORT).show()
    }
}

private fun sendFeedback(context: Context) {
    val email = "liuct05@foxmail.com"
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "[论语App] 用户反馈与建议")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Feedback Email", email)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "未检测到可用邮件应用，反馈邮箱已复制到剪贴板（$email）", Toast.LENGTH_LONG).show()
    }
}
