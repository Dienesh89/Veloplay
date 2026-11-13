package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val YtBlack = Color(0xFF0F0F0F)
val YtTrueBlack = Color(0xFF000000)
val YtSurface = Color(0xFF181818)
val YtSheetSurface = Color(0xFF212121)
val YtChipBackground = Color(0xFF272727)
val YtChipElevated = Color(0xFF3A3A3A)
val YtRed = Color(0xFFFF0033)
val YtProgressRed = Color(0xFFFF0000)
val YtTextPrimary = Color(0xFFFFFFFF)
val YtTextSecondary = Color(0xFFAAAAAA)
val YtTextTertiary = Color(0xFF717171)
val YtBlue = Color(0xFF3EA6FF)
val YtDivider = Color(0xFF2C2C2C)
val YtOverlayScrim = Color(0x99000000)
val YtBadgeDark = Color(0xCC000000)
val YtSuccessGreen = Color(0xFF2BA640)

// Authentic YouTube Light Mode palette
val YtLightBackground = Color(0xFFFFFFFF)
val YtLightSurface = Color(0xFFF6F6F6)
val YtLightSheetSurface = Color(0xFFFFFFFF)
val YtLightChipBackground = Color(0xFFF2F2F2)
val YtLightChipElevated = Color(0xFFE5E5E5)
val YtLightTextPrimary = Color(0xFF0F0F0F)
val YtLightTextSecondary = Color(0xFF606060)
val YtLightBlue = Color(0xFF065FD4)
val YtLightDivider = Color(0xFFE5E5E5)

@Immutable
data class VeloColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val sheetSurface: Color,
    val chipBackground: Color,
    val chipElevated: Color,
    val selectedChipBackground: Color,
    val selectedChipText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentBlue: Color,
    val divider: Color,
    val red: Color,
    val progressRed: Color,
    val successGreen: Color
)

val DarkVeloColors = VeloColors(
    isDark = true,
    background = YtBlack,
    surface = YtSurface,
    sheetSurface = YtSheetSurface,
    chipBackground = YtChipBackground,
    chipElevated = YtChipElevated,
    selectedChipBackground = YtTextPrimary,
    selectedChipText = YtBlack,
    textPrimary = YtTextPrimary,
    textSecondary = YtTextSecondary,
    accentBlue = YtBlue,
    divider = YtDivider,
    red = YtRed,
    progressRed = YtProgressRed,
    successGreen = YtSuccessGreen
)

val LightVeloColors = VeloColors(
    isDark = false,
    background = YtLightBackground,
    surface = YtLightSurface,
    sheetSurface = YtLightSheetSurface,
    chipBackground = YtLightChipBackground,
    chipElevated = YtLightChipElevated,
    selectedChipBackground = YtLightTextPrimary,
    selectedChipText = YtLightBackground,
    textPrimary = YtLightTextPrimary,
    textSecondary = YtLightTextSecondary,
    accentBlue = YtLightBlue,
    divider = YtLightDivider,
    red = YtRed,
    progressRed = YtProgressRed,
    successGreen = YtSuccessGreen
)
