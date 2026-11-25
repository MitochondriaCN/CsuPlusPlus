package ink.qingshuige.csuplusplus.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileMetadataResponse(
    val id: Long,
    val guidFilename: String,
    val friendlyFilename: String? = null,
    val extension: String,
    val sha256Hash: String,
    val uploaderUserId: Long,
    val uploaderUserNickname: String,
    val createdAt: String,
) {
    val displayName: String
        get() = friendlyFilename?.takeIf { it.isNotBlank() } ?: guidFilename
}

@Serializable
data class CourseFileResponse(
    val courseId: Long,
    val fileId: Long,
)

@Serializable
data class DownloadFileUrlResponse(
    val url: String,
    val expiresAt: String,
    val downloadName: String,
    val contentType: String,
)

