package com.ilunyu.lunyu.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilunyu.lunyu.data.db.InstalledResourceEntity
import com.ilunyu.lunyu.data.resource.ContentSnapshot
import com.ilunyu.lunyu.data.resource.ResourceKind
import com.ilunyu.lunyu.data.resource.ResourceLocationType
import com.ilunyu.lunyu.data.resource.ResourceOperationState
import com.ilunyu.lunyu.data.resource.ResourceRegistry
import com.ilunyu.lunyu.data.resource.ResourceRegistryPackage
import com.ilunyu.lunyu.ui.common.LunyuTopBar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ResourceManagementScreen(
    snapshot: ContentSnapshot,
    installedResources: List<InstalledResourceEntity>,
    registry: ResourceRegistry?,
    operationState: ResourceOperationState,
    onClearOperationState: () -> Unit = {},
    onAddUrl: (String) -> Unit,
    onRefresh: suspend () -> Result<ResourceRegistry>,
    onDownload: (String) -> Unit,
    onSelectEdition: (InstalledResourceEntity) -> Unit,
    onSetExerciseEnabled: (String, Boolean) -> Unit,
    onDeleteResource: (InstalledResourceEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isAddingFromUrl by remember { mutableStateOf(false) }
    val isAddLoading = isAddingFromUrl || (operationState is ResourceOperationState.Downloading && operationState.packageId == "URL 资源") || (isAddingFromUrl && operationState is ResourceOperationState.Installing)
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var resourceUrl by rememberSaveable { mutableStateOf("") }
    var resourcePendingDeletion by remember { mutableStateOf<InstalledResourceEntity?>(null) }

    // 全局互斥状态：记录当前展开的列表项唯一 key，保证同一时间只能有一个列表项展开
    var revealedItemKey by remember { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()
    val isScrolledUnder by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    // 当列表发生滚动时，自动收起已展开的列表项
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            revealedItemKey = null
        }
    }

    // 页面离开时，确保已结束的操作状态被清除，防止重新进入时重复弹出 Snackbar
    DisposableEffect(Unit) {
        onDispose {
            onClearOperationState()
        }
    }

    // 监听资源操作结果（成功或失败通过 Snackbar 进行报告，消费后立即重置为 Idle）
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is ResourceOperationState.Complete -> {
                isAddingFromUrl = false
                val pkgName = installedResources.find { it.packageId == state.packageId }?.name
                    ?: registry?.packages?.find { it.packageId == state.packageId }?.name
                    ?: state.packageId
                val message = "$pkgName ${state.versionName} 已成功安装"
                onClearOperationState()
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                    )
                }
            }
            is ResourceOperationState.Failed -> {
                isAddingFromUrl = false
                val errorMsg = state.message.takeIf { it.isNotBlank() } ?: "操作失败"
                val message = "资源获取失败：$errorMsg"
                onClearOperationState()
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = message,
                        withDismissAction = true,
                    )
                }
            }
            else -> {}
        }
    }

    val copyToClipboard: (String) -> Unit = { url ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Resource Download Link", url))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "已复制下载链接到剪切板",
                withDismissAction = true,
            )
        }
    }

    val installedEditions = remember(installedResources) {
        installedResources.filter { it.kind == ResourceKind.EDITION }
    }
    val uninstalledEditions = remember(registry, installedResources) {
        registry?.packages?.filter { pkg ->
            pkg.kind == ResourceKind.EDITION && installedResources.none { it.packageId == pkg.packageId }
        } ?: emptyList()
    }

    val installedExercises = remember(installedResources) {
        installedResources.filter { it.kind == ResourceKind.EXERCISE }
    }
    val uninstalledExercises = remember(registry, installedResources) {
        registry?.packages?.filter { pkg ->
            pkg.kind == ResourceKind.EXERCISE && installedResources.none { it.packageId == pkg.packageId }
        } ?: emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // 点击屏幕空白区域时收起展开项
                if (revealedItemKey != null) {
                    revealedItemKey = null
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LunyuTopBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(
                        text = "资源管理",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                },
                actions = {
                    // 靠左：检查更新 / 刷新官方资源目录（静态 icon 改为 Update）
                    IconButton(
                        onClick = {
                            revealedItemKey = null
                            if (!isCheckingUpdates) {
                                scope.launch {
                                    isCheckingUpdates = true
                                    val result = try {
                                        onRefresh()
                                    } catch (e: Exception) {
                                        Result.failure(e)
                                    }
                                    isCheckingUpdates = false

                                    if (result.isFailure) {
                                        val errorMsg = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "网络连接异常"
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            message = "检查更新失败：$errorMsg",
                                            withDismissAction = true,
                                        )
                                    } else {
                                        val newUpdates = checkUpdates(result.getOrNull(), installedResources)
                                        val updateMsg = when {
                                            newUpdates.isEmpty() -> "所有资源均为最新版"
                                            newUpdates.size == 1 -> {
                                                val name = if (newUpdates[0].name.endsWith("资源库")) newUpdates[0].name else "${newUpdates[0].name}资源库"
                                                "${name}有更新"
                                            }
                                            else -> {
                                                val name = if (newUpdates[0].name.endsWith("资源库")) newUpdates[0].name else "${newUpdates[0].name}资源库"
                                                "${name}等 ${newUpdates.size} 个资源库有更新"
                                            }
                                        }
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            message = updateMsg,
                                            withDismissAction = true,
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdates,
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Update,
                                contentDescription = "检查更新",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 靠右：从 URL 添加资源加号按钮（加载中呈现转动指示器，不可重复点击）
                    IconButton(
                        onClick = {
                            revealedItemKey = null
                            showAddDialog = true
                        },
                        enabled = !isAddLoading,
                    ) {
                        if (isAddLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "从 URL 添加资源",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                showDivider = isScrolledUnder,
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
                // 1. 译注版本小标题
                item {
                    ResourceSectionHeader("译注版本")
                }

                // 已安装译注列表
                items(
                    count = installedEditions.size,
                    key = { index -> "edition-${installedEditions[index].packageId}-${installedEditions[index].versionCode}-${installedEditions[index].locationType}" },
                ) { index ->
                    val item = installedEditions[index]
                    val itemKey = "edition-${item.packageId}-${item.versionCode}-${item.locationType}"
                    val activeEdition = snapshot.activeEdition
                    val isActive = activeEdition != null &&
                        item.packageId == activeEdition.packageId &&
                        item.versionCode == activeEdition.versionCode &&
                        item.locationType == activeEdition.locationType
                    val remoteUpdate = registry?.packages?.find {
                        it.packageId == item.packageId && it.versionCode > item.versionCode
                    }
                    val isLatestInstalled = installedEditions
                        .filter { it.packageId == item.packageId }
                        .maxOfOrNull { it.versionCode } == item.versionCode

                    val subtitle = buildString {
                        append(item.versionName)
                        append(" · ")
                        append(if (isActive) "已启用" else "未启用")
                        if (remoteUpdate != null && isLatestInstalled) {
                            append(" · 可更新至 ")
                            append(remoteUpdate.versionName)
                        }
                    }

                    val githubUrl = item.originUrl.ifBlank {
                        getPackageGithubUrl(item.packageId, registry)
                    }
                    val downloadUrl = getResourceDownloadUrl(item, registry)

                    SwipeableResourceRow(
                        itemKey = itemKey,
                        revealedItemKey = revealedItemKey,
                        onRevealedKeyChange = { revealedItemKey = it },
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = item.name,
                        subtitle = subtitle,
                        isEnabled = isActive,
                        githubUrl = githubUrl,
                        onCopyDownloadUrl = { copyToClipboard(downloadUrl) },
                        onToggleEnabled = {
                            if (!isActive) {
                                onSelectEdition(item)
                            } else {
                                // 若尝试停用当前使用中的译注版本，尝试切换回内置或提示
                                val bundled = installedResources.find {
                                    it.kind == ResourceKind.EDITION && it.locationType == ResourceLocationType.BUNDLED
                                }
                                if (bundled != null && (bundled.packageId != item.packageId || bundled.versionCode != item.versionCode)) {
                                    onSelectEdition(bundled)
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val disableMsg = getDisableMessage(item.name, item.kind)
                                        val result = snackbarHostState.showSnackbar(
                                            message = disableMsg,
                                            actionLabel = "启用",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onSelectEdition(item)
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "当前译注版本正在使用中，至少需保留一个可用译注", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onClick = {
                            // 允许点击产生水波纹反馈，但取消其更改启用/禁用状态的效果
                        },
                        onDelete = if (item.locationType == ResourceLocationType.DOWNLOADED) {
                            {
                                resourcePendingDeletion = item
                                revealedItemKey = null
                            }
                        } else null,
                        trailingAction = if (remoteUpdate != null && isLatestInstalled) {
                            {
                                OutlinedButton(
                                    onClick = { onDownload(remoteUpdate.packageId) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                ) {
                                    Text("更新", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        } else null,
                    )
                }

                // 未安装但可下载的译注版本
                items(
                    count = uninstalledEditions.size,
                    key = { index -> "remote-edition-${uninstalledEditions[index].packageId}" },
                ) { index ->
                    val item = uninstalledEditions[index]
                    val itemKey = "remote-edition-${item.packageId}"
                    val githubUrl = item.sourceRepository.ifBlank {
                        item.releasePageUrl.ifBlank { "https://github.com/ilunyu" }
                    }
                    val downloadUrl = getRemoteResourceDownloadUrl(item)

                    SwipeableResourceRow(
                        itemKey = itemKey,
                        revealedItemKey = revealedItemKey,
                        onRevealedKeyChange = { revealedItemKey = it },
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = item.name,
                        subtitle = "${item.versionName} · ${formatSize(item.size)}",
                        isEnabled = false,
                        enableActionIcon = Icons.Outlined.Download,
                        githubUrl = githubUrl,
                        onCopyDownloadUrl = { copyToClipboard(downloadUrl) },
                        onToggleEnabled = { onDownload(item.packageId) },
                        onClick = {
                            // 允许点击，不改变状态
                        },
                        trailingAction = {
                            OutlinedButton(
                                onClick = { onDownload(item.packageId) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text("下载", fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                        },
                    )
                }

                // 2. 试题库与译注版本之间的全宽分割线
                item {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // 3. 试题库小标题
                item {
                    ResourceSectionHeader("试题库")
                }

                // 已安装试题库列表
                items(
                    count = installedExercises.size,
                    key = { index -> "exercise-${installedExercises[index].packageId}-${installedExercises[index].versionCode}-${installedExercises[index].locationType}" },
                ) { index ->
                    val item = installedExercises[index]
                    val itemKey = "exercise-${item.packageId}-${item.versionCode}-${item.locationType}"
                    val enabled = snapshot.enabledExercisePackages.any { resource ->
                        resource.packageId == item.packageId && resource.versionCode == item.versionCode
                    }
                    val remoteUpdate = registry?.packages?.find {
                        it.packageId == item.packageId && it.versionCode > item.versionCode
                    }
                    val isLatestInstalled = installedExercises
                        .filter { it.packageId == item.packageId }
                        .maxOfOrNull { it.versionCode } == item.versionCode

                    val subtitle = buildString {
                        append(item.versionName)
                        append(" · ")
                        append(if (enabled) "已启用" else "未启用")
                        if (remoteUpdate != null && isLatestInstalled) {
                            append(" · 可更新至 ")
                            append(remoteUpdate.versionName)
                        }
                    }

                    val githubUrl = item.originUrl.ifBlank {
                        getPackageGithubUrl(item.packageId, registry)
                    }
                    val downloadUrl = getResourceDownloadUrl(item, registry)

                    SwipeableResourceRow(
                        itemKey = itemKey,
                        revealedItemKey = revealedItemKey,
                        onRevealedKeyChange = { revealedItemKey = it },
                        icon = Icons.Outlined.School,
                        title = item.name,
                        subtitle = subtitle,
                        isEnabled = enabled,
                        githubUrl = githubUrl,
                        onCopyDownloadUrl = { copyToClipboard(downloadUrl) },
                        onToggleEnabled = {
                            val newEnabled = !enabled
                            onSetExerciseEnabled(item.packageId, newEnabled)
                            if (!newEnabled) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val disableMsg = getDisableMessage(item.name, item.kind)
                                    val result = snackbarHostState.showSnackbar(
                                        message = disableMsg,
                                        actionLabel = "启用",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onSetExerciseEnabled(item.packageId, true)
                                    }
                                }
                            }
                        },
                        onClick = {
                            // 允许点击产生水波纹反馈，但取消其更改启用/禁用状态的效果
                        },
                        onDelete = if (item.locationType == ResourceLocationType.DOWNLOADED) {
                            {
                                resourcePendingDeletion = item
                                revealedItemKey = null
                            }
                        } else null,
                        trailingAction = if (remoteUpdate != null && isLatestInstalled) {
                            {
                                OutlinedButton(
                                    onClick = { onDownload(remoteUpdate.packageId) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                ) {
                                    Text("更新", fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        } else null,
                    )
                }

                // 未安装但可下载的试题库
                items(
                    count = uninstalledExercises.size,
                    key = { index -> "remote-exercise-${uninstalledExercises[index].packageId}" },
                ) { index ->
                    val item = uninstalledExercises[index]
                    val itemKey = "remote-exercise-${item.packageId}"
                    val githubUrl = item.sourceRepository.ifBlank {
                        item.releasePageUrl.ifBlank { "https://github.com/ilunyu" }
                    }
                    val downloadUrl = getRemoteResourceDownloadUrl(item)

                    SwipeableResourceRow(
                        itemKey = itemKey,
                        revealedItemKey = revealedItemKey,
                        onRevealedKeyChange = { revealedItemKey = it },
                        icon = Icons.Outlined.School,
                        title = item.name,
                        subtitle = "${item.versionName} · ${formatSize(item.size)}",
                        isEnabled = false,
                        enableActionIcon = Icons.Outlined.Download,
                        githubUrl = githubUrl,
                        onCopyDownloadUrl = { copyToClipboard(downloadUrl) },
                        onToggleEnabled = { onDownload(item.packageId) },
                        onClick = {
                            // 允许点击，不改变状态
                        },
                        trailingAction = {
                            OutlinedButton(
                                onClick = { onDownload(item.packageId) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text("下载", fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                        },
                    )
                }

                // 底部留白
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("从 URL 添加资源") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "粘贴 GitHub 仓库、Release 页面或 .ilunyupack 直链。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = resourceUrl,
                            onValueChange = { resourceUrl = it },
                            label = { Text("资源 URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = resourceUrl.trim().isNotEmpty(),
                        onClick = {
                            val url = resourceUrl.trim()
                            resourceUrl = ""
                            showAddDialog = false
                            isAddingFromUrl = true
                            onAddUrl(url)
                        },
                    ) { Text("添加") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } },
            )
        }

        resourcePendingDeletion?.let { resource ->
            ResourceDeleteConfirmDialog(
                resourceName = resource.name,
                onConfirm = {
                    onDeleteResource(resource)
                    resourcePendingDeletion = null
                },
                onDismiss = { resourcePendingDeletion = null },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) { data ->
            val actionTextColor = MaterialTheme.colorScheme.inversePrimary
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp),
                action = data.visuals.actionLabel?.let { actionLabel ->
                    {
                        TextButton(
                            onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(contentColor = actionTextColor),
                        ) {
                            Text(
                                text = actionLabel,
                                color = actionTextColor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                },
                dismissAction = {
                    IconButton(
                        onClick = { data.dismiss() },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                },
            ) {
                Text(data.visuals.message)
            }
        }
    }
}

@Composable
private fun ResourceSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * 资源删除确认对话框：与标签删除 Dialog 设计完全对齐
 * - Hero 警告/删除图标 (error tint)
 * - 标题 "删除资源" (加粗)
 * - 主副两段式提示内容
 * - 红色警告删除确认按钮
 */
@Composable
private fun ResourceDeleteConfirmDialog(
    resourceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
        },
        title = {
            Text(
                text = "删除资源",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "确定要删除资源“$resourceName”吗？",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "删除后将清除该资源包的本地文件与安装记录，若需要可重新下载或从 URL 添加。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("删除", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

/**
 * 带有 Swipe to reveal 功能的列表项组件：
 * - 纯 Icon、间距为 8 的 Narrow 胶囊按钮，高度为全高（与行高度一致）
 * - 选项按钮顺序：GitHub 链接、复制下载链接、启用/禁用（或下载）、删除（可选）
 * - 仅允许向左滑动在右侧展开选项，同一时间仅允许一个列表项处于展开状态
 */
@Composable
private fun SwipeableResourceRow(
    itemKey: String,
    revealedItemKey: String?,
    onRevealedKeyChange: (String?) -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    githubUrl: String,
    onCopyDownloadUrl: () -> Unit,
    onToggleEnabled: () -> Unit,
    onClick: () -> Unit,
    trailingAction: @Composable (() -> Unit)? = null,
    enableActionIcon: ImageVector? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isRevealed = (revealedItemKey == itemKey)
    var dragOffset by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val buttonWidth = 44.dp
    val spacing = 2.dp
    val startPadding = 8.dp
    val endPadding = 12.dp
    val buttonCount = if (onDelete != null) 4 else 3
    val revealWidthDp = startPadding + (buttonWidth * buttonCount) + (spacing * (buttonCount - 1)) + endPadding
    val revealWidthPx = with(density) { revealWidthDp.toPx() }
    val context = LocalContext.current

    // 只能向左偏移以在右侧揭示选项，不允许向右滑动露出左侧选项
    val targetOffset = if (isRevealed) {
        -revealWidthPx + dragOffset.coerceIn(0f, revealWidthPx)
    } else {
        dragOffset.coerceIn(-revealWidthPx, 0f)
    }

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipeOffset",
    )

    val draggableState = rememberDraggableState { delta ->
        if (!isRevealed) {
            dragOffset = (dragOffset + delta).coerceIn(-revealWidthPx, 0f)
        } else {
            dragOffset = (dragOffset + delta).coerceIn(0f, revealWidthPx)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        // 1. 背景层：纯 Icon、间距为 2 的 Narrow 胶囊按钮，全高对齐
        Box(
            modifier = Modifier.matchParentSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(revealWidthDp)
                    .align(Alignment.CenterEnd)
                    .padding(start = startPadding, end = endPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val grayColor = MaterialTheme.colorScheme.onSurfaceVariant
                val grayBg = MaterialTheme.colorScheme.surfaceContainerHigh

                // 按钮 1: 复制下载链接按钮（与 GitHub 按钮配色一致，纯 Icon，点击复制下载链接）
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(grayBg)
                        .clickable {
                            onCopyDownloadUrl()
                            onRevealedKeyChange(null)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制下载链接",
                        tint = grayColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // 按钮 2: 指向 GitHub 链接的按钮（采用 OpenInNew 图标，灰阶配色，在浏览器中打开）
                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(grayBg)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                            context.startActivity(intent)
                            onRevealedKeyChange(null)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "GitHub",
                        tint = grayColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // 按钮 3: 启用 / 禁用（或未安装项的下载）
                val defaultActionIcon = if (isEnabled) Icons.Outlined.Block else Icons.Outlined.Check
                val actionIcon = enableActionIcon ?: defaultActionIcon
                val actionDesc = if (enableActionIcon != null) "下载" else if (isEnabled) "禁用" else "启用"

                val actionColor = if (isEnabled) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
                val actionBg = if (isEnabled) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }

                Box(
                    modifier = Modifier
                        .width(buttonWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(actionBg)
                        .clickable {
                            onToggleEnabled()
                            onRevealedKeyChange(null)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDesc,
                        tint = actionColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // 按钮 4: 删除按钮（深浅红色反置，使用 error 实心红底与 onError 亮色图标，更具警示性）
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .width(buttonWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.error)
                            .clickable {
                                onDelete()
                                onRevealedKeyChange(null)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // 2. 前景层：主内容卡片/行（右侧 padding 为 12.dp，与篇阅读列表、试题列表书签按钮对齐）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val threshold = revealWidthPx * 0.35f
                        if (!isRevealed) {
                            if (dragOffset < -threshold || velocity < -300f) {
                                onRevealedKeyChange(itemKey)
                            } else {
                                onRevealedKeyChange(null)
                            }
                        } else {
                            if (dragOffset > threshold || velocity > 300f) {
                                onRevealedKeyChange(null)
                            } else {
                                onRevealedKeyChange(itemKey)
                            }
                        }
                        dragOffset = 0f
                    }
                )
                .clickable {
                    // 若当前存在已展开的项，点击任意列表项主体一律收起
                    if (revealedItemKey != null) {
                        onRevealedKeyChange(null)
                    } else {
                        onClick()
                    }
                }
                .padding(start = 24.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // 尾部区域：更新按钮 (若有更新) 放在竖省略号 icon 之左侧，省略号为 40x40 触摸区
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingAction?.invoke()
                IconButton(
                    onClick = {
                        if (isRevealed) {
                            onRevealedKeyChange(null)
                        } else {
                            onRevealedKeyChange(itemKey)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun getDisableMessage(name: String, kind: String): String {
    val baseName = if (name.endsWith("资源库")) name else "${name}资源库"
    val firstChar = name.firstOrNull()
    val needsSpace = (kind == ResourceKind.EXERCISE) || (firstChar != null && firstChar.isDigit())
    return if (needsSpace) {
        "已禁用 $baseName"
    } else {
        "已禁用$baseName"
    }
}

private fun getPackageGithubUrl(packageId: String, registry: ResourceRegistry?): String {
    val regPkg = registry?.packages?.find { it.packageId == packageId }
    if (regPkg != null) {
        if (regPkg.sourceRepository.isNotBlank()) return regPkg.sourceRepository
        if (regPkg.releasePageUrl.isNotBlank()) return regPkg.releasePageUrl
    }
    return when (packageId) {
        "edition.yangbojun" -> "https://github.com/ilunyu/ilunyu-edition-yangbojun"
        "exercise.bundled" -> "https://github.com/ilunyu/ilunyu-android"
        else -> {
            val slug = packageId.replace('.', '-')
            "https://github.com/ilunyu/ilunyu-$slug"
        }
    }
}

private fun getResourceDownloadUrl(
    item: InstalledResourceEntity,
    registry: ResourceRegistry?,
): String {
    if (item.originUrl.isNotBlank()) return item.originUrl
    val regPkg = registry?.packages?.find { it.packageId == item.packageId }
    if (regPkg != null) {
        val directDownload = regPkg.downloadUrls.firstOrNull { it.isNotBlank() }
        if (directDownload != null) return directDownload
        if (regPkg.releasePageUrl.isNotBlank()) return regPkg.releasePageUrl
        if (regPkg.sourceRepository.isNotBlank()) return regPkg.sourceRepository
    }
    return getPackageGithubUrl(item.packageId, registry)
}

private fun getRemoteResourceDownloadUrl(
    pkg: ResourceRegistryPackage,
): String {
    val directDownload = pkg.downloadUrls.firstOrNull { it.isNotBlank() }
    if (directDownload != null) return directDownload
    if (pkg.releasePageUrl.isNotBlank()) return pkg.releasePageUrl
    if (pkg.sourceRepository.isNotBlank()) return pkg.sourceRepository
    return "https://github.com/ilunyu"
}

private fun checkUpdates(
    registry: ResourceRegistry?,
    installedResources: List<InstalledResourceEntity>,
): List<ResourceRegistryPackage> {
    if (registry == null) return emptyList()
    return registry.packages.filter { regPkg ->
        val currentMax = installedResources
            .filter { it.packageId == regPkg.packageId }
            .maxOfOrNull { it.versionCode }
        currentMax != null && regPkg.versionCode > currentMax
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
}
