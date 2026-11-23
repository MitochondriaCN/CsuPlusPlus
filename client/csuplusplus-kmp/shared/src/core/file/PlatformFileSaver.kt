package ink.qingshuige.csuplusplus.core.file

import androidx.compose.runtime.Composable
import ink.qingshuige.csuplusplus.data.dto.DownloadedFile

sealed interface FileSaveResult {
    data class Success(val message: String) : FileSaveResult
    data object Cancelled : FileSaveResult
    data class Failure(val message: String) : FileSaveResult
}

interface PlatformFileSaver {
    suspend fun saveWithPicker(
        suggestedName: String,
        extension: String,
        fetchFile: suspend () -> Result<DownloadedFile>,
    ): FileSaveResult
}

@Composable
expect fun rememberPlatformFileSaver(): PlatformFileSaver

internal data class DownloadFileParts(
    val baseName: String,
    val extension: String?,
    val fullName: String,
)

internal fun resolveDownloadFileParts(
    suggestedName: String,
    fallbackExtension: String,
): DownloadFileParts {
    val sanitizedRaw = suggestedName
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
        .trim()
        .ifEmpty { "course_file" }

    val cleanFallbackExt = fallbackExtension
        .trim()
        .trimStart('.')
        .lowercase()
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "")
        .takeIf { it.isNotBlank() }

    val dotIndex = sanitizedRaw.lastIndexOf('.')
    val hasValidDotExt = dotIndex > 0 && dotIndex < sanitizedRaw.length - 1
    val detectedExt = if (hasValidDotExt) {
        sanitizedRaw.substring(dotIndex + 1).lowercase()
    } else {
        null
    }

    val finalExt = detectedExt ?: cleanFallbackExt
    val baseName = if (hasValidDotExt) {
        sanitizedRaw.substring(0, dotIndex)
    } else {
        sanitizedRaw
    }.ifBlank { "course_file" }

    val fullName = if (finalExt != null) "$baseName.$finalExt" else baseName
    return DownloadFileParts(
        baseName = baseName,
        extension = finalExt,
        fullName = fullName,
    )
}
