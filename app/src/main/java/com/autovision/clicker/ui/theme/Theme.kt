package com.autovision.clicker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65E4FF),
    secondary = Color(0xFFA78BFA),
    tertiary = Color(0xFF79F2C0),
    background = Color(0xFF0B1020),
    surface = Color(0xFF121A2E),
    surfaceVariant = Color(0xFF1A2540)
)

@Composable
fun AutoVisionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}