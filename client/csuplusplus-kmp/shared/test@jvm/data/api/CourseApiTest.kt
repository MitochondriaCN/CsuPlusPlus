package ink.qingshuige.csuplusplus.data.api

import com.sun.net.httpserver.HttpServer
import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.ApiException
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.data.dto.PageQuery
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

class CourseApiTest {

    @Test
    fun listSendsOptionalCollegeIdAndHandlesCrossUniversityError() = runBlocking {
        var lastPath: String? = null
        var lastQuery: String? = null
        var nextStatus = 200
        var nextContentType = "application/json; charset=utf-8"
        var nextBody = """
            {
              "items": [
                {
                  "id": 10,
                  "name": "数据结构",
                  "universityId": 1,
                  "universityName": "中南大学",
                  "collegeId": 2,
                  "collegeName": "计算机学院",
                  "alias": "DS",
                  "hours": 64,
                  "createdAt": "2026-11-24T12:00:00+08:00",
                  "updatedAt": "2026-11-25T12:00:00+08:00"
                }
              ],
              "page": 1,
              "pageSize": 20,
              "totalCount": 1
            }
        """.trimIndent()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            lastPath = exchange.requestURI.path
            lastQuery = exchange.requestURI.query
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
        val courseApi = CourseApi(ApiClient(client, json, config))

        try {
            val allResult = courseApi.list(query = PageQuery(page = 1, pageSize = 20))
            assertTrue(allResult.isSuccess)
            assertEquals("/api/course/list", lastPath)
            val allQuery = requireNotNull(lastQuery)
            assertFalse(allQuery.contains("collegeId="))
            assertTrue(allQuery.contains("page=1"))
            assertTrue(allQuery.contains("pageSize=20"))

            val filteredResult = courseApi.list(collegeId = 2L, query = PageQuery(page = 2, pageSize = 10))
            assertTrue(filteredResult.isSuccess)
            assertEquals("/api/course/list", lastPath)
            val filteredQuery = requireNotNull(lastQuery)
            assertTrue(filteredQuery.contains("collegeId=2"))
            assertTrue(filteredQuery.contains("page=2"))
            assertTrue(filteredQuery.contains("pageSize=10"))
            assertEquals("计算机学院", filteredResult.getOrThrow().items.single().collegeName)

            nextStatus = 403
            nextContentType = "application/problem+json; charset=utf-8"
            nextBody = """{"title":"只能查询本校学院的课程。","status":403}"""
            val forbiddenResult = courseApi.list(collegeId = 999L)
            assertTrue(forbiddenResult.isFailure)
            val forbiddenError = assertIs<ApiException>(forbiddenResult.exceptionOrNull())
            assertEquals(403, forbiddenError.statusCode)
            assertEquals("只能查询本校学院的课程。", forbiddenError.userMessage)
        } finally {
            client.close()
            server.stop(0)
        }
    }
}
