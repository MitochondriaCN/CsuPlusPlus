package ink.qingshuige.csuplusplus.core.network

import kotlinx.coroutines.flow.*

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
