package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.*
import io.ktor.client.request.*

/**
 * 课程接口
 */
class CourseApi(private val api: ApiClient) {

    suspend fun list(query: PageQuery = PageQuery()): Result<PagedResponse<CourseResponse>> =
        api.get("course/list") {
            pageParams(query)
        }

    suspend fun search(
        keyword: String? = null,
        query: PageQuery = PageQuery(),
    ): Result<PagedResponse<CourseResponse>> =
        api.get("course/search") {
            keyword?.trim()?.takeIf { it.isNotEmpty() }?.let {
                parameter("keyword", it)
            }
            pageParams(query)
        }

    suspend fun listByCollege(
        collegeId: Long,
        query: PageQuery = PageQuery(),
    ): Result<PagedResponse<CourseResponse>> =
        api.get("course/list/college") {
            parameter("collegeId", collegeId)
            pageParams(query)
        }

    suspend fun getById(id: Long): Result<CourseResponse> =
        api.get("course/$id")

    suspend fun create(request: CreateCourseRequest): Result<CourseResponse> =
        api.post("course/create", body = request)

    suspend fun createForUniversity(
        universityId: Long,
        request: CreateCourseRequest,
    ): Result<CourseResponse> =
        api.post("course/create/any", body = request) {
            parameter("universityId", universityId)
        }
}
