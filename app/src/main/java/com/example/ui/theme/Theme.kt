package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Theme color schemes matching Color.kt
private val ClassicLightColors = lightColorScheme(
    primary = PurpleLightPrimary,
    secondary = PurpleLightSecondary,
    tertiary = PurpleLightTertiary,
    background = PurpleLightBg,
    surface = PurpleLightSurface
)

private val ClassicDarkColors = darkColorScheme(
    primary = PurpleDarkPrimary,
    secondary = PurpleDarkSecondary,
    tertiary = PurpleDarkTertiary,
    background = PurpleDarkBg,
    surface = PurpleDarkSurface
)

private val TealLightColors = lightColorScheme(
    primary = TealLightPrimary,
    secondary = TealLightSecondary,
    tertiary = TealLightTertiary,
    background = TealLightBg,
    surface = TealLightSurface
)

private val TealDarkColors = darkColorScheme(
    primary = TealDarkPrimary,
    secondary = TealDarkSecondary,
    tertiary = TealDarkTertiary,
    background = TealDarkBg,
    surface = TealDarkSurface
)

private val SunsetLightColors = lightColorScheme(
    primary = SunsetLightPrimary,
    secondary = SunsetLightSecondary,
    tertiary = SunsetLightTertiary,
    background = SunsetLightBg,
    surface = SunsetLightSurface
)

private val SunsetDarkColors = darkColorScheme(
    primary = SunsetDarkPrimary,
    secondary = SunsetDarkSecondary,
    tertiary = SunsetDarkTertiary,
    background = SunsetDarkBg,
    surface = SunsetDarkSurface
)

private val ForestLightColors = lightColorScheme(
    primary = ForestLightPrimary,
    secondary = ForestLightSecondary,
    tertiary = ForestLightTertiary,
    background = ForestLightBg,
    surface = ForestLightSurface
)

private val ForestDarkColors = darkColorScheme(
    primary = ForestDarkPrimary,
    secondary = ForestDarkSecondary,
    tertiary = ForestDarkTertiary,
    background = ForestDarkBg,
    surface = ForestDarkSurface
)

@Composable
fun MyApplicationTheme(
    themePreset: String = "classic",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreset.lowercase()) {
        "teal" -> if (darkTheme) TealDarkColors else TealLightColors
        "sunset" -> if (darkTheme) SunsetDarkColors else SunsetLightColors
        "forest" -> if (darkTheme) ForestDarkColors else ForestLightColors
        else -> if (darkTheme) ClassicDarkColors else ClassicLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
