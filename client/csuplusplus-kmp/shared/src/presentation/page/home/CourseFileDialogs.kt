package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.math.round

@Composable
internal fun DeleteCourseFileDialog(
    file: CourseFile,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("删除文件") },
        text = { Text("确定要删除「${file.name}」吗？删除后将无法恢复。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除中...")
                } else {
                    Text("删除")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text("取消")
            }
        },
    )
}

private data class PickedUploadFile(
    val platformFile: PlatformFile,
    val name: String,
    val sizeText: String?,
    val mimeType: String?,
)

@Composable
internal fun UploadCourseFileDialog(
    uploadState: CourseFileUploadUiState,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onUpload: (
        fileName: String,
        mimeType: String?,
        friendlyName: String,
        readBytes: suspend () -> ByteArray,
    ) -> Unit,
) {
    var selectedFile by remember { mutableStateOf<PickedUploadFile?>(null) }
    var friendlyName by remember { mutableStateOf("") }
    var hasEditedFriendlyName by remember { mutableStateOf(false) }
    var pickerError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberFilePickerLauncher(
        onError = { error ->
            pickerError = "无法打开文件选择器：${error.message ?: "未知错误"}"
        },
    ) { file ->
        if (file != null) {
            val fileName = file.name
            val defaultFriendlyName = file.nameWithoutExtension.ifBlank { fileName }
            val sizeText = runCatching { file.size() }
                .getOrNull()
                ?.takeIf { it >= 0L }
                ?.let(::formatFileSize)
            val mimeType = runCatching { file.mimeType()?.toString() }.getOrNull()

            selectedFile = PickedUploadFile(
                platformFile = file,
                name = fileName,
                sizeText = sizeText,
                mimeType = mimeType,
            )
            if (!hasEditedFriendlyName || friendlyName.isBlank()) {
                friendlyName = defaultFriendlyName
            }
            pickerError = null
            onClearError()
        }
    }

    val isUploading = uploadState.isUploading
    val displayError = pickerError ?: uploadState.errorMessage

    AlertDialog(
        onDismissRequest = {
            if (!isUploading) onDismiss()
        },
        title = { Text("上传文件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(
                    onClick = { filePickerLauncher.launch() },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (selectedFile == null) Lucide.Upload else Lucide.FileText,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = selectedFile?.name ?: "选择文件",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = selectedFile?.sizeText ?: "点击从本地选取要上传的文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selectedFile != null) {
                            Text(
                                text = "重选",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = friendlyName,
                    onValueChange = {
                        friendlyName = it
                        hasEditedFriendlyName = true
                        if (uploadState.errorMessage != null) {
                            onClearError()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading,
                    label = { Text("显示名称") },
                    placeholder = { Text("留空则使用默认名称") },
                    singleLine = true,
                )

                if (displayError != null) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFile?.let { file ->
                        onUpload(
                            file.name,
                            file.mimeType,
                            friendlyName,
                            { file.platformFile.readBytes() },
                        )
                    }
                },
                enabled = selectedFile != null && !isUploading,
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传中...")
                } else {
                    Text("上传")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading,
            ) {
                Text("取消")
            }
        },
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${formatOneDecimal(kb)} KB"
    val mb = kb / 1024.0
    return "${formatOneDecimal(mb)} MB"
}

private fun formatOneDecimal(value: Double): String {
    val roundedTenths = round(value * 10.0).toLong()
    val whole = roundedTenths / 10L
    val frac = roundedTenths % 10L
    return if (frac == 0L) "$whole" else "$whole.$frac"
}
