package ink.qingshuige.csuplusplus.core.network

import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TokenProvider {
    val tokenFlow: StateFlow<String?>
    val isReady: StateFlow<Boolean>
    val unauthorizedEvents: SharedFlow<Unit>

    suspend fun awaitReady()
    fun getToken(): String?
    fun updateToken(token: String?)
    fun clearToken()
    fun onUnauthorized()
}

class InMemoryTokenProvider(
    initialToken: String? = null,
) : TokenProvider {
    private val _tokenFlow = MutableStateFlow(initialToken?.takeIf { it.isNotBlank() })
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override suspend fun awaitReady() = Unit

    override fun getToken(): String? = _tokenFlow.value

    override fun updateToken(token: String?) {
        _tokenFlow.value = token?.takeIf { it.isNotBlank() }
    }

    override fun clearToken() {
        _tokenFlow.value = null
    }

    override fun onUnauthorized() {
        _tokenFlow.value = null
        _unauthorizedEvents.tryEmit(Unit)
    }
}

class PersistentTokenProvider(
    private val ksafe: KSafe?,
) : TokenProvider {
    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    private val initMutex = Mutex()

    init {
        if (ksafe == null || !ksafeNeedsAsyncInit) {
            _tokenFlow.value = ksafe?.readStoredToken()
            _isReady.value = true
        }
    }

    override suspend fun awaitReady() {
        if (_isReady.value) return
        initMutex.withLock {
            if (_isReady.value) return
            val store = ksafe
            if (store != null) {
                try {
                    awaitKSafeReady(store)
                    if (_tokenFlow.value == null) {
                        _tokenFlow.value = store.readStoredToken()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    currentCoroutineContext().ensureActive()
                    // Degrade cleanly to in-memory session if encrypted storage fails to initialize.
                }
            }
            _isReady.value = true
        }
    }

    override fun getToken(): String? = _tokenFlow.value

    override fun updateToken(token: String?) {
        val normalized = token?.takeIf { it.isNotBlank() }
        _tokenFlow.value = normalized
        val store = ksafe ?: return
        runCatching {
            if (normalized == null) {
                store.deleteDirect(KEY_AUTH_TOKEN)
            } else {
                store.putDirect(KEY_AUTH_TOKEN, normalized)
            }
        }
    }

    override fun clearToken() {
        updateToken(null)
    }

    override fun onUnauthorized() {
        updateToken(null)
        _unauthorizedEvents.tryEmit(Unit)
    }

    private fun KSafe.readStoredToken(): String? =
        runCatching { getDirect(KEY_AUTH_TOKEN, "").takeIf { it.isNotBlank() } }.getOrNull()

    private companion object {
        const val KEY_AUTH_TOKEN = "auth_token"
    }
}
