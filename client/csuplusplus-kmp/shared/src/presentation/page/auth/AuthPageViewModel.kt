package ink.qingshuige.csuplusplus.presentation.page.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.api.AuthApi
import ink.qingshuige.csuplusplus.data.api.CollegeApi
import ink.qingshuige.csuplusplus.data.api.UniversityApi
import ink.qingshuige.csuplusplus.data.dto.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthPageViewModel(
    private val authApi: AuthApi,
    private val universityApi: UniversityApi,
    private val collegeApi: CollegeApi,
    private val tokenProvider: TokenProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthPageUiState>(AuthPageUiState.LoggingIn())
    val uiState = _uiState.asStateFlow()

    private var universityJob: Job? = null
    private var collegeJob: Job? = null

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        viewModelScope.launch {
            _uiState.update { AuthPageUiState.LoggingIn(isLoading = true) }

            authApi.login(LoginRequest(trimmedEmail, password)).fold(
                onSuccess = { resp ->
                    tokenProvider.updateToken(resp.token)
                    _uiState.update { AuthPageUiState.Success }
                },
                onFailure = { e ->
                    val message = if (e is ApiException && e.statusCode == 401 && e.problem?.displayMessage == null) {
                        "登录失败，请检查邮箱或密码"
                    } else {
                        e.userMessage
                    }
                    _uiState.update { AuthPageUiState.LoggingIn(isLoading = false, errorMessage = message) }
                },
            )
        }
    }

    fun signUp(
        email: String,
        password: String,
        nickname: String,
        collegeId: Long,
    ) {
        val trimmedEmail = email.trim()
        val trimmedNickname = nickname.trim()
        viewModelScope.launch {
            updateSigningUp { copy(isLoading = true, errorMessage = null) }

            authApi.register(
                RegisterRequest(
                    email = trimmedEmail,
                    password = password,
                    nickname = trimmedNickname,
                    collegeId = collegeId,
                ),
            ).fold(
                onSuccess = {
                    authApi.login(LoginRequest(trimmedEmail, password)).fold(
                        onSuccess = { resp ->
                            tokenProvider.updateToken(resp.token)
                            _uiState.update { AuthPageUiState.Success }
                        },
                        onFailure = {
                            _uiState.update {
                                AuthPageUiState.LoggingIn(errorMessage = "注册成功，请使用新账号登录")
                            }
                        },
                    )
                },
                onFailure = { e ->
                    updateSigningUp {
                        copy(isLoading = false, errorMessage = e.userMessage)
                    }
                },
            )
        }
    }

    fun switchToSignUp() {
        universityJob?.cancel()
        collegeJob?.cancel()
        _uiState.update { AuthPageUiState.SigningUp() }
        loadUniversities()
    }

    fun switchToLogin() {
        universityJob?.cancel()
        collegeJob?.cancel()
        _uiState.update { AuthPageUiState.LoggingIn() }
    }

    fun loadUniversities() {
        universityJob?.cancel()
        universityJob = viewModelScope.launch {
            updateSigningUp {
                copy(
                    isLoadingUniversities = true,
                    errorMessage = null,
                )
            }

            universityApi.list(PageQuery(page = 1, pageSize = 100)).fold(
                onSuccess = { resp ->
                    val list = resp.items
                    val autoSelectedUniversityId = if (list.size == 1) list.first().id else null
                    updateSigningUp {
                        copy(
                            universities = list,
                            isLoadingUniversities = false,
                            selectedUniversityId = autoSelectedUniversityId,
                        )
                    }
                    if (autoSelectedUniversityId != null) {
                        loadColleges(autoSelectedUniversityId)
                    }
                },
                onFailure = { e ->
                    updateSigningUp {
                        copy(
                            isLoadingUniversities = false,
                            errorMessage = e.userMessage,
                        )
                    }
                },
            )
        }
    }

    fun selectUniversity(universityId: Long) {
        val current = _uiState.value as? AuthPageUiState.SigningUp ?: return
        if (current.selectedUniversityId == universityId && current.colleges.isNotEmpty()) {
            return
        }
        updateSigningUp {
            copy(
                selectedUniversityId = universityId,
                selectedCollegeId = null,
                colleges = emptyList(),
                errorMessage = null,
            )
        }
        loadColleges(universityId)
    }

    fun selectCollege(collegeId: Long) {
        updateSigningUp {
            copy(
                selectedCollegeId = collegeId,
                errorMessage = null,
            )
        }
    }

    fun resetToLogin() {
        universityJob?.cancel()
        collegeJob?.cancel()
        _uiState.update { AuthPageUiState.LoggingIn() }
    }

    private fun loadColleges(universityId: Long) {
        collegeJob?.cancel()
        collegeJob = viewModelScope.launch {
            updateSigningUp {
                copy(
                    isLoadingColleges = true,
                    errorMessage = null,
                )
            }

            collegeApi.list(universityId, PageQuery(page = 1, pageSize = 100)).fold(
                onSuccess = { resp ->
                    val list = resp.items
                    val autoSelectedCollegeId = if (list.size == 1) list.first().id else null
                    updateSigningUp {
                        copy(
                            colleges = list,
                            isLoadingColleges = false,
                            selectedCollegeId = autoSelectedCollegeId,
                        )
                    }
                },
                onFailure = { e ->
                    updateSigningUp {
                        copy(
                            isLoadingColleges = false,
                            errorMessage = e.userMessage,
                        )
                    }
                },
            )
        }
    }

    private inline fun updateSigningUp(transform: AuthPageUiState.SigningUp.() -> AuthPageUiState.SigningUp) {
        _uiState.update { state ->
            if (state is AuthPageUiState.SigningUp) state.transform() else state
        }
    }
}

sealed interface AuthPageUiState {
    data class LoggingIn(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    ) : AuthPageUiState

    data class SigningUp(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val universities: List<UniversityResponse> = emptyList(),
        val isLoadingUniversities: Boolean = false,
        val selectedUniversityId: Long? = null,
        val colleges: List<CollegeResponse> = emptyList(),
        val isLoadingColleges: Boolean = false,
        val selectedCollegeId: Long? = null,
    ) : AuthPageUiState

    data object Success : AuthPageUiState
}
