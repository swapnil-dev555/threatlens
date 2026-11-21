package com.example.threatlens.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThreatLensColorPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cyan: Color,
    val cyanDark: Color,
    val cyanLight: Color,
    val activeGreen: Color,
    val activeGreenBg: Color,
    val highSeverity: Color,
    val mediumSeverity: Color,
    val lowSeverity: Color,
    val warning: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val isDark: Boolean
)

val DarkThreatLensColors = ThreatLensColorPalette(
    background = Color(0xFF080E1E),
    surface = Color(0xFF0E172E),
    surfaceElevated = Color(0xFF13203E),
    surfaceHighlight = Color(0xFF1B2C52),
    cardBorder = Color(0xFF1D325E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8FA3CE),
    textTertiary = Color(0xFF5A709E),
    cyan = Color(0xFF00E5FF),
    cyanDark = Color(0xFF0097A7),
    cyanLight = Color(0xFF84FFFF),
    activeGreen = Color(0xFF00E676),
    activeGreenBg = Color(0xFF072B1E),
    highSeverity = Color(0xFFFF3366),
    mediumSeverity = Color(0xFF00B4D8),
    lowSeverity = Color(0xFF00E676),
    warning = Color(0xFFFFAB00),
    error = Color(0xFFFF3B5C),
    errorContainer = Color(0xFF3B0D18),
    onErrorContainer = Color(0xFFFF8DA1),
    isDark = true
)

val LightThreatLensColors = ThreatLensColorPalette(
    background = Color(0xFFF3F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceHighlight = Color(0xFFE7ECF3),
    cardBorder = Color(0xFFD6DFEC),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF8290A5),
    cyan = Color(0xFF00838F),
    cyanDark = Color(0xFF005662),
    cyanLight = Color(0xFFE0F7FA),
    activeGreen = Color(0xFF15803D),
    activeGreenBg = Color(0xFFDCFCE7),
    highSeverity = Color(0xFFDC2626),
    mediumSeverity = Color(0xFF0284C7),
    lowSeverity = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    isDark = false
)

val LocalThreatLensColors = staticCompositionLocalOf { DarkThreatLensColors }

// Composable properties that automatically reflect the active theme
val ThreatLensBackground: Color @Composable get() = LocalThreatLensColors.current.background
val ThreatLensSurface: Color @Composable get() = LocalThreatLensColors.current.surface
val ThreatLensSurfaceElevated: Color @Composable get() = LocalThreatLensColors.current.surfaceElevated
val ThreatLensSurfaceHighlight: Color @Composable get() = LocalThreatLensColors.current.surfaceHighlight
val ThreatLensCardBorder: Color @Composable get() = LocalThreatLensColors.current.cardBorder
val ThreatLensTextPrimary: Color @Composable get() = LocalThreatLensColors.current.textPrimary
val ThreatLensTextSecondary: Color @Composable get() = LocalThreatLensColors.current.textSecondary
val ThreatLensTextTertiary: Color @Composable get() = LocalThreatLensColors.current.textTertiary
val ThreatLensCyan: Color @Composable get() = LocalThreatLensColors.current.cyan
val ThreatLensCyanDark: Color @Composable get() = LocalThreatLensColors.current.cyanDark
val ThreatLensCyanLight: Color @Composable get() = LocalThreatLensColors.current.cyanLight
val ThreatLensActiveGreen: Color @Composable get() = LocalThreatLensColors.current.activeGreen
val ThreatLensActiveGreenBg: Color @Composable get() = LocalThreatLensColors.current.activeGreenBg
val ThreatLensHighSeverity: Color @Composable get() = LocalThreatLensColors.current.highSeverity
val ThreatLensMediumSeverity: Color @Composable get() = LocalThreatLensColors.current.mediumSeverity
val ThreatLensLowSeverity: Color @Composable get() = LocalThreatLensColors.current.lowSeverity
val ThreatLensWarning: Color @Composable get() = LocalThreatLensColors.current.warning
val ThreatLensError: Color @Composable get() = LocalThreatLensColors.current.error
val ThreatLensErrorContainer: Color @Composable get() = LocalThreatLensColors.current.errorContainer
val ThreatLensOnErrorContainer: Color @Composable get() = LocalThreatLensColors.current.onErrorContainer
