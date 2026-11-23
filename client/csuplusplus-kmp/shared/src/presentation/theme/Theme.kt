package ink.qingshuige.csuplusplus.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1D5BD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF4E6178),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3F7),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA7F3EB),
    onTertiaryContainer = Color(0xFF00201D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF181C22),
    surface = Color(0xFFF8FAFD),
    onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFE1E6F0),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF747780),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5FA),
    surfaceContainer = Color(0xFFEAEFF7),
    surfaceContainerHigh = Color(0xFFE2E9F3),
    surfaceContainerHighest = Color(0xFFDCE3EE),
    inverseSurface = Color(0xFF2D3138),
    inverseOnSurface = Color(0xFFEFF1F7),
    inversePrimary = Color(0xFFB0C6FF),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF00419E),
    onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFFB9C7DC),
    onSecondary = Color(0xFF243245),
    secondaryContainer = Color(0xFF39485C),
    onSecondaryContainer = Color(0xFFD6E3F7),
    tertiary = Color(0xFF4FD8C8),
    onTertiary = Color(0xFF003732),
    tertiaryContainer = Color(0xFF005049),
    onTertiaryContainer = Color(0xFFA7F3EB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F141B),
    onBackground = Color(0xFFE2E6EE),
    surface = Color(0xFF0F141B),
    onSurface = Color(0xFFE2E6EE),
    surfaceVariant = Color(0xFF2E3542),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF3A4250),
    surfaceContainerLowest = Color(0xFF0A0E14),
    surfaceContainerLow = Color(0xFF171D26),
    surfaceContainer = Color(0xFF1C232E),
    surfaceContainerHigh = Color(0xFF262E3B),
    surfaceContainerHighest = Color(0xFF313A49),
    inverseSurface = Color(0xFFE2E6EE),
    inverseOnSurface = Color(0xFF2D3138),
    inversePrimary = Color(0xFF1D5BD6),
    scrim = Color(0xFF000000),
)

@Composable
fun CsuPlusPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
