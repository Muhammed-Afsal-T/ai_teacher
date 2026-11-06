package com.aiteacher.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Navy950,
    primaryContainer = Navy800,
    onPrimaryContainer = CyanAccent,
    secondary = IndigoAccent,
    onSecondary = PureWhite,
    background = Navy900,
    onBackground = Slate200,
    surface = Navy800,
    onSurface = Slate200,
    surfaceVariant = Navy700,
    onSurfaceVariant = Slate400,
    outline = Slate600
)

@Composable
fun AITeacherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
