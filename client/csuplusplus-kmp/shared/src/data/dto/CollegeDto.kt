package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollegeResponse(
    val id: Long,
    val name: String,
    val universityId: Long,
    val universityName: String,
)

@Serializable
data class CreateCollegeRequest(
    val universityId: Long,
    val name: String,
)
