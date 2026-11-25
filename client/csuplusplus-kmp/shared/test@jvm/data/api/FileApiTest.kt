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

    @Test
    fun getMetadataDecodesFileMetadataResponseWithCreatedAt() = runBlocking {
        var lastMethod: String? = null
        var lastPath: String? = null
        var lastQuery: String? = null
        val responseBody = """
            {
              "id": 7,
              "guidFilename": "a1b2c3.pdf",
              "friendlyFilename": "高数课件.pdf",
              "extension": ".pdf",
              "sha256Hash": "deadbeef",
              "uploaderUserId": 1,
              "uploaderUserNickname": "Tester",
              "createdAt": "2026-11-25T08:00:00+08:00"
            }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            lastQuery = exchange.requestURI.query
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
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
        val fileApi = FileApi(ApiClient(client, json, config))

        try {
            val result = fileApi.getMetadata(7L)
            assertTrue(result.isSuccess)
            assertEquals("GET", lastMethod)
            assertEquals("/api/file", lastPath)
            assertEquals("id=7", lastQuery)
            val metadata = result.getOrThrow()
            assertEquals(7L, metadata.id)
            assertEquals("高数课件.pdf", metadata.displayName)
            assertEquals("2026-11-25T08:00:00+08:00", metadata.createdAt)
        } finally {
            client.close()
            server.stop(0)
        }
    }

    @Test
    fun listCourseFilesDecodesPagedFileMetadataWithCreatedAt() = runBlocking {
        var lastMethod: String? = null
        var lastPath: String? = null
        var lastQuery: String? = null
        val responseBody = """
            {
              "items": [
                {
                  "id": 7,
                  "guidFilename": "a1b2c3.pdf",
                  "friendlyFilename": "高数课件.pdf",
                  "extension": ".pdf",
                  "sha256Hash": "deadbeef",
                  "uploaderUserId": 1,
                  "uploaderUserNickname": "Tester",
                  "createdAt": "2026-11-25T08:00:00+08:00"
                }
              ],
              "page": 1,
              "pageSize": 20,
              "totalCount": 1
            }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastMethod = exchange.requestMethod
            lastPath = exchange.requestURI.path
            lastQuery = exchange.requestURI.query
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
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
        val fileApi = FileApi(ApiClient(client, json, config))

        try {
            val result = fileApi.listCourseFiles(courseId = 3L)
            assertTrue(result.isSuccess)
            assertEquals("GET", lastMethod)
            assertEquals("/api/file/list/course", lastPath)
            assertTrue(lastQuery?.contains("courseId=3") == true)
            val page = result.getOrThrow()
            assertEquals(1, page.items.size)
            assertEquals("2026-11-25T08:00:00+08:00", page.items.single().createdAt)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
