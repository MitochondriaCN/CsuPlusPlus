package ink.qingshuige.csuplusplus.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (url, fileName) => {
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.rel = 'noopener noreferrer';
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
    """,
)
private external fun triggerBrowserDownload(url: String, fileName: String)

@Composable
actual fun rememberPlatformFileSaver(): PlatformFileSaver = remember {
    object : PlatformFileSaver {
        override suspend fun saveFromUrl(
            url: String,
            suggestedName: String,
            extension: String,
            fetchBytes: suspend (url: String) -> Result<ByteArray>,
        ): FileSaveResult {
            if (url.isBlank()) {
                return FileSaveResult.Failure("下载失败：未获取到有效的下载链接")
            }
            val parts = resolveDownloadFileParts(suggestedName, extension)
            return try {
                triggerBrowserDownload(url = url, fileName = parts.fullName)
                FileSaveResult.Success("已开始下载：${parts.fullName}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                FileSaveResult.Failure("下载失败：${e.message ?: "未知错误"}")
            }
        }
    }
}

