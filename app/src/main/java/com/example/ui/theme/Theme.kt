package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameDeckColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonGreen,
    onTertiary = BackgroundDark,
    error = PerformanceRed,
    onError = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorderDark
)

@Composable
fun GameDeckTheme(
    content: @Composable () -> Unit
) {
    // GameDeck is always dark theme for gaming performance and OLED battery savings
    MaterialTheme(
        colorScheme = GameDeckColorScheme,
        typography = Typography,
        content = content
    )
}
