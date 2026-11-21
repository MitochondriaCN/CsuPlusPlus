package ink.qingshuige.csuplusplus.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ink.qingshuige.csuplusplus.core.network.userMessage
import ink.qingshuige.csuplusplus.data.dto.DownloadedFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

private class PendingSaveHolder {
    var deferred: CompletableDeferred<PlatformFile?>? = null
}

@Composable
actual fun rememberPlatformFileSaver(): PlatformFileSaver {
    val holder = remember { PendingSaveHolder() }
    val launcher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = { error ->
            holder.deferred?.completeExceptionally(error)
            holder.deferred = null
        },
    ) { file ->
        holder.deferred?.complete(file)
        holder.deferred = null
    }

    return remember(launcher) {
        object : PlatformFileSaver {
            override suspend fun saveWithPicker(
                suggestedName: String,
                extension: String,
                fetchFile: suspend () -> Result<DownloadedFile>,
            ): FileSaveResult {
                val parts = resolveDownloadFileParts(suggestedName, extension)
                val targetFile = try {
                    val deferred = CompletableDeferred<PlatformFile?>()
                    holder.deferred?.cancel()
                    holder.deferred = deferred
                    withContext(Dispatchers.Main) {
                        launcher.launch(
                            suggestedName = parts.baseName,
                            defaultExtension = parts.extension,
                        )
                    }
                    deferred.await()
                } catch (e: CancellationException) {
                    holder.deferred = null
                    throw e
                } catch (e: Throwable) {
                    holder.deferred = null
                    return FileSaveResult.Failure("无法打开保存窗口：${e.message ?: "未知错误"}")
                } ?: return FileSaveResult.Cancelled

                val downloaded = fetchFile().getOrElse { error ->
                    return FileSaveResult.Failure("下载失败：${error.userMessage}")
                }

                return try {
                    withContext(Dispatchers.IO) {
                        targetFile.write(downloaded.bytes)
                    }
                    val savedName = targetFile.name.takeIf { it.isNotBlank() } ?: parts.fullName
                    FileSaveResult.Success("已保存：$savedName")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    FileSaveResult.Failure("保存失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }
}
