package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.qingshuige.csuplusplus.core.file.FileSaveResult
import ink.qingshuige.csuplusplus.core.file.PlatformFileSaver
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.core.pagination.Paginator
import ink.qingshuige.csuplusplus.core.pagination.mapItems
import ink.qingshuige.csuplusplus.core.time.formatFriendlyTime
import ink.qingshuige.csuplusplus.data.api.CourseApi
import ink.qingshuige.csuplusplus.data.api.FileApi
import ink.qingshuige.csuplusplus.data.dto.CourseResponse
import ink.qingshuige.csuplusplus.data.dto.FileMetadataResponse
import ink.qingshuige.csuplusplus.data.dto.PagedResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseSummary(
    val id: Long,
    val name: String,
    val universityName: String,
    val collegeName: String?,
    val alias: String?,
    val hours: Int?,
    val updatedAt: String,
)

enum class CourseFileType(val label: String) {
    PDF("PDF"),
    SLIDES("PPT"),
    ARCHIVE("ZIP"),
    CODE("CODE"),
    OTHER("FILE"),
}

data class CourseFile(
    val id: Long,
    val name: String,
    val extension: String,
    val uploaderName: String,
    val type: CourseFileType,
)

sealed interface CourseDetailUiState {
    data object Empty : CourseDetailUiState
    data object Loading : CourseDetailUiState
    data class Error(val message: String) : CourseDetailUiState
    data class Success(val course: CourseSummary) : CourseDetailUiState
}

data class CourseFileUploadUiState(
    val isDialogVisible: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
)

data class HomeUiState(
    val courses: PagedState<CourseSummary> = PagedState(),
    val selectedCourseId: Long? = null,
    val detailState: CourseDetailUiState = CourseDetailUiState.Empty,
    val files: PagedState<CourseFile> = PagedState(),
    val downloadingFileIds: Set<Long> = emptySet(),
    val uploadState: CourseFileUploadUiState = CourseFileUploadUiState(),
)

class HomeViewModel(
    private val courseApi: CourseApi,
    private val fileApi: FileApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var detailJob: Job? = null
    private var activeDetailCourseId: Long? = null

    private val coursePaginator = Paginator(
        scope = viewModelScope,
        onStateChange = { paged ->
            _uiState.update { it.copy(courses = paged) }
        },
        fetchPage = { query ->
            courseApi.list(query).map { resp ->
                resp.mapItems(CourseResponse::toSummary)
            }
        },
        getKey = { it.id.toString() },
    )

    private val filePaginator = Paginator(
        scope = viewModelScope,
        onStateChange = { paged ->
            _uiState.update { it.copy(files = paged) }
        },
        fetchPage = { query ->
            val courseId = activeDetailCourseId
                ?: return@Paginator Result.success(
                    PagedResponse(
                        items = emptyList(),
                        page = 1,
                        pageSize = query.pageSize,
                        totalCount = 0,
                    ),
                )
            fileApi.listCourseFiles(courseId, query).map { resp ->
                resp.mapItems(FileMetadataResponse::toCourseFile)
            }
        },
        getKey = { it.id.toString() },
    )

    init {
        coursePaginator.refresh()
    }

    fun refreshCourses() {
        coursePaginator.refresh()
    }

    fun loadMoreCourses() {
        coursePaginator.loadNextPage()
    }

    fun retryCourses() {
        coursePaginator.retry()
    }

    fun loadMoreFiles() {
        filePaginator.loadNextPage()
    }

    fun retryFiles() {
        filePaginator.retry()
    }

    fun selectCourse(courseId: Long) {
        val currentState = _uiState.value
        if (currentState.selectedCourseId == courseId &&
            currentState.detailState is CourseDetailUiState.Success
        ) {
            return
        }

        val cached = currentState.courses.items.find { it.id == courseId }
        activeDetailCourseId = courseId
        detailJob?.cancel()

        _uiState.update {
            it.copy(
                selectedCourseId = courseId,
                detailState = if (cached != null) {
                    CourseDetailUiState.Success(cached)
                } else {
                    CourseDetailUiState.Loading
                },
            )
        }

        filePaginator.refresh(clearItems = true)

        detailJob = viewModelScope.launch {
            courseApi.getById(courseId).fold(
                onSuccess = { dto ->
                    val summary = dto.toSummary()
                    _uiState.update {
                        it.copy(detailState = CourseDetailUiState.Success(summary))
                    }
                    coursePaginator.updateItems { items ->
                        items.map { if (it.id == summary.id) summary else it }
                    }
                },
                onFailure = { error ->
                    if (cached == null) {
                        _uiState.update {
                            it.copy(detailState = CourseDetailUiState.Error(error.userMessage))
                        }
                    }
                },
            )
        }
    }

    fun retryDetail() {
        val courseId = _uiState.value.selectedCourseId ?: return
        _uiState.update { it.copy(detailState = CourseDetailUiState.Loading) }
        activeDetailCourseId = courseId
        filePaginator.refresh(clearItems = true)
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            courseApi.getById(courseId).fold(
                onSuccess = { dto ->
                    _uiState.update {
                        it.copy(detailState = CourseDetailUiState.Success(dto.toSummary()))
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(detailState = CourseDetailUiState.Error(error.userMessage))
                    }
                },
            )
        }
    }

    fun downloadFile(file: CourseFile, fileSaver: PlatformFileSaver) {
        if (file.id in _uiState.value.downloadingFileIds) return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadingFileIds = it.downloadingFileIds + file.id) }
            try {
                val result = fileSaver.saveWithPicker(
                    suggestedName = file.name,
                    extension = file.extension,
                    fetchFile = { fileApi.downloadCourseFile(file.id) },
                )
                when (result) {
                    is FileSaveResult.Success -> _messages.send(result.message)
                    is FileSaveResult.Failure -> _messages.send(result.message)
                    FileSaveResult.Cancelled -> Unit
                }
            } finally {
                _uiState.update { it.copy(downloadingFileIds = it.downloadingFileIds - file.id) }
            }
        }
    }

    fun openUploadDialog() {
        if (activeDetailCourseId == null) return
        _uiState.update {
            it.copy(
                uploadState = it.uploadState.copy(
                    isDialogVisible = true,
                    errorMessage = null,
                ),
            )
        }
    }

    fun dismissUploadDialog() {
        if (_uiState.value.uploadState.isUploading) return
        _uiState.update {
            it.copy(uploadState = CourseFileUploadUiState())
        }
    }

    fun clearUploadError() {
        if (_uiState.value.uploadState.errorMessage == null) return
        _uiState.update {
            it.copy(uploadState = it.uploadState.copy(errorMessage = null))
        }
    }

    fun uploadCourseFile(
        fileName: String,
        contentType: String?,
        friendlyName: String?,
        readBytes: suspend () -> ByteArray,
    ) {
        val courseId = activeDetailCourseId ?: return
        if (_uiState.value.uploadState.isUploading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    uploadState = it.uploadState.copy(
                        isUploading = true,
                        errorMessage = null,
                    ),
                )
            }

            val bytes = try {
                readBytes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        uploadState = it.uploadState.copy(
                            isUploading = false,
                            errorMessage = "读取文件失败：${e.message ?: "未知错误"}",
                        ),
                    )
                }
                return@launch
            }

            val normalizedFriendlyName = friendlyName?.trim()?.takeIf { it.isNotBlank() }
            fileApi.uploadCourseFile(
                courseId = courseId,
                fileName = fileName,
                fileBytes = bytes,
                friendlyFilename = normalizedFriendlyName,
                contentType = contentType,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(uploadState = CourseFileUploadUiState())
                    }
                    if (activeDetailCourseId == courseId) {
                        filePaginator.refresh()
                    }
                    _messages.send("上传成功")
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            uploadState = it.uploadState.copy(
                                isUploading = false,
                                errorMessage = error.userMessage,
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun CourseResponse.toSummary(): CourseSummary = CourseSummary(
    id = id,
    name = name,
    universityName = universityName,
    collegeName = collegeName,
    alias = alias,
    hours = hours,
    updatedAt = formatFriendlyTime(updatedAt),
)

private fun FileMetadataResponse.toCourseFile(): CourseFile {
    val ext = extension.trim().trimStart('.').lowercase()
    val type = when (ext) {
        "pdf" -> CourseFileType.PDF
        "ppt", "pptx", "key" -> CourseFileType.SLIDES
        "zip", "rar", "7z", "tar", "gz" -> CourseFileType.ARCHIVE
        "kt", "java", "c", "cpp", "h", "hpp", "py", "js", "ts", "rs", "go", "md", "txt" -> CourseFileType.CODE
        else -> CourseFileType.OTHER
    }
    return CourseFile(
        id = id,
        name = displayName,
        extension = ext.uppercase(),
        uploaderName = uploaderUserNickname,
        type = type,
    )
}
