package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.*

/**
 * 大学接口
 */
class UniversityApi(private val api: ApiClient) {

    suspend fun list(query: PageQuery = PageQuery()): Result<PagedResponse<UniversityResponse>> =
        api.get("university/list") {
            pageParams(query)
        }

    suspend fun create(request: CreateUniversityRequest): Result<UniversityResponse> =
        api.post("university/create", body = request)
}
