package ink.qingshuige.csuplusplus.presentation.page.me

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.*
import ink.qingshuige.csuplusplus.data.dto.UserProfileDto
import ink.qingshuige.csuplusplus.getPlatform
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MePage(viewModel: MeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val openExternalUrl: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }
            .onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar("无法在浏览器中打开链接")
                }
            }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("我的", style = MaterialTheme.typography.titleLarge) }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                uiState.isInitialLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null && uiState.profile == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        FilledTonalButton(onClick = viewModel::retry) {
                            Text("重试")
                        }
                    }
                }

                uiState.profile != null -> {
                    MeContent(
                        profile = uiState.profile!!,
                        isCheckingUpdate = uiState.isCheckingUpdate,
                        onEditNicknameClick = viewModel::openEditNicknameDialog,
                        onCheckUpdateClick = viewModel::checkForUpdates,
                        onLogoutClick = { showLogoutDialog = true },
                        onGitHubClick = { openExternalUrl(GITHUB_REPO_URL) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }

    uiState.availableUpdate?.let { update ->
        val release = update.release
        val targetUrl = release.releaseUrl.trim()
        val hasValidUrl = targetUrl.isNotEmpty()

        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateDialog,
            icon = { Icon(Lucide.ArrowUpRight, contentDescription = null) },
            title = { Text("发现新版本") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "最新版本：${release.currentBuildName}（Build ${release.currentBuildCode}）\n当前版本：Build ${update.currentBuildCode}",
                    )
                    if (hasValidUrl) {
                        Text(
                            text = "是否前往浏览器打开以下链接获取更新？\n$targetUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "服务端未提供可用的更新链接。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                if (hasValidUrl) {
                    TextButton(
                        onClick = {
                            viewModel.dismissUpdateDialog()
                            openExternalUrl(targetUrl)
                        },
                    ) {
                        Text("前往更新")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdateDialog) {
                    Text(if (hasValidUrl) "稍后再说" else "关闭")
                }
            },
        )
    }

    if (uiState.showEditNicknameDialog && uiState.profile != null) {
        EditNicknameDialog(
            initialNickname = uiState.profile!!.nickname,
            isUpdating = uiState.isUpdatingNickname,
            errorMessage = uiState.editNicknameError,
            onNicknameChange = viewModel::clearEditNicknameError,
            onDismiss = viewModel::dismissEditNicknameDialog,
            onConfirm = viewModel::updateNickname,
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Lucide.LogOut, contentDescription = null) },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？退出后需要重新登录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("退出登录")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun MeContent(
    profile: UserProfileDto,
    isCheckingUpdate: Boolean,
    onEditNicknameClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onGitHubClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 360.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
    ) {
        item(key = "profile") {
            MeProfileCard(
                profile = profile,
                isCheckingUpdate = isCheckingUpdate,
                onEditNicknameClick = onEditNicknameClick,
                onCheckUpdateClick = onCheckUpdateClick,
                onLogoutClick = onLogoutClick,
            )
        }
        item(key = "about") {
            MeAboutCard(onGitHubClick = onGitHubClick)
        }
    }
}

@Composable
private fun MeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun MeProfileCard(
    profile: UserProfileDto,
    isCheckingUpdate: Boolean,
    onEditNicknameClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val platform = remember { getPlatform() }

    MeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile.nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "UID ${profile.id}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "账号信息",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        ProfileInfoRow(
            icon = Lucide.User,
            label = "昵称",
            value = profile.nickname,
            trailingContent = {
                IconButton(
                    onClick = onEditNicknameClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = "修改昵称",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
        HorizontalDivider()
        ProfileInfoRow(icon = Lucide.Mail, label = "邮箱", value = profile.email)
        HorizontalDivider()
        ProfileInfoRow(icon = Lucide.GraduationCap, label = "所属大学", value = profile.universityName)
        HorizontalDivider()
        ProfileInfoRow(icon = Lucide.Building2, label = "所属学院", value = profile.collegeName)
        HorizontalDivider()
        ProfileInfoRow(icon = Lucide.Info, label = "运行平台", value = platform.name)
        HorizontalDivider()
        ProfileInfoRow(
            icon = Lucide.Tag,
            label = "当前版本",
            value = "${platform.buildName} (Build ${platform.buildCode})",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onCheckUpdateClick,
                enabled = !isCheckingUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("检查中...")
                } else {
                    Icon(
                        imageVector = Lucide.RefreshCw,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("检查更新")
                }
            }

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Icon(
                    imageVector = Lucide.LogOut,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }
}

private const val GITHUB_REPO_URL = "https://github.com/MitochondriaCN/CsuPlusPlus"

@Composable
private fun MeAboutCard(
    onGitHubClick: () -> Unit,
) {
    MeCard {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "CSU++是完全公益、开源的学习资料分享平台。我们的初衷是打破大学课程学习资料分享的壁垒，做到人人可上传、可下载，同时提供各平台的高性能客户端，切实为广大学子提供方便。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        ProfileInfoRow(
            icon = Lucide.Code,
            label = "开发者",
            value = "XianlitiCN",
        )

        FilledTonalButton(
            onClick = onGitHubClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = Lucide.Github,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("GitHub")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ArrowUpRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EditNicknameDialog(
    initialNickname: String,
    isUpdating: Boolean,
    errorMessage: String?,
    onNicknameChange: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nickname by remember(initialNickname) { mutableStateOf(initialNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Lucide.Pencil, contentDescription = null) },
        title = { Text("修改昵称") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    onNicknameChange()
                },
                label = { Text("昵称") },
                singleLine = true,
                enabled = !isUpdating,
                isError = errorMessage != null,
                supportingText = {
                    Text(errorMessage ?: "1~20 个字符")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nickname) },
                enabled = !isUpdating,
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
