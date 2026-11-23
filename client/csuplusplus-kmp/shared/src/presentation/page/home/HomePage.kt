package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.*
import ink.qingshuige.csuplusplus.core.file.rememberPlatformFileSaver
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.presentation.component.PaginatedLazyColumn
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.math.round
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomePage(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 禁用高度 >= 900dp 时的上下双分栏
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
        .copy(
            maxVerticalPartitions = 1,
            verticalPartitionSpacerSize = 0.dp,
        )

    val navigator = rememberListDetailPaneScaffoldNavigator<Long>(
        scaffoldDirective = scaffoldDirective,
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileSaver = rememberPlatformFileSaver()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isDualPane =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                CourseListPane(
                    coursesState = uiState.courses,
                    // 仅在双栏同时可见时高亮选中卡片
                    highlightedCourseId = if (isDualPane) uiState.selectedCourseId else null,
                    onRefresh = viewModel::refreshCourses,
                    onLoadMore = viewModel::loadMoreCourses,
                    onRetry = viewModel::retryCourses,
                    onCourseClick = { course ->
                        viewModel.selectCourse(course.id)
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, course.id)
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                CourseDetailPane(
                    detailState = uiState.detailState,
                    filesState = uiState.files,
                    downloadingFileIds = uiState.downloadingFileIds,
                    isUploadingFile = uiState.uploadState.isUploading,
                    showBackButton = !isDualPane,
                    snackbarHostState = snackbarHostState,
                    onBackClick = {
                        scope.launch { navigator.navigateBack() }
                    },
                    onUploadClick = viewModel::openUploadDialog,
                    onRetryDetail = viewModel::retryDetail,
                    onLoadMoreFiles = viewModel::loadMoreFiles,
                    onRetryFiles = viewModel::retryFiles,
                    onFileClick = { file ->
                        viewModel.downloadFile(file, fileSaver)
                    },
                )
            }
        },
    )

    if (uiState.uploadState.isDialogVisible) {
        UploadCourseFileDialog(
            uploadState = uiState.uploadState,
            onClearError = viewModel::clearUploadError,
            onDismiss = viewModel::dismissUploadDialog,
            onUpload = { file, friendlyName ->
                viewModel.uploadCourseFile(
                    fileName = file.name,
                    contentType = file.mimeType,
                    friendlyName = friendlyName,
                    readBytes = { file.platformFile.readBytes() },
                )
            },
        )
    }
}

@Composable
private fun CourseListPane(
    coursesState: PagedState<CourseSummary>,
    highlightedCourseId: Long?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onCourseClick: (CourseSummary) -> Unit,
) {
    val subtitle = when {
        coursesState.isFirstLoading -> "加载课程中..."
        coursesState.page > 0 -> "共 ${coursesState.totalCount} 门课程"
        else -> "课程列表"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("课程列表", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Lucide.RefreshCw, contentDescription = "刷新课程")
                    }
                },
            )
        },
    ) { innerPadding ->
        PaginatedLazyColumn(
            pagedState = coursesState,
            onLoadMore = onLoadMore,
            onRetry = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemKey = { it.id },
            emptyContent = {
                Text(
                    text = "暂无课程",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        ) { course ->
            CourseCard(
                course = course,
                isSelected = course.id == highlightedCourseId,
                onClick = { onCourseClick(course) },
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: CourseSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val border = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Text(
                        text = course.collegeName ?: course.universityName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                course.hours?.let { hours ->
                    Text(
                        text = "$hours 学时",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Lucide.BookOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = buildString {
                        append(course.universityName)
                        if (!course.alias.isNullOrBlank()) {
                            append(" · ")
                            append(course.alias)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailPane(
    detailState: CourseDetailUiState,
    filesState: PagedState<CourseFile>,
    downloadingFileIds: Set<Long>,
    isUploadingFile: Boolean,
    showBackButton: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRetryDetail: () -> Unit,
    onLoadMoreFiles: () -> Unit,
    onRetryFiles: () -> Unit,
    onFileClick: (CourseFile) -> Unit,
) {
    val title = when (detailState) {
        is CourseDetailUiState.Success -> detailState.course.name
        CourseDetailUiState.Loading -> "加载课程中..."
        is CourseDetailUiState.Error -> "加载失败"
        CourseDetailUiState.Empty -> "课程详情"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    // 仅在窄屏（单栏切换）时显示返回键；宽屏双栏时隐藏，避免语义混乱
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Icon(Lucide.ArrowLeft, contentDescription = "返回课程列表")
                        }
                    }
                },
                actions = {
                    if (detailState is CourseDetailUiState.Success) {
                        IconButton(
                            onClick = onUploadClick,
                            enabled = !isUploadingFile,
                        ) {
                            Icon(Lucide.Upload, contentDescription = "上传文件")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (detailState) {
                CourseDetailUiState.Empty -> {
                    EmptyDetailPlaceholder(modifier = Modifier.align(Alignment.Center))
                }

                CourseDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is CourseDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = detailState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        FilledTonalButton(onClick = onRetryDetail) {
                            Text("重试")
                        }
                    }
                }

                is CourseDetailUiState.Success -> {
                    CourseDetailContent(
                        course = detailState.course,
                        filesState = filesState,
                        downloadingFileIds = downloadingFileIds,
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        onLoadMoreFiles = onLoadMoreFiles,
                        onRetryFiles = onRetryFiles,
                        onFileClick = onFileClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Lucide.BookOpen,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Text(
            text = "从左侧选择一门课程",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "这里将展示课程基本信息与课件资料列表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CourseDetailContent(
    course: CourseSummary,
    filesState: PagedState<CourseFile>,
    downloadingFileIds: Set<Long>,
    modifier: Modifier = Modifier,
    onLoadMoreFiles: () -> Unit,
    onRetryFiles: () -> Unit,
    onFileClick: (CourseFile) -> Unit,
) {
    val fileCountLabel = if (filesState.page > 0) " (${filesState.totalCount})" else ""

    PaginatedLazyColumn(
        pagedState = filesState,
        onLoadMore = onLoadMoreFiles,
        onRetry = onRetryFiles,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemKey = { it.id },
        headerContent = {
            item(key = "__course_info_header__") {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AssistChip(onClick = {}, label = { Text(course.universityName) })
                                course.collegeName?.let { college ->
                                    AssistChip(onClick = {}, label = { Text(college) })
                                }
                                course.hours?.let { hours ->
                                    AssistChip(onClick = {}, label = { Text("$hours 学时") })
                                }
                            }

                            HorizontalDivider()

                            if (!course.alias.isNullOrBlank()) {
                                InfoRow(icon = Lucide.BookOpen, label = "课程别名", value = course.alias)
                            }
                            InfoRow(icon = Lucide.Clock, label = "最近更新", value = course.updatedAt)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "课程资料$fileCountLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        emptyContent = {
            Text(
                text = "该课程暂无资料",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { file ->
        CourseFileItem(
            file = file,
            isDownloading = file.id in downloadingFileIds,
            onClick = { onFileClick(file) },
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CourseFileItem(
    file: CourseFile,
    isDownloading: Boolean,
    onClick: () -> Unit,
) {
    val fileIcon = when (file.type) {
        CourseFileType.PDF -> Lucide.FileText
        CourseFileType.SLIDES -> Lucide.Presentation
        CourseFileType.ARCHIVE -> Lucide.FileArchive
        CourseFileType.CODE -> Lucide.FileCode
        CourseFileType.OTHER -> Lucide.FileText
    }
    val typeLabel = if (file.type == CourseFileType.OTHER && file.extension.isNotBlank()) {
        file.extension
    } else {
        file.type.label
    }

    Card(
        onClick = onClick,
        enabled = !isDownloading,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$typeLabel · 上传者 ${file.uploaderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Lucide.Download,
                    contentDescription = "下载",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class PickedUploadFile(
    val platformFile: PlatformFile,
    val name: String,
    val sizeText: String?,
    val mimeType: String?,
)

@Composable
private fun UploadCourseFileDialog(
    uploadState: CourseFileUploadUiState,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onUpload: (PickedUploadFile, String) -> Unit,
) {
    var selectedFile by remember { mutableStateOf<PickedUploadFile?>(null) }
    var friendlyName by remember { mutableStateOf("") }
    var hasEditedFriendlyName by remember { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberFilePickerLauncher(
        onError = { error ->
            pickerError = "无法打开文件选择器：${error.message ?: "未知错误"}"
        },
    ) { file ->
        if (file != null) {
            val fileName = file.name
            val defaultFriendlyName = file.nameWithoutExtension.ifBlank { fileName }
            val sizeText = runCatching { file.size() }
                .getOrNull()
                ?.takeIf { it >= 0L }
                ?.let(::formatFileSize)
            val mimeType = runCatching { file.mimeType()?.toString() }.getOrNull()

            selectedFile = PickedUploadFile(
                platformFile = file,
                name = fileName,
                sizeText = sizeText,
                mimeType = mimeType,
            )
            if (!hasEditedFriendlyName || friendlyName.isBlank()) {
                friendlyName = defaultFriendlyName
            }
            pickerError = null
            onClearError()
        }
    }

    val isUploading = uploadState.isUploading
    val displayError = pickerError ?: uploadState.errorMessage

    AlertDialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        title = { Text("上传文件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(
                    onClick = { filePickerLauncher.launch() },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (selectedFile == null) Lucide.Upload else Lucide.FileText,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = selectedFile?.name ?: "选择文件",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = selectedFile?.sizeText ?: "点击从本地选取要上传的文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selectedFile != null) {
                            Text(
                                text = "重选",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = friendlyName,
                    onValueChange = {
                        friendlyName = it
                        hasEditedFriendlyName = true
                        if (uploadState.errorMessage != null) {
                            onClearError()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading,
                    label = { Text("显示名称") },
                    placeholder = { Text("留空则使用默认名称") },
                    singleLine = true,
                )

                if (displayError != null) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFile?.let { file ->
                        onUpload(file, friendlyName)
                    }
                },
                enabled = selectedFile != null && !isUploading,
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传中...")
                } else {
                    Text("上传")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading,
            ) {
                Text("取消")
            }
        },
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${formatOneDecimal(kb)} KB"
    val mb = kb / 1024.0
    return "${formatOneDecimal(mb)} MB"
}

private fun formatOneDecimal(value: Double): String {
    val roundedTenths = round(value * 10.0).toLong()
    val whole = roundedTenths / 10L
    val frac = roundedTenths % 10L
    return if (frac == 0L) "$whole" else "$whole.$frac"
}

