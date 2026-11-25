package ink.qingshuige.csuplusplus.data.api

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.core.network.SkipAuthAttributeKey
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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

    @Test
    fun getCourseDownloadUrlAndDownloadBytesRespectAuthIsolation() = runBlocking {
        var apiAuthHeader: String? = null
        var apiPath: String? = null
        var r2AuthHeader: String? = "unexpected"
        var r2Path: String? = null
        var r2Query: String? = null
        var r2AcceptHeader: String? = null
        var unauthorizedTriggered = false
        var nextR2Status = 200
        val expectedPayload = byteArrayOf(1, 2, 3, 4, 5, 6, 7)

        val r2Server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        r2Server.createContext("/") { exchange ->
            r2AuthHeader = exchange.requestHeaders.getFirst("Authorization")
            r2AcceptHeader = exchange.requestHeaders.getFirst("Accept")
            r2Path = exchange.requestURI.path
            r2Query = exchange.requestURI.query
            if (nextR2Status == 200) {
                exchange.responseHeaders.add("Content-Type", "application/pdf")
                exchange.sendResponseHeaders(200, expectedPayload.size.toLong())
                exchange.responseBody.use { it.write(expectedPayload) }
            } else {
                exchange.sendResponseHeaders(nextR2Status, -1)
            }
            exchange.close()
        }
        r2Server.start()

        val presignedUrl =
            "http://127.0.0.1:${r2Server.address.port}/bucket/abc123.pdf?X-Amz-Signature=sig"

        val apiServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        apiServer.createContext("/") { exchange ->
            apiAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            apiPath = exchange.requestURI.path
            val body = """
                {
                  "url": "$presignedUrl",
                  "expiresAt": "2026-11-25T10:30:00+00:00",
                  "downloadName": "高数课件.pdf",
                  "contentType": "application/pdf"
                }
            """.trimIndent().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
            exchange.close()
        }
        apiServer.start()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
        val config = NetworkConfig(baseUrl = "http://127.0.0.1:${apiServer.address.port}/api")
        val client = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(DefaultRequest) {
                url(config.normalizedBaseUrl)
                accept(ContentType.Application.Json)
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                val skipAuth = request.attributes.getOrNull(SkipAuthAttributeKey) == true
                var sentBearerToken = false
                if (!skipAuth && !request.headers.contains(HttpHeaders.Authorization)) {
                    request.headers.append(HttpHeaders.Authorization, "Bearer test-jwt-token")
                    sentBearerToken = true
                }
                val call = execute(request)
                if (sentBearerToken && call.response.status == HttpStatusCode.Unauthorized) {
                    unauthorizedTriggered = true
                }
                call
            }
        }
        val fileApi = FileApi(ApiClient(client, json, config))

        try {
            val urlResult = fileApi.getCourseDownloadUrl(15L)
            assertTrue(urlResult.isSuccess)
            assertEquals("/api/file/download/course/15", apiPath)
            assertEquals("Bearer test-jwt-token", apiAuthHeader)
            val urlResponse = urlResult.getOrThrow()
            assertEquals(presignedUrl, urlResponse.url)
            assertEquals("高数课件.pdf", urlResponse.downloadName)
            assertEquals("application/pdf", urlResponse.contentType)

            val bytesResult = fileApi.downloadBytes(urlResponse.url)
            assertTrue(bytesResult.isSuccess)
            assertEquals("/bucket/abc123.pdf", r2Path)
            assertEquals("X-Amz-Signature=sig", r2Query)
            assertNull(r2AuthHeader)
            assertTrue(r2AcceptHeader?.contains("*/*") == true)
            assertContentEquals(expectedPayload, bytesResult.getOrThrow())

            nextR2Status = 401
            val failedResult = fileApi.downloadBytes(urlResponse.url)
            assertTrue(failedResult.isFailure)
            assertFalse(unauthorizedTriggered)
        } finally {
            client.close()
            apiServer.stop(0)
            r2Server.stop(0)
        }
    }
}
