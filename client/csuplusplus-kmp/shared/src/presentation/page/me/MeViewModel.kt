package ink.qingshuige.csuplusplus.presentation.page.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.qingshuige.csuplusplus.Platform
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.api.AuthApi
import ink.qingshuige.csuplusplus.data.api.ReleaseApi
import ink.qingshuige.csuplusplus.data.dto.ReleaseResponse
import ink.qingshuige.csuplusplus.data.dto.UserProfileDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateAvailableState(
    val release: ReleaseResponse,
    val currentBuildCode: Long,
)

data class MeUiState(
    val profile: UserProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: UpdateAvailableState? = null,
) {
    val isInitialLoading: Boolean
        get() = isLoading && profile == null

    val isRefreshing: Boolean
        get() = isLoading && profile != null
}

class MeViewModel(
    private val authApi: AuthApi,
    private val releaseApi: ReleaseApi,
    private val tokenProvider: TokenProvider,
    private val platform: Platform,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var fetchJob: Job? = null
    private var updateCheckJob: Job? = null

    init {
        viewModelScope.launch {
            tokenProvider.tokenFlow
                .collect { token ->
                    fetchJob?.cancel()
                    updateCheckJob?.cancel()
                    _uiState.value = MeUiState()
                    if (token != null) {
                        loadProfile()
                    }
                }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun retry() {
        loadProfile()
    }

    fun checkForUpdates() {
        if (_uiState.value.isCheckingUpdate) return

        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val currentBuildCode = platform.buildCode
            releaseApi.getRelease().fold(
                onSuccess = { release ->
                    if (release.hasUpdate(currentBuildCode)) {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdate = false,
                                availableUpdate = UpdateAvailableState(
                                    release = release,
                                    currentBuildCode = currentBuildCode,
                                ),
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdate = false,
                                availableUpdate = null,
                            )
                        }
                        _messages.send("当前已是最新版本（Build $currentBuildCode）")
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isCheckingUpdate = false) }
                    _messages.send(error.userMessage)
                },
            )
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    fun logout() {
        fetchJob?.cancel()
        updateCheckJob?.cancel()
        tokenProvider.clearToken()
    }

    private fun loadProfile() {
        if (tokenProvider.getToken() == null || _uiState.value.isLoading) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authApi.getMyProfile().fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    val message = error.userMessage
                    val hasExistingProfile = _uiState.value.profile != null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (hasExistingProfile) null else message,
                        )
                    }
                    if (hasExistingProfile) {
                        _messages.send(message)
                    }
                },
            )
        }
    }
}
