package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*

@Composable
internal fun CourseFileItem(
    file: CourseFile,
    isDownloading: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val fileIcon = when (file.type) {
        CourseFileType.PDF -> Lucide.FileText
        CourseFileType.SLIDES -> Lucide.Presentation
        CourseFileType.ARCHIVE -> Lucide.FileArchive
        CourseFileType.MARKDOWN -> Lucide.FileText
        CourseFileType.IMAGE -> Lucide.FileImage
        CourseFileType.CODE -> Lucide.FileCode
        CourseFileType.OTHER -> Lucide.FileText
    }
    val typeLabel = if (file.type == CourseFileType.OTHER && file.extension.isNotBlank()) {
        file.extension
    } else {
        file.type.label
    }
    val iconContainerColor = when (file.type) {
        CourseFileType.PDF, CourseFileType.SLIDES, CourseFileType.MARKDOWN ->
            MaterialTheme.colorScheme.primaryContainer

        CourseFileType.IMAGE, CourseFileType.CODE ->
            MaterialTheme.colorScheme.tertiaryContainer

        CourseFileType.ARCHIVE, CourseFileType.OTHER ->
            MaterialTheme.colorScheme.secondaryContainer
    }
    val iconContentColor = when (file.type) {
        CourseFileType.PDF, CourseFileType.SLIDES, CourseFileType.MARKDOWN ->
            MaterialTheme.colorScheme.onPrimaryContainer

        CourseFileType.IMAGE, CourseFileType.CODE ->
            MaterialTheme.colorScheme.onTertiaryContainer

        CourseFileType.ARCHIVE, CourseFileType.OTHER ->
            MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        enabled = !isDownloading,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = if (canDelete) 8.dp else 16.dp,
                    top = 14.dp,
                    bottom = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = iconContainerColor,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconContentColor,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatCourseFileSubtitle(
                        typeLabel = typeLabel,
                        uploaderName = file.uploaderName,
                        createdAt = file.createdAt,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Lucide.Download,
                    contentDescription = "下载",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (canDelete) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDownloading,
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "删除文件",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

internal fun formatCourseFileSubtitle(
    typeLabel: String,
    uploaderName: String,
    createdAt: String,
): String = buildList {
    add(typeLabel)
    uploaderName.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
    createdAt.trim().takeIf { it.isNotEmpty() }?.let(::add)
}.joinToString(" · ")
