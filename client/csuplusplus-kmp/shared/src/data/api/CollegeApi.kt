package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.*
import io.ktor.client.request.*

/**
 * 学院接口
 */
class CollegeApi(private val api: ApiClient) {

    suspend fun list(
        universityId: Long,
        query: PageQuery = PageQuery(),
    ): Result<PagedResponse<CollegeResponse>> =
        api.get("college/list") {
            parameter("universityId", universityId)
            pageParams(query)
        }

    suspend fun create(request: CreateCollegeRequest): Result<CollegeResponse> =
        api.post("college/create", body = request)
}
