package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String,
    val collegeId: Long,
)

@Serializable
data class UserProfileDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val universityId: Long,
    val universityName: String,
    val collegeId: Long,
    val collegeName: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresAt: String,
    val user: UserProfileDto,
)

@Serializable
data class CapabilitiesResponse(
    val capabilities: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String,
)
