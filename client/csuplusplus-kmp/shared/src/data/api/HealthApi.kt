package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.HealthCheckResponse

/**
 * 健康检查接口
 */
class HealthApi(private val api: ApiClient) {

    suspend fun check(): Result<HealthCheckResponse> =
        api.get("health/check")
}
