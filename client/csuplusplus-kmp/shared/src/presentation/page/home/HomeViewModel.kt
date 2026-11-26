package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.qingshuige.csuplusplus.core.file.FileSaveResult
import ink.qingshuige.csuplusplus.core.file.PlatformFileSaver
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.core.pagination.PagedState
import ink.qingshuige.csuplusplus.core.pagination.Paginator
import ink.qingshuige.csuplusplus.core.pagination.fetchAllPages
import ink.qingshuige.csuplusplus.core.pagination.mapItems
import ink.qingshuige.csuplusplus.core.session.SessionState
import ink.qingshuige.csuplusplus.core.session.SessionStore
import ink.qingshuige.csuplusplus.core.time.formatFriendlyTime
import ink.qingshuige.csuplusplus.data.api.CollegeApi
import ink.qingshuige.csuplusplus.data.api.CourseApi
import ink.qingshuige.csuplusplus.data.api.FileApi
import ink.qingshuige.csuplusplus.data.api.UniversityApi
import ink.qingshuige.csuplusplus.data.dto.CollegeResponse
import ink.qingshuige.csuplusplus.data.dto.CourseResponse
import ink.qingshuige.csuplusplus.data.dto.FileMetadataResponse
import ink.qingshuige.csuplusplus.data.dto.PagedResponse
import ink.qingshuige.csuplusplus.data.dto.UniversityResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
    MARKDOWN("MD"),
    IMAGE("IMAGE"),
    CODE("CODE"),
    OTHER("FILE"),
}

data class CourseFile(
    val id: Long,
    val name: String,
    val extension: String,
    val uploaderUserId: Long,
    val uploaderName: String,
    val createdAt: String,
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

data class CourseFileDeleteUiState(
    val file: CourseFile? = null,
    val isDeleting: Boolean = false,
)

data class CourseScopeUiState(
    val homeUniversityId: Long? = null,
    val homeUniversityName: String? = null,
    val canSwitchUniversity: Boolean = false,
    val universities: List<UniversityResponse> = emptyList(),
    val isLoadingUniversities: Boolean = false,
    val universitiesError: String? = null,
    val selectedUniversityId: Long? = null,
    val selectedUniversityName: String? = null,
    val colleges: List<CollegeResponse> = emptyList(),
    val isLoadingColleges: Boolean = false,
    val collegesError: String? = null,
    val selectedCollegeId: Long? = null,
    val selectedCollegeName: String? = null,
) {
    val isViewingOtherUniversity: Boolean
        get() = selectedUniversityId != null &&
                homeUniversityId != null &&
                selectedUniversityId != homeUniversityId
}

data class HomeUiState(
    val currentUserId: Long? = null,
    val scope: CourseScopeUiState = CourseScopeUiState(),
    val searchQuery: String = "",
    val activeSearchQuery: String = "",
    val courses: PagedState<CourseSummary> = PagedState(),
    val selectedCourseId: Long? = null,
    val detailState: CourseDetailUiState = CourseDetailUiState.Empty,
    val files: PagedState<CourseFile> = PagedState(),
    val downloadingFileIds: Set<Long> = emptySet(),
    val uploadState: CourseFileUploadUiState = CourseFileUploadUiState(),
    val deleteState: CourseFileDeleteUiState = CourseFileDeleteUiState(),
)

class HomeViewModel(
    private val courseApi: CourseApi,
    private val fileApi: FileApi,
    private val universityApi: UniversityApi,
    private val collegeApi: CollegeApi,
    private val sessionStore: SessionStore,
    private val tokenProvider: TokenProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var detailJob: Job? = null
    private var collegeJob: Job? = null
    private var universityJob: Job? = null
    private var searchDebounceJob: Job? = null
    private var activeDetailCourseId: Long? = null
    private var loadedCollegesUniversityId: Long? = null
    private var hasLoadedUniversities: Boolean = false

    private val coursePaginator = Paginator(
        scope = viewModelScope,
        onStateChange = { paged ->
            _uiState.update { it.copy(courses = paged) }
        },
        fetchPage = { query ->
            val state = _uiState.value
            if (state.scope.isViewingOtherUniversity) {
                Result.success(
                    PagedResponse(
                        items = emptyList(),
                        page = 1,
                        pageSize = query.pageSize,
                        totalCount = 0,
                    ),
                )
            } else {
                val keyword = state.activeSearchQuery
                val result = if (keyword.isBlank()) {
                    courseApi.list(collegeId = state.scope.selectedCollegeId, query = query)
                } else {
                    courseApi.search(keyword = keyword, query = query)
                }
                result.map { resp ->
                    resp.mapItems(CourseResponse::toSummary)
                }
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
        viewModelScope.launch {
            tokenProvider.tokenFlow.collect { token ->
                detailJob?.cancel()
                collegeJob?.cancel()
                universityJob?.cancel()
                searchDebounceJob?.cancel()
                activeDetailCourseId = null
                loadedCollegesUniversityId = null
                hasLoadedUniversities = false
                _uiState.value = HomeUiState()
                if (token != null) {
                    syncSession(sessionStore.state.value)
                    coursePaginator.refresh(clearItems = true)
                }
            }
        }
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                if (tokenProvider.getToken() != null) {
                    syncSession(session)
                }
            }
        }
    }

    fun selectCollege(collegeId: Long?) {
        val current = _uiState.value
        if (current.scope.selectedUniversityId == null) return
        if (current.scope.selectedCollegeId == collegeId &&
            current.activeSearchQuery.isBlank() &&
            current.courses.errorMessage == null
        ) {
            return
        }

        searchDebounceJob?.cancel()
        val collegeName = collegeId?.let { id ->
            current.scope.colleges.find { it.id == id }?.name
        }
        _uiState.update {
            it.copy(
                searchQuery = "",
                activeSearchQuery = "",
                scope = it.scope.copy(
                    selectedCollegeId = collegeId,
                    selectedCollegeName = collegeName,
                ),
            )
        }
        if (!current.scope.isViewingOtherUniversity) {
            coursePaginator.refresh(clearItems = true)
        }
    }

    fun selectUniversity(universityId: Long) {
        val current = _uiState.value
        if (!current.scope.canSwitchUniversity) return
        if (current.scope.selectedUniversityId == universityId) return

        val targetName = current.scope.universities.find { it.id == universityId }?.name
            ?: current.scope.homeUniversityName?.takeIf { current.scope.homeUniversityId == universityId }

        applyUniversitySwitch(universityId = universityId, universityName = targetName)
    }

    fun switchBackToHomeUniversity() {
        val homeId = _uiState.value.scope.homeUniversityId ?: return
        val homeName = _uiState.value.scope.homeUniversityName
        if (_uiState.value.scope.selectedUniversityId == homeId) return
        applyUniversitySwitch(universityId = homeId, universityName = homeName)
    }

    fun retryUniversities() {
        if (!_uiState.value.scope.canSwitchUniversity) return
        hasLoadedUniversities = false
        loadUniversities()
    }

    fun retryColleges() {
        val universityId = _uiState.value.scope.selectedUniversityId ?: return
        loadedCollegesUniversityId = null
        loadColleges(universityId)
    }

    fun onSearchQueryChange(query: String) {
        if (_uiState.value.scope.isViewingOtherUniversity) return
        _uiState.update { it.copy(searchQuery = query) }
        val normalized = query.trim()
        searchDebounceJob?.cancel()
        if (normalized == _uiState.value.activeSearchQuery) {
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(300L.milliseconds)
            executeSearch(normalized)
        }
    }

    fun submitSearch() {
        if (_uiState.value.scope.isViewingOtherUniversity) return
        searchDebounceJob?.cancel()
        val normalized = _uiState.value.searchQuery.trim()
        if (normalized != _uiState.value.activeSearchQuery || _uiState.value.courses.errorMessage != null) {
            executeSearch(normalized)
        }
    }

    fun clearSearch() {
        searchDebounceJob?.cancel()
        val hadActiveQuery = _uiState.value.activeSearchQuery.isNotEmpty()
        _uiState.update { it.copy(searchQuery = "", activeSearchQuery = "") }
        if (hadActiveQuery && !_uiState.value.scope.isViewingOtherUniversity) {
            coursePaginator.refresh(clearItems = true)
        }
    }

    private fun executeSearch(normalizedQuery: String) {
        if (_uiState.value.scope.isViewingOtherUniversity) return
        if (_uiState.value.activeSearchQuery == normalizedQuery &&
            _uiState.value.courses.errorMessage == null &&
            _uiState.value.courses.page > 0
        ) {
            return
        }
        _uiState.update { it.copy(activeSearchQuery = normalizedQuery) }
        coursePaginator.refresh(clearItems = true)
    }

    fun refreshCourses() {
        val scope = _uiState.value.scope
        if (scope.selectedUniversityId != null && scope.collegesError != null) {
            retryColleges()
        }
        if (scope.isViewingOtherUniversity) {
            scope.selectedUniversityId?.let {
                loadedCollegesUniversityId = null
                loadColleges(it)
            }
            return
        }
        coursePaginator.refresh()
    }

    fun loadMoreCourses() {
        if (_uiState.value.scope.isViewingOtherUniversity) return
        coursePaginator.loadNextPage()
    }

    fun retryCourses() {
        if (_uiState.value.scope.isViewingOtherUniversity) return
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
        ensureSessionLoaded()
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
        ensureSessionLoaded()
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
        val currentState = _uiState.value
        val isDeletingThisFile =
            currentState.deleteState.isDeleting && currentState.deleteState.file?.id == file.id
        if (file.id in currentState.downloadingFileIds || isDeletingThisFile) return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadingFileIds = it.downloadingFileIds + file.id) }
            try {
                val download = fileApi.getCourseDownloadUrl(file.id).getOrElse { error ->
                    _messages.send("下载失败：${error.userMessage}")
                    return@launch
                }
                val result = fileSaver.saveFromUrl(
                    url = download.url,
                    suggestedName = download.downloadName.ifBlank { file.name },
                    extension = file.extension,
                    fetchBytes = fileApi::downloadBytes,
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

    fun requestDeleteFile(file: CourseFile) {
        val currentState = _uiState.value
        if (currentState.deleteState.isDeleting || file.id in currentState.downloadingFileIds) return
        if (currentState.currentUserId != file.uploaderUserId) return
        _uiState.update { it.copy(deleteState = CourseFileDeleteUiState(file = file)) }
    }

    fun dismissDeleteDialog() {
        val deleteState = _uiState.value.deleteState
        if (deleteState.file == null || deleteState.isDeleting) return
        _uiState.update { it.copy(deleteState = CourseFileDeleteUiState()) }
    }

    fun confirmDeleteFile() {
        val currentState = _uiState.value
        val file = currentState.deleteState.file ?: return
        if (currentState.deleteState.isDeleting || file.id in currentState.downloadingFileIds) return

        val courseId = activeDetailCourseId
        viewModelScope.launch {
            _uiState.update { it.copy(deleteState = it.deleteState.copy(isDeleting = true)) }
            try {
                fileApi.deleteFile(file.id).fold(
                    onSuccess = {
                        if (activeDetailCourseId == courseId) {
                            filePaginator.removeItems { it.id == file.id }
                        }
                        _messages.send("删除成功")
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        _messages.send(error.userMessage)
                    },
                )
            } finally {
                _uiState.update { it.copy(deleteState = CourseFileDeleteUiState()) }
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
                onSuccess = { uploaded ->
                    _uiState.update {
                        it.copy(
                            currentUserId = it.currentUserId ?: uploaded.uploaderUserId,
                            uploadState = CourseFileUploadUiState(),
                        )
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

    private fun syncSession(session: SessionState) {
        val profile = session.profile
        val canSwitch = session.capabilities.platformAdmin
        var shouldSnapBackToHome = false

        _uiState.update { state ->
            val currentScope = state.scope
            val homeId = profile?.universityId ?: currentScope.homeUniversityId
            val homeName = profile?.universityName ?: currentScope.homeUniversityName

            val nextSelectedUniversityId = when {
                !canSwitch && homeId != null -> homeId
                currentScope.selectedUniversityId != null -> currentScope.selectedUniversityId
                else -> homeId
            }
            val nextSelectedUniversityName = when {
                nextSelectedUniversityId == null -> null
                nextSelectedUniversityId == homeId -> homeName
                else -> currentScope.universities.find { it.id == nextSelectedUniversityId }?.name
                    ?: currentScope.selectedUniversityName
            }

            shouldSnapBackToHome = currentScope.selectedUniversityId != null &&
                    nextSelectedUniversityId != currentScope.selectedUniversityId

            state.copy(
                currentUserId = profile?.id ?: state.currentUserId,
                scope = currentScope.copy(
                    homeUniversityId = homeId,
                    homeUniversityName = homeName,
                    canSwitchUniversity = canSwitch,
                    selectedUniversityId = nextSelectedUniversityId,
                    selectedUniversityName = nextSelectedUniversityName,
                    colleges = if (shouldSnapBackToHome) emptyList() else currentScope.colleges,
                    collegesError = if (shouldSnapBackToHome) null else currentScope.collegesError,
                    selectedCollegeId = if (shouldSnapBackToHome) null else currentScope.selectedCollegeId,
                    selectedCollegeName = if (shouldSnapBackToHome) null else currentScope.selectedCollegeName,
                ),
            )
        }

        if (shouldSnapBackToHome) {
            loadedCollegesUniversityId = null
            coursePaginator.refresh(clearItems = true)
        }

        val targetUniversityId = _uiState.value.scope.selectedUniversityId
        if (targetUniversityId != null &&
            loadedCollegesUniversityId != targetUniversityId &&
            collegeJob?.isActive != true
        ) {
            loadColleges(targetUniversityId)
        }

        if (canSwitch && !hasLoadedUniversities && universityJob?.isActive != true) {
            loadUniversities()
        }
    }

    private fun applyUniversitySwitch(universityId: Long, universityName: String?) {
        searchDebounceJob?.cancel()
        collegeJob?.cancel()
        detailJob?.cancel()
        activeDetailCourseId = null
        loadedCollegesUniversityId = null

        _uiState.update {
            it.copy(
                searchQuery = "",
                activeSearchQuery = "",
                selectedCourseId = null,
                detailState = CourseDetailUiState.Empty,
                files = PagedState(),
                scope = it.scope.copy(
                    selectedUniversityId = universityId,
                    selectedUniversityName = universityName,
                    colleges = emptyList(),
                    isLoadingColleges = false,
                    collegesError = null,
                    selectedCollegeId = null,
                    selectedCollegeName = null,
                ),
            )
        }

        loadColleges(universityId)

        if (_uiState.value.scope.isViewingOtherUniversity) {
            coursePaginator.clear()
        } else {
            coursePaginator.refresh(clearItems = true)
        }
    }

    private fun loadUniversities() {
        universityJob?.cancel()
        universityJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scope = it.scope.copy(
                        isLoadingUniversities = true,
                        universitiesError = null,
                    ),
                )
            }

            fetchAllPages { query -> universityApi.list(query) }.fold(
                onSuccess = { list ->
                    hasLoadedUniversities = true
                    _uiState.update { state ->
                        val selectedId = state.scope.selectedUniversityId
                        val resolvedName = selectedId?.let { id ->
                            list.find { it.id == id }?.name ?: state.scope.selectedUniversityName
                        }
                        state.copy(
                            scope = state.scope.copy(
                                universities = list,
                                isLoadingUniversities = false,
                                universitiesError = null,
                                selectedUniversityName = resolvedName,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            scope = it.scope.copy(
                                isLoadingUniversities = false,
                                universitiesError = error.userMessage,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun loadColleges(universityId: Long) {
        collegeJob?.cancel()
        collegeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scope = it.scope.copy(
                        isLoadingColleges = true,
                        collegesError = null,
                    ),
                )
            }

            fetchAllPages { query -> collegeApi.list(universityId, query) }.fold(
                onSuccess = { list ->
                    if (_uiState.value.scope.selectedUniversityId != universityId) return@fold
                    loadedCollegesUniversityId = universityId
                    _uiState.update { state ->
                        val selectedCollegeId = state.scope.selectedCollegeId
                        val resolvedCollege = selectedCollegeId?.let { id ->
                            list.find { it.id == id }
                        }
                        state.copy(
                            scope = state.scope.copy(
                                colleges = list,
                                isLoadingColleges = false,
                                collegesError = null,
                                selectedCollegeId = resolvedCollege?.id,
                                selectedCollegeName = resolvedCollege?.name,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    if (_uiState.value.scope.selectedUniversityId != universityId) return@fold
                    _uiState.update {
                        it.copy(
                            scope = it.scope.copy(
                                isLoadingColleges = false,
                                collegesError = error.userMessage,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun ensureSessionLoaded() {
        val session = sessionStore.state.value
        if (session.profile == null && !session.isLoading && tokenProvider.getToken() != null) {
            viewModelScope.launch {
                sessionStore.refresh()
            }
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

internal fun FileMetadataResponse.toCourseFile(): CourseFile =
    toCourseFile(::formatFriendlyTime)

internal fun FileMetadataResponse.toCourseFile(
    formatTime: (String) -> String,
): CourseFile {
    val ext = extension.trim().trimStart('.').lowercase()
    val type = when (ext) {
        "pdf" -> CourseFileType.PDF
        "ppt", "pptx", "key" -> CourseFileType.SLIDES
        "zip", "rar", "7z", "tar", "gz" -> CourseFileType.ARCHIVE
        "md", "markdown" -> CourseFileType.MARKDOWN
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg" -> CourseFileType.IMAGE
        "kt", "java", "c", "cpp", "h", "hpp", "py", "js", "ts", "rs", "go", "txt" -> CourseFileType.CODE
        else -> CourseFileType.OTHER
    }
    return CourseFile(
        id = id,
        name = displayName,
        extension = ext.uppercase(),
        uploaderUserId = uploaderUserId,
        uploaderName = uploaderUserNickname,
        createdAt = formatTime(createdAt),
        type = type,
    )
}
