package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UniversityResponse(
    val id: Long,
    val name: String,
    val emblemFilename: String? = null,
)

@Serializable
data class CreateUniversityRequest(
    val name: String,
)
