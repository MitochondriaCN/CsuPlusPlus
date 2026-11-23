package ink.qingshuige.csuplusplus.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.dto.DownloadedFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.coroutines.CancellationException

@Composable
actual fun rememberPlatformFileSaver(): PlatformFileSaver = remember {
    object : PlatformFileSaver {
        override suspend fun saveWithPicker(
            suggestedName: String,
            extension: String,
            fetchFile: suspend () -> Result<DownloadedFile>,
        ): FileSaveResult {
            val downloaded = fetchFile().getOrElse { error ->
                return FileSaveResult.Failure("下载失败：${error.userMessage}")
            }
            val parts = resolveDownloadFileParts(
                suggestedName = downloaded.fileName ?: suggestedName,
                fallbackExtension = extension,
            )
            return try {
                FileKit.download(
                    bytes = downloaded.bytes,
                    fileName = parts.fullName,
                )
                FileSaveResult.Success("已开始下载：${parts.fullName}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                FileSaveResult.Failure("保存失败：${e.message ?: "未知错误"}")
            }
        }
    }
}
