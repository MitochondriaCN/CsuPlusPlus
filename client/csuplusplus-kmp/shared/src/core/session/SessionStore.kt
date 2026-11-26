package ink.qingshuige.csuplusplus.core.session

import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.api.AuthApi
import ink.qingshuige.csuplusplus.data.dto.UserProfileDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object PolicyNames {
    const val ACCESS_OWN_UNIVERSITY = "AccessOwnUniversity"
    const val MANAGE_OWN_UNIVERSITY = "ManageOwnUniversity"
    const val PLATFORM_ADMIN = "PlatformAdmin"
}

data class UserCapabilities(
    val accessOwnUniversity: Boolean = false,
    val manageOwnUniversity: Boolean = false,
    val platformAdmin: Boolean = false,
) {
    companion object {
        fun fromMap(map: Map<String, Boolean>): UserCapabilities = UserCapabilities(
            accessOwnUniversity = map[PolicyNames.ACCESS_OWN_UNIVERSITY] == true,
            manageOwnUniversity = map[PolicyNames.MANAGE_OWN_UNIVERSITY] == true,
            platformAdmin = map[PolicyNames.PLATFORM_ADMIN] == true,
        )
    }
}

data class SessionState(
    val profile: UserProfileDto? = null,
    val capabilities: UserCapabilities = UserCapabilities(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 当前登录用户的统一会话与权限状态中心。
 */
class SessionStore(
    private val authApi: AuthApi,
    private val tokenProvider: TokenProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var activeToken: String? = null
    private var loadTask: Deferred<Result<Unit>>? = null

    init {
        scope.launch {
            tokenProvider.tokenFlow.collect { token ->
                loadTask?.cancel()
                loadTask = null
                if (token == null) {
                    activeToken = null
                    _state.value = SessionState()
                } else {
                    val isAccountSwitch = activeToken != null && activeToken != token
                    activeToken = token
                    if (isAccountSwitch) {
                        _state.value = SessionState()
                    } else {
                        _state.update {
                            it.copy(
                                capabilities = UserCapabilities(),
                                errorMessage = null,
                            )
                        }
                    }
                    startLoad()
                }
            }
        }
    }

    fun seedProfile(profile: UserProfileDto) {
        _state.update { it.copy(profile = profile, errorMessage = null) }
    }

    suspend fun refresh(): Result<Unit> {
        if (tokenProvider.getToken() == null) {
            return Result.failure(IllegalStateException("未登录"))
        }
        val task = startLoad()
        return task.await()
    }

    private fun startLoad(): Deferred<Result<Unit>> {
        loadTask?.cancel()
        val expectedToken = tokenProvider.getToken()
        val task = scope.async {
            if (expectedToken == null) {
                return@async Result.failure(IllegalStateException("未登录"))
            }
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                coroutineScope {
                    val profileDeferred = async { authApi.getMyProfile() }
                    val capabilitiesDeferred = async { authApi.getCapabilities() }

                    val profileResult = profileDeferred.await()
                    val capabilitiesResult = capabilitiesDeferred.await()

                    if (tokenProvider.getToken() != expectedToken) {
                        return@coroutineScope Result.failure(CancellationException("会话已变更"))
                    }

                    val nextCapabilities = capabilitiesResult.getOrNull()?.let {
                        UserCapabilities.fromMap(it.capabilities)
                    }

                    profileResult.fold(
                        onSuccess = { profile ->
                            _state.update { current ->
                                current.copy(
                                    profile = profile,
                                    capabilities = nextCapabilities ?: current.capabilities,
                                    isLoading = false,
                                    errorMessage = null,
                                )
                            }
                            Result.success(Unit)
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            _state.update { current ->
                                current.copy(
                                    capabilities = nextCapabilities ?: current.capabilities,
                                    isLoading = false,
                                    errorMessage = if (current.profile == null) error.userMessage else null,
                                )
                            }
                            Result.failure(error)
                        },
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isLoading = false) }
                throw e
            }
        }
        loadTask = task
        return task
    }
}
