package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseResponse(
    val currentBuildCode: Long,
    val currentBuildName: String,
    val releaseUrl: String,
) {
    fun hasUpdate(localBuildCode: Long): Boolean = currentBuildCode > localBuildCode
}
