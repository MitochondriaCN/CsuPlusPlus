package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.LoginRequest
import ink.qingshuige.csuplusplus.data.dto.LoginResponse
import ink.qingshuige.csuplusplus.data.dto.RegisterRequest
import ink.qingshuige.csuplusplus.data.dto.UserProfileDto

/**
 * 认证与当前用户资料接口
 */
class AuthApi(private val api: ApiClient) {

    suspend fun register(request: RegisterRequest): Result<UserProfileDto> =
        api.post("auth/register", body = request)

    suspend fun login(request: LoginRequest): Result<LoginResponse> =
        api.post("auth/login", body = request)

    suspend fun getMyProfile(): Result<UserProfileDto> =
        api.get("profile/me")
}
