package ink.qingshuige.csuplusplus.core.network

import kotlinx.coroutines.flow.*

/**
 * 访问令牌提供器：负责向请求拦截器提供 Bearer Token 并接收 401 失效通知。
 * 后续接入本地持久化（如 DataStore / Settings）时，只需替换此接口的实现。
 */
interface TokenProvider {
    val tokenFlow: StateFlow<String?>
    val unauthorizedEvents: SharedFlow<Unit>

    fun getToken(): String?
    fun updateToken(token: String?)
    fun clearToken()
    fun onUnauthorized()
}

class InMemoryTokenProvider : TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override fun getToken(): String? = _tokenFlow.value

    override fun updateToken(token: String?) {
        _tokenFlow.value = token
    }

    override fun clearToken() {
        _tokenFlow.value = null
    }

    override fun onUnauthorized() {
        _tokenFlow.value = null
        _unauthorizedEvents.tryEmit(Unit)
    }
}
