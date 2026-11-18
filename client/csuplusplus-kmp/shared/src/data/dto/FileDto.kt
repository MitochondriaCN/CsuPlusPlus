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
) {
    val displayName: String
        get() = friendlyFilename?.takeIf { it.isNotBlank() } ?: guidFilename
}

@Serializable
data class CourseFileResponse(
    val courseId: Long,
    val fileId: Long,
)

data class DownloadedFile(
    val bytes: ByteArray,
    val fileName: String?,
    val contentType: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadedFile) return false
        return bytes.contentEquals(other.bytes) &&
                fileName == other.fileName &&
                contentType == other.contentType
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        return result
    }
}
