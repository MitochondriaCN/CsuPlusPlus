package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.presentation.component.PaginatedLazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseDetailPane(
    detailState: CourseDetailUiState,
    filesState: PagedState<CourseFile>,
    currentUserId: Long?,
    canDeleteAnyFile: Boolean,
    canDeleteCourse: Boolean,
    downloadingFileIds: Set<Long>,
    isUploadingFile: Boolean,
    showBackButton: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onUploadClick: () -> Unit,
    onDeleteCourseClick: (CourseSummary) -> Unit,
    onRetryDetail: () -> Unit,
    onLoadMoreFiles: () -> Unit,
    onRetryFiles: () -> Unit,
    onFileClick: (CourseFile) -> Unit,
    onDeleteFileClick: (CourseFile) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val title = when (detailState) {
        is CourseDetailUiState.Success -> detailState.course.name
        CourseDetailUiState.Loading -> "加载课程中..."
        is CourseDetailUiState.Error -> "加载失败"
        CourseDetailUiState.Empty -> "课程详情"
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
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
                    if (detailState is CourseDetailUiState.Success && canDeleteCourse) {
                        IconButton(onClick = { onDeleteCourseClick(detailState.course) }) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "删除课程",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (detailState is CourseDetailUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isUploadingFile) {
                            onUploadClick()
                        }
                    },
                    icon = {
                        if (isUploadingFile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                        } else {
                            Icon(Lucide.Upload, contentDescription = null)
                        }
                    },
                    text = {
                        Text(if (isUploadingFile) "上传中..." else "上传资料")
                    },
                )
            }
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
                        currentUserId = currentUserId,
                        canDeleteAnyFile = canDeleteAnyFile,
                        downloadingFileIds = downloadingFileIds,
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        onLoadMoreFiles = onLoadMoreFiles,
                        onRetryFiles = onRetryFiles,
                        onFileClick = onFileClick,
                        onDeleteFileClick = onDeleteFileClick,
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
            textAlign = TextAlign.Center,
        )
        Text(
            text = "这里将展示课程基本信息与课件资料列表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseDetailContent(
    course: CourseSummary,
    filesState: PagedState<CourseFile>,
    currentUserId: Long?,
    canDeleteAnyFile: Boolean,
    downloadingFileIds: Set<Long>,
    modifier: Modifier = Modifier,
    onLoadMoreFiles: () -> Unit,
    onRetryFiles: () -> Unit,
    onFileClick: (CourseFile) -> Unit,
    onDeleteFileClick: (CourseFile) -> Unit,
) {
    PaginatedLazyColumn(
        pagedState = filesState,
        onLoadMore = onLoadMoreFiles,
        onRetry = onRetryFiles,
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemKey = { it.id },
        headerContent = {
            item(key = "__course_info_header__") {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CourseTitleText(
                                name = course.name,
                                alias = course.alias,
                                titleStyle = MaterialTheme.typography.headlineSmall,
                                aliasStyle = MaterialTheme.typography.bodyMedium,
                                titleColor = MaterialTheme.colorScheme.onSurface,
                                aliasColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MetadataPill(
                                    text = course.universityName,
                                    icon = Lucide.GraduationCap,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                course.collegeName?.takeIf { it.isNotBlank() }?.let { college ->
                                    MetadataPill(
                                        text = college,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                course.hours?.let { hours ->
                                    MetadataPill(
                                        text = "$hours 学时",
                                        icon = Lucide.Clock,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )

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
                            text = "课程资料",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (filesState.page > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = filesState.totalCount.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
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
            canDelete = canDeleteAnyFile || (currentUserId != null && file.uploaderUserId == currentUserId),
            onClick = { onFileClick(file) },
            onDeleteClick = { onDeleteFileClick(file) },
        )
    }
}
