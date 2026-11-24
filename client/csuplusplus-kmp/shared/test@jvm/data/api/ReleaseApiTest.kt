package ink.qingshuige.csuplusplus.data.api

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.data.dto.ReleaseResponse
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReleaseApiTest {

    @Test
    fun getReleaseParsesResponseAndHandlesHttpErrors() = runBlocking {
        var lastMethod: String? = null
        var lastPath: String? = null
        var nextStatus = 200
        var nextContentType = "application/json; charset=utf-8"
        var nextBody =
            """{"currentBuildCode":2,"currentBuildName":"0.2.0","releaseUrl":"https://github.com/MitochondriaCN/CsuPlusPlus"}"""

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
        val releaseApi = ReleaseApi(ApiClient(client, json, config))

        try {
            val result = releaseApi.getRelease()
            assertTrue(result.isSuccess)
            assertEquals("GET", lastMethod)
            assertEquals("/api/release", lastPath)
            assertEquals(
                ReleaseResponse(
                    currentBuildCode = 2L,
                    currentBuildName = "0.2.0",
                    releaseUrl = "https://github.com/MitochondriaCN/CsuPlusPlus",
                ),
                result.getOrNull(),
            )

            nextStatus = 500
            nextContentType = "application/problem+json; charset=utf-8"
            nextBody = """{"title":"服务器内部错误","status":500}"""
            val failure = releaseApi.getRelease()
            assertTrue(failure.isFailure)
            val error = assertIs<ApiException>(failure.exceptionOrNull())
            assertEquals(500, error.statusCode)
            assertEquals("服务器内部错误", error.userMessage)
        } finally {
            client.close()
            server.stop(0)
        }
    }

    @Test
    fun hasUpdateComparesOnlyByBuildCode() {
        val release = ReleaseResponse(
            currentBuildCode = 5L,
            currentBuildName = "0.0.1",
            releaseUrl = "https://example.com/release",
        )

        assertTrue(release.hasUpdate(localBuildCode = 4L))
        assertFalse(release.hasUpdate(localBuildCode = 5L))
        assertFalse(release.hasUpdate(localBuildCode = 6L))
    }
}
