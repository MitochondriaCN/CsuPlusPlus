package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.ReleaseResponse

/**
 * 发布信息接口
 */
class ReleaseApi(private val api: ApiClient) {

    suspend fun getRelease(): Result<ReleaseResponse> =
        api.get("release")
}
