package com.abaan.wrap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.abaan.wrap.ui.platform.LocalWrapStyle
import com.abaan.wrap.ui.platform.WrapStyle
import com.abaan.wrap.ui.platform.platformStyle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.Font

private val LightColors = lightColorScheme(
    primary = InkText,
    onPrimary = Paper,
    primaryContainer = PaperHighest,
    onPrimaryContainer = InkText,
    inversePrimary = PaperText,
    secondary = InkMuted,
    onSecondary = PaperCard,
    secondaryContainer = PaperHighest,
    onSecondaryContainer = InkText,
    tertiary = PaidGreenDeep,
    onTertiary = PaperCard,
    tertiaryContainer = Color(0xFFDDF3E6),
    onTertiaryContainer = Color(0xFF0B5132),
    background = Paper,
    onBackground = InkText,
    surface = Paper,
    onSurface = InkText,
    surfaceVariant = PaperHigh,
    onSurfaceVariant = InkMuted,
    surfaceTint = InkText,
    inverseSurface = Color(0xFF1C1C20),
    inverseOnSurface = PaperText,
    error = LateRedDeep,
    onError = PaperCard,
    errorContainer = Color(0xFFFBE4E2),
    onErrorContainer = Color(0xFF7A1414),
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    surfaceBright = PaperCard,
    surfaceDim = PaperDim,
    surfaceContainerLowest = PaperCard,
    surfaceContainerLow = PaperLow,
    surfaceContainer = PaperCard,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
)

private val DarkColors = darkColorScheme(
    primary = PaperText,
    onPrimary = Ink,
    primaryContainer = InkHighest,
    onPrimaryContainer = PaperText,
    inversePrimary = InkText,
    secondary = PaperSoft,
    onSecondary = Ink,
    secondaryContainer = InkHighest,
    onSecondaryContainer = PaperText,
    tertiary = PaidGreen,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF1E3329),
    onTertiaryContainer = PaidGreen,
    background = Ink,
    onBackground = PaperText,
    surface = Ink,
    onSurface = PaperText,
    surfaceVariant = InkHigh,
    onSurfaceVariant = PaperMuted,
    surfaceTint = PaperText,
    inverseSurface = PaperText,
    inverseOnSurface = InkText,
    error = LateRed,
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF3A1A1C),
    onErrorContainer = Color(0xFFFFB4AE),
    outline = InkOutline,
    outlineVariant = InkHighest,
    surfaceBright = InkBright,
    surfaceDim = Ink,
    surfaceContainerLowest = InkLowest,
    surfaceContainerLow = InkLow,
    surfaceContainer = InkCard,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHighest,
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

/** Rounder than the Material default: soft cards and pill buttons, like the Wrap logo. */
private val WrapShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Inter for text and Inter Display for headings and big amounts. Headings use tabular figures
 * so amounts line up and don't jump as they change.
 */
@Composable
private fun wrapTypography(): Typography {
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    val display = FontFamily(
        Font(Res.font.interdisplay_semibold, FontWeight.SemiBold),
    )
    val base = Typography()

    fun TextStyle.heading(tracking: Float) = copy(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = tracking.em,
        fontFeatureSettings = "tnum",
    )
    fun TextStyle.text(weight: FontWeight? = null, figures: Boolean = false) = copy(
        fontFamily = inter,
        fontWeight = weight ?: fontWeight,
        fontFeatureSettings = if (figures) "tnum" else fontFeatureSettings,
    )

    return Typography(
        displayLarge = base.displayLarge.heading(-0.02f),
        displayMedium = base.displayMedium.heading(-0.02f),
        displaySmall = base.displaySmall.heading(-0.02f),
        headlineLarge = base.headlineLarge.heading(-0.015f),
        headlineMedium = base.headlineMedium.heading(-0.015f),
        headlineSmall = base.headlineSmall.heading(-0.01f),
        titleLarge = base.titleLarge.text(FontWeight.SemiBold, figures = true),
        titleMedium = base.titleMedium.text(FontWeight.SemiBold, figures = true),
        titleSmall = base.titleSmall.text(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.text(),
        bodyMedium = base.bodyMedium.text(),
        bodySmall = base.bodySmall.text(),
        labelLarge = base.labelLarge.text(FontWeight.Medium),
        labelMedium = base.labelMedium.text(FontWeight.Medium),
        labelSmall = base.labelSmall.text(FontWeight.Medium),
    )
}

@Composable
fun WrapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    nightShoot: Boolean = false,
    style: WrapStyle = platformStyle,
    content: @Composable () -> Unit,
) {
    val dark = darkTheme || nightShoot
    val ios = style == WrapStyle.IOS
    val colorScheme = when {
        nightShoot -> NightShootColors
        ios -> if (darkTheme) IosDarkColors else IosLightColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    val statusColors = when {
        ios && !nightShoot -> if (dark) IosDarkStatusColors else IosLightStatusColors
        dark -> DarkStatusColors
        else -> LightStatusColors
    }

    CompositionLocalProvider(LocalStatusColors provides statusColors, LocalWrapStyle provides style) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (ios) IosTypography else wrapTypography(),
            shapes = if (ios) IosShapes else WrapShapes,
            content = content,
        )
    }
}
