package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide

@Composable
internal fun CourseTitleText(
    name: String,
    alias: String?,
    titleStyle: TextStyle,
    aliasStyle: TextStyle,
    titleColor: Color,
    aliasColor: Color,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val annotatedTitle = buildAnnotatedString {
        withStyle(
            titleStyle.toSpanStyle().copy(
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            ),
        ) {
            append(name)
        }
        if (!alias.isNullOrBlank()) {
            append("  ")
            withStyle(
                aliasStyle.toSpanStyle().copy(
                    fontWeight = FontWeight.Normal,
                    color = aliasColor,
                ),
            ) {
                append(alias)
            }
        }
    }

    Text(
        text = annotatedTitle,
        modifier = modifier,
        style = titleStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ScopeSelectorBadge(
    label: String,
    fallbackIcon: ImageVector,
    emblem: Any? = null,
    emblemContentDescription: String? = null,
    maxTextWidth: Dp = 120.dp,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val iconSize = if (compact) 24.dp else 26.dp
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                start = if (compact) 4.dp else 5.dp,
                end = when {
                    onClick != null && compact -> 6.dp
                    onClick != null -> 8.dp
                    compact -> 8.dp
                    else -> 10.dp
                },
                top = 4.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
        ) {
            ScopeBadgeLeadingIcon(
                emblem = emblem,
                fallbackIcon = fallbackIcon,
                contentDescription = emblemContentDescription,
                size = iconSize,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = maxTextWidth),
            )
            if (onClick != null) {
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            content()
        }
    }
}

@Composable
private fun ScopeBadgeLeadingIcon(
    emblem: Any?,
    fallbackIcon: ImageVector,
    contentDescription: String?,
    size: Dp,
) {
    val normalizedEmblem = if (emblem is String) emblem.trim().takeIf { it.isNotEmpty() } else emblem
    when (normalizedEmblem) {
        null -> DefaultScopeIcon(
            icon = fallbackIcon,
            contentDescription = contentDescription,
            size = size,
        )

        is Painter -> {
            Image(
                painter = normalizedEmblem,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        }

        else -> {
            val painter = rememberAsyncImagePainter(model = normalizedEmblem)
            val state by painter.state.collectAsState()
            if (state is AsyncImagePainter.State.Success) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            } else {
                DefaultScopeIcon(
                    icon = fallbackIcon,
                    contentDescription = contentDescription,
                    size = size,
                )
            }
        }
    }
}

@Composable
private fun DefaultScopeIcon(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (size < 26.dp) 14.dp else 15.dp),
            )
        }
    }
}

@Composable
internal fun MetadataPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
