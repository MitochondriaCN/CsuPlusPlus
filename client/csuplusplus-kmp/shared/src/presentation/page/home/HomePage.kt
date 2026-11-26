package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.qingshuige.csuplusplus.core.file.rememberPlatformFileSaver
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = koinViewModel(),
    universityEmblem: Any? = null,
) {
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
                    scope = uiState.scope,
                    fallbackUniversityName = uiState.courses.items.firstOrNull()?.universityName,
                    universityEmblem = universityEmblem,
                    searchQuery = uiState.searchQuery,
                    activeSearchQuery = uiState.activeSearchQuery,
                    coursesState = uiState.courses,
                    // 仅在双栏同时可见时高亮选中卡片
                    highlightedCourseId = if (isDualPane) uiState.selectedCourseId else null,
                    onSelectUniversity = viewModel::selectUniversity,
                    onSelectCollege = viewModel::selectCollege,
                    onRetryUniversities = viewModel::retryUniversities,
                    onRetryColleges = viewModel::retryColleges,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchSubmit = viewModel::submitSearch,
                    onClearSearch = viewModel::clearSearch,
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
                    currentUserId = uiState.currentUserId,
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
                    onDeleteFileClick = viewModel::requestDeleteFile,
                )
            }
        },
    )

    if (uiState.uploadState.isDialogVisible) {
        UploadCourseFileDialog(
            uploadState = uiState.uploadState,
            onClearError = viewModel::clearUploadError,
            onDismiss = viewModel::dismissUploadDialog,
            onUpload = { fileName, mimeType, friendlyName, readBytes ->
                viewModel.uploadCourseFile(
                    fileName = fileName,
                    contentType = mimeType,
                    friendlyName = friendlyName,
                    readBytes = readBytes,
                )
            },
        )
    }

    uiState.deleteState.file?.let { file ->
        DeleteCourseFileDialog(
            file = file,
            isDeleting = uiState.deleteState.isDeleting,
            onConfirm = viewModel::confirmDeleteFile,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }
}
