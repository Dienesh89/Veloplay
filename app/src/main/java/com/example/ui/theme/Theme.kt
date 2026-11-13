package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

private val VeloDarkColorScheme = darkColorScheme(
    primary = YtRed,
    onPrimary = YtTextPrimary,
    primaryContainer = YtChipBackground,
    onPrimaryContainer = YtTextPrimary,
    secondary = YtBlue,
    onSecondary = YtBlack,
    secondaryContainer = YtChipElevated,
    onSecondaryContainer = YtTextPrimary,
    tertiary = YtTextSecondary,
    onTertiary = YtBlack,
    background = YtBlack,
    onBackground = YtTextPrimary,
    surface = YtSurface,
    onSurface = YtTextPrimary,
    surfaceVariant = YtSheetSurface,
    onSurfaceVariant = YtTextSecondary,
    surfaceContainer = YtSheetSurface,
    surfaceContainerHigh = YtChipBackground,
    surfaceContainerLow = YtBlack,
    outline = YtDivider,
    outlineVariant = YtDivider
)

private val VeloLightColorScheme = lightColorScheme(
    primary = YtRed,
    onPrimary = YtLightBackground,
    primaryContainer = YtLightChipBackground,
    onPrimaryContainer = YtLightTextPrimary,
    secondary = YtLightBlue,
    onSecondary = YtLightBackground,
    secondaryContainer = YtLightChipElevated,
    onSecondaryContainer = YtLightTextPrimary,
    tertiary = YtLightTextSecondary,
    onTertiary = YtLightBackground,
    background = YtLightBackground,
    onBackground = YtLightTextPrimary,
    surface = YtLightSurface,
    onSurface = YtLightTextPrimary,
    surfaceVariant = YtLightSheetSurface,
    onSurfaceVariant = YtLightTextSecondary,
    surfaceContainer = YtLightSheetSurface,
    surfaceContainerHigh = YtLightChipBackground,
    surfaceContainerLow = YtLightBackground,
    outline = YtLightDivider,
    outlineVariant = YtLightDivider
)

val LocalVeloColors = staticCompositionLocalOf { DarkVeloColors }

object VeloTheme {
    val colors: VeloColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVeloColors.current
}

@Composable
fun VeloPlayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    forceDarkSystemBars: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val veloColors = if (useDark) DarkVeloColors else LightVeloColors
    val colorScheme = if (useDark) VeloDarkColorScheme else VeloLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                val lightIconsNeeded = !useDark && !forceDarkSystemBars
                controller.isAppearanceLightStatusBars = lightIconsNeeded
                controller.isAppearanceLightNavigationBars = lightIconsNeeded
            }
        }
    }

    CompositionLocalProvider(LocalVeloColors provides veloColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VeloTypography,
            content = content
        )
    }
}
