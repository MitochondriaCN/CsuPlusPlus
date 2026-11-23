package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheckResponse(
    val currentDateTime: String,
)
