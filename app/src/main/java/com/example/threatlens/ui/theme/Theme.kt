package com.example.threatlens.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createThreatLensColorScheme(palette: ThreatLensColorPalette) = if (palette.isDark) {
    darkColorScheme(
        primary = palette.cyan,
        onPrimary = Color(0xFF002029),
        primaryContainer = Color(0xFF003744),
        onPrimaryContainer = palette.cyanLight,

        secondary = palette.mediumSeverity,
        onSecondary = Color(0xFF001F29),
        secondaryContainer = Color(0xFF0A2E46),
        onSecondaryContainer = Color(0xFF8CE3FF),

        tertiary = palette.activeGreen,
        onTertiary = Color(0xFF00220E),
        tertiaryContainer = palette.activeGreenBg,
        onTertiaryContainer = Color(0xFF6BFFB0),

        background = palette.background,
        onBackground = palette.textPrimary,

        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,

        outline = palette.cardBorder,
        outlineVariant = Color(0xFF162547),

        error = palette.error,
        onError = Color.White,
        errorContainer = palette.errorContainer,
        onErrorContainer = palette.onErrorContainer
    )
} else {
    lightColorScheme(
        primary = palette.cyan,
        onPrimary = Color.White,
        primaryContainer = palette.cyanLight,
        onPrimaryContainer = Color(0xFF00363F),

        secondary = palette.mediumSeverity,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = Color(0xFF0369A1),

        tertiary = palette.activeGreen,
        onTertiary = Color.White,
        tertiaryContainer = palette.activeGreenBg,
        onTertiaryContainer = Color(0xFF14532D),

        background = palette.background,
        onBackground = palette.textPrimary,

        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,

        outline = palette.cardBorder,
        outlineVariant = Color(0xFFCBD5E1),

        error = palette.error,
        onError = Color.White,
        errorContainer = palette.errorContainer,
        onErrorContainer = palette.onErrorContainer
    )
}

@Composable
fun CyberSentinelTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkThreatLensColors else LightThreatLensColors
    val colorScheme = createThreatLensColorScheme(palette)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.surface.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalThreatLensColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = CyberSentinelTheme(darkTheme = darkTheme, content = content)
