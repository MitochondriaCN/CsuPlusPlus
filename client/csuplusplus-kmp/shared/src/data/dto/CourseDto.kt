package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    val id: Long,
    val name: String,
    val universityId: Long,
    val universityName: String,
    val collegeId: Long? = null,
    val collegeName: String? = null,
    val alias: String? = null,
    val hours: Int? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateCourseRequest(
    val name: String,
    val collegeId: Long? = null,
    val alias: String? = null,
    val hours: Int? = null,
)
