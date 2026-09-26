package com.ahmedsamy.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
        darkColorScheme(
                primary = TerminalGreen,
                onPrimary = TerminalBackground,
                secondary = ButtonActive,
                onSecondary = TextPrimary,
                tertiary = ButtonSecondary,
                onTertiary = TextLight,
                background = TerminalBackground,
                onBackground = TextPrimary,
                surface = TerminalCardBackground,
                onSurface = TextPrimary,
                surfaceVariant = ButtonInactive,
                onSurfaceVariant = TextSecondary,
                outline = TerminalBorder,
                outlineVariant = DividerMedium
        )

private val LightColorScheme =
        lightColorScheme(
                primary = LightTerminalGreen,
                onPrimary = LightTerminalBackground,
                secondary = LightButtonActive,
                onSecondary = LightTextPrimary,
                tertiary = LightButtonSecondary,
                onTertiary = LightTextPrimary,
                background = LightTerminalBackground,
                onBackground = LightTextPrimary,
                surface = LightTerminalCardBackground,
                onSurface = LightTextPrimary,
                surfaceVariant = LightButtonInactive,
                onSurfaceVariant = LightTextSecondary,
                outline = LightTerminalBorder,
                outlineVariant = LightDividerMedium
        )

@Composable
fun AppTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
