package ink.qingshuige.csuplusplus.data.api

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
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

class FileApiTest {

    @Test
    fun deleteFileSendsDeleteRequestAndHandlesEmpty200AndProblemDetails() = runBlocking {
        var lastMethod: String? = null
        var lastPath: String? = null
        var nextStatus = 200
        var nextBody = ""

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            val bytes = nextBody.toByteArray(Charsets.UTF_8)
            if (nextStatus == 200 && bytes.isEmpty()) {
                exchange.sendResponseHeaders(200, -1)
            } else {
                exchange.responseHeaders.add("Content-Type", "application/problem+json; charset=utf-8")
                exchange.sendResponseHeaders(nextStatus, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
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
        val fileApi = FileApi(ApiClient(client, json, config))

        try {
            val successResult = fileApi.deleteFile(42L)
            assertTrue(successResult.isSuccess)
            assertEquals("DELETE", lastMethod)
            assertEquals("/api/file/42", lastPath)

            nextStatus = 403
            nextBody = """{"title":"只能删除自己上传的文件。","status":403}"""
            val forbiddenResult = fileApi.deleteFile(99L)
            assertTrue(forbiddenResult.isFailure)
            val forbiddenError = assertIs<ApiException>(forbiddenResult.exceptionOrNull())
            assertEquals(403, forbiddenError.statusCode)
            assertEquals("只能删除自己上传的文件。", forbiddenError.userMessage)

            nextStatus = 404
            nextBody = """{"title":"文件不存在。","status":404}"""
            val notFoundResult = fileApi.deleteFile(100L)
            assertTrue(notFoundResult.isFailure)
            val notFoundError = assertIs<ApiException>(notFoundResult.exceptionOrNull())
            assertEquals(404, notFoundError.statusCode)
            assertEquals("文件不存在。", notFoundError.userMessage)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
