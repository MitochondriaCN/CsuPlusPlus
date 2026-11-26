package ink.qingshuige.csuplusplus.presentation.page.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.qingshuige.csuplusplus.Platform
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.core.session.SessionStore
import ink.qingshuige.csuplusplus.data.api.ReleaseApi
import ink.qingshuige.csuplusplus.data.dto.ReleaseResponse
import ink.qingshuige.csuplusplus.data.dto.UserProfileDto
import kotlinx.coroutines.CancellationException
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
    private val sessionStore: SessionStore,
    private val releaseApi: ReleaseApi,
    private val tokenProvider: TokenProvider,
    private val platform: Platform,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var refreshJob: Job? = null
    private var updateCheckJob: Job? = null

    init {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                _uiState.update { current ->
                    current.copy(
                        profile = session.profile,
                        isLoading = session.isLoading,
                        errorMessage = session.errorMessage,
                    )
                }
            }
        }
        viewModelScope.launch {
            tokenProvider.tokenFlow.collect { token ->
                if (token == null) {
                    refreshJob?.cancel()
                    updateCheckJob?.cancel()
                    _uiState.value = MeUiState()
                }
            }
        }
    }

    fun refreshProfile() {
        reloadSession()
    }

    fun retry() {
        reloadSession()
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
        refreshJob?.cancel()
        updateCheckJob?.cancel()
        tokenProvider.clearToken()
    }

    private fun reloadSession() {
        if (tokenProvider.getToken() == null || _uiState.value.isLoading) return

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hadProfile = _uiState.value.profile != null
            sessionStore.refresh().onFailure { error ->
                if (error is CancellationException) throw error
                if (hadProfile) {
                    _messages.send(error.userMessage)
                }
            }
        }
    }
}
