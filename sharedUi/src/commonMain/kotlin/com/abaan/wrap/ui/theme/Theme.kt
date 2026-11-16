package com.abaan.wrap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.abaan.wrap.ui.platform.wallpaperColorScheme
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Teal40,
    secondary = Sand40,
    tertiary = Slate40,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    secondary = Sand80,
    tertiary = Slate80,
)

/** The hidden "Night shoot" look: always dark, with amber accents like a work light. */
private val NightShootColors = darkColorScheme(
    primary = Color(0xFFFFB951),
    onPrimary = Color(0xFF432C00),
    primaryContainer = Color(0xFF5F4100),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDDC2A1),
    secondaryContainer = Color(0xFF56432A),
    onSecondaryContainer = Color(0xFFFADEBC),
    tertiary = Color(0xFFB9CEA1),
    background = Color(0xFF100E0B),
    surface = Color(0xFF100E0B),
    surfaceContainer = Color(0xFF1E1A15),
    surfaceContainerHighest = Color(0xFF332D25),
)

private val baseTypography = Typography()

/** Slightly heavier headings than the Material default so amounts read at a glance. */
private val WrapTypography = baseTypography.copy(
    displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun WrapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    nightShoot: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = darkTheme || nightShoot
    val wallpaper = if (dynamicColor && !nightShoot) wallpaperColorScheme(darkTheme) else null
    val colorScheme = when {
        nightShoot -> NightShootColors
        wallpaper != null -> wallpaper
        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WrapTypography,
            content = content,
        )
    }
}
