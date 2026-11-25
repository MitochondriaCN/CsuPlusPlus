package ink.qingshuige.csuplusplus.data.api

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.data.dto.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthApiTest {

    @Test
    fun registerDecodesLoginResponseAndHandlesProblemDetails() = runBlocking {
        var lastMethod: String? = null
        var lastPath: String? = null
        var nextStatus = 200
        var nextContentType = "application/json; charset=utf-8"
        var nextBody = """
            {
              "token": "jwt-token-123",
              "expiresAt": "2026-11-24T12:00:00+00:00",
              "user": {
                "id": 1,
                "email": "test@csu.edu.cn",
                "nickname": "Tester",
                "universityId": 1,
                "universityName": "中南大学",
                "collegeId": 2,
                "collegeName": "计算机学院"
              }
            }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            val bytes = nextBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", nextContentType)
            exchange.sendResponseHeaders(nextStatus, bytes.size.toLong())
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

        try {
            val request = RegisterRequest(
                email = "test@csu.edu.cn",
                password = "Password123!",
                nickname = "Tester",
                collegeId = 2L,
            )
            val successResult = authApi.register(request)
            assertTrue(successResult.isSuccess)
            assertEquals("POST", lastMethod)
            assertEquals("/api/auth/register", lastPath)
            val response = successResult.getOrThrow()
            assertEquals("jwt-token-123", response.token)
            assertEquals("2026-11-24T12:00:00+00:00", response.expiresAt)
            assertEquals(1L, response.user.id)
            assertEquals("test@csu.edu.cn", response.user.email)
            assertEquals("Tester", response.user.nickname)
            assertEquals(1L, response.user.universityId)
            assertEquals("中南大学", response.user.universityName)
            assertEquals(2L, response.user.collegeId)
            assertEquals("计算机学院", response.user.collegeName)

            nextStatus = 409
            nextContentType = "application/problem+json; charset=utf-8"
            nextBody = """{"title":"邮箱已注册。","status":409}"""
            val conflictResult = authApi.register(request)
            assertTrue(conflictResult.isFailure)
            val conflictError = assertIs<ApiException>(conflictResult.exceptionOrNull())
            assertEquals(409, conflictError.statusCode)
            assertEquals("邮箱已注册。", conflictError.userMessage)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
