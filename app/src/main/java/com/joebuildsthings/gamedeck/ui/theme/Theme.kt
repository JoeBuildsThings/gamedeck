package com.joebuildsthings.gamedeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = BgDark,
    secondary = AccentRedBright,
    background = BgDark,
    onBackground = InkLight,
    surface = PanelDark,
    onSurface = InkLight,
    surfaceVariant = PanelDark,
    onSurfaceVariant = MutedGrey,
    outline = LineDark,
)

@Composable
fun GameDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else DarkScheme,
        content = content
    )
}
