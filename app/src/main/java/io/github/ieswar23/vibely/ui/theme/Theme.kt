package io.github.ieswar23.vibely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Violet600,
    onPrimary = Color.White,
    primaryContainer = Violet100,
    onPrimaryContainer = Violet950,
    secondary = Pink500,
    onSecondary = Color.White,
    secondaryContainer = Pink100,
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Orange400,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    inverseSurface = Color(0xFF26252E),
    inverseOnSurface = Color(0xFFF4F3F8),
    inversePrimary = Violet200,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1E0B4B),
    primaryContainer = Color(0xFF3B1C78),
    onPrimaryContainer = Violet100,
    secondary = Pink400,
    onSecondary = Color(0xFF3F0420),
    secondaryContainer = Color(0xFF5B1236),
    onSecondaryContainer = Pink100,
    tertiary = Orange400,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF09090D),
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    inverseSurface = Color(0xFFEDECF3),
    inverseOnSurface = Color(0xFF1C1B22),
    inversePrimary = Violet700,
)

private val VibelyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 4dp based spacing scale used across all screens. */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun VibelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = VibelyTypography,
            shapes = VibelyShapes,
            content = content,
        )
    }
}

object VibelyThemeExt {
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
