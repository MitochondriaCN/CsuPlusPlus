package ink.qingshuige.csuplusplus.core.session

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.InMemoryTokenProvider
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.data.api.AuthApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SessionStoreTest {

    @Test
    fun loadsProfileAndCapabilitiesOnTokenAndResetsOnClear() = runBlocking {
        var capabilitiesStatus = 200
        var capabilitiesBody = """
            {
              "capabilities": {
                "AccessOwnUniversity": true,
                "ManageOwnUniversity": false,
                "PlatformAdmin": true
              }
            }
        """.trimIndent()
        val profileBody = """
            {
              "id": 42,
              "email": "admin@csu.edu.cn",
              "nickname": "Admin",
              "universityId": 1,
              "universityName": "中南大学",
              "collegeId": 2,
              "collegeName": "计算机学院"
            }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            val (status, body) = when (path) {
                "/api/profile/me" -> 200 to profileBody
                "/api/auth/capabilities" -> capabilitiesStatus to capabilitiesBody
                else -> 404 to "{}"
            }
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
        val config = NetworkConfig(baseUrl = "http://127.0.0.1:${server.address.port}/api")
        val client = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(DefaultRequest) {
                url(config.normalizedBaseUrl)
                accept(ContentType.Application.Json)
            }
        }
        val authApi = AuthApi(ApiClient(client, json, config))
        val tokenProvider = InMemoryTokenProvider()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val store = SessionStore(authApi = authApi, tokenProvider = tokenProvider, scope = scope)

        try {
            tokenProvider.updateToken("token-1")
            awaitUntil { store.state.value.profile != null && store.state.value.isCapabilitiesLoaded }

            val loaded = store.state.value
            assertEquals(42L, loaded.profile?.id)
            assertEquals("中南大学", loaded.profile?.universityName)
            assertTrue(loaded.capabilities.accessOwnUniversity)
            assertFalse(loaded.capabilities.manageOwnUniversity)
            assertTrue(loaded.capabilities.platformAdmin)
            assertNull(loaded.errorMessage)

            // If capabilities refresh fails transiently, profile and prior capabilities remain intact.
            capabilitiesStatus = 500
            capabilitiesBody = """{"title":"服务器错误","status":500}"""
            val refreshResult = store.refresh()
            assertTrue(refreshResult.isSuccess)
            assertTrue(store.state.value.capabilities.platformAdmin)

            // Logout clears profile and capabilities.
            tokenProvider.clearToken()
            awaitUntil { store.state.value.profile == null }
            assertFalse(store.state.value.capabilities.platformAdmin)
            assertFalse(store.state.value.isCapabilitiesLoaded)
        } finally {
            scope.cancel()
            client.close()
            server.stop(0)
        }
    }

    private suspend fun awaitUntil(timeoutMs: Long = 3_000, predicate: () -> Boolean) {
        val step = 20L
        var elapsed = 0L
        while (!predicate() && elapsed < timeoutMs) {
            delay(step.milliseconds)
            elapsed += step
        }
        assertTrue(predicate(), "Timed out waiting for SessionStore condition")
    }
}
