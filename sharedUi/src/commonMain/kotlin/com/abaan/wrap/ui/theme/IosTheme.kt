package com.abaan.wrap.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The iPhone look: Apple's system colours on grouped grey backgrounds with white cards (black
// and dark grey at night), the system blue for anything you can tap, and Apple's type sizes in
// the system font (SF Pro). Green, orange and red keep their Wrap meaning: paid, owed, late.

internal val IosLightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EFFF),
    onPrimaryContainer = Color(0xFF00397A),
    inversePrimary = Color(0xFF0A84FF),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EB),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF248A3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF6E3),
    onTertiaryContainer = Color(0xFF1B5E2B),
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE9E9EB),
    onSurfaceVariant = Color(0xFF8A8A8E),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E2),
    onErrorContainer = Color(0xFF8A1A13),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFE5E5EA),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF2F2F7),
    surfaceContainerHighest = Color(0xFFE9E9EB),
)

internal val IosDarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0B2E55),
    onPrimaryContainer = Color(0xFF9DCBFF),
    inversePrimary = Color(0xFF007AFF),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0F3D1E),
    onTertiaryContainer = Color(0xFF7EE29A),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8D8D93),
    surfaceTint = Color.Transparent,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF3B1513),
    onErrorContainer = Color(0xFFFFB4AE),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF2C2C2E),
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF161618),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = Color(0xFF2C2C2E),
)

internal val IosLightStatusColors = StatusColors(
    success = Color(0xFF248A3D),
    successContainer = Color(0xFFDDF6E3),
    onSuccessContainer = Color(0xFF1B5E2B),
    warning = Color(0xFFC76A00),
    warningContainer = Color(0xFFFFEFD9),
    onWarningContainer = Color(0xFF7A4100),
)

internal val IosDarkStatusColors = StatusColors(
    success = Color(0xFF30D158),
    successContainer = Color(0xFF0F3D1E),
    onSuccessContainer = Color(0xFF7EE29A),
    warning = Color(0xFFFF9F0A),
    warningContainer = Color(0xFF3D2A0A),
    onWarningContainer = Color(0xFFFFC266),
)

/** iOS uses smaller, tighter corners than Wrap's Android look. */
internal val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** Apple's text sizes (Large Title 34, Title 28/22/20, Headline 17, Body 17, Footnote 13...). */
internal val IosTypography: Typography = run {
    val system = FontFamily.Default
    fun style(size: Int, line: Int, weight: FontWeight, figures: Boolean = false) = TextStyle(
        fontFamily = system,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        fontFeatureSettings = if (figures) "tnum" else null,
    )
    Typography(
        displayLarge = style(48, 56, FontWeight.Bold, figures = true),
        displayMedium = style(40, 48, FontWeight.Bold, figures = true),
        displaySmall = style(34, 41, FontWeight.Bold, figures = true),
        headlineLarge = style(34, 41, FontWeight.Bold, figures = true),
        headlineMedium = style(28, 34, FontWeight.Bold, figures = true),
        headlineSmall = style(22, 28, FontWeight.Bold, figures = true),
        titleLarge = style(20, 25, FontWeight.SemiBold, figures = true),
        titleMedium = style(17, 22, FontWeight.SemiBold, figures = true),
        titleSmall = style(15, 20, FontWeight.SemiBold),
        bodyLarge = style(17, 22, FontWeight.Normal),
        bodyMedium = style(15, 20, FontWeight.Normal),
        bodySmall = style(13, 18, FontWeight.Normal),
        labelLarge = style(15, 20, FontWeight.Medium),
        labelMedium = style(12, 16, FontWeight.Medium),
        labelSmall = style(11, 13, FontWeight.Medium),
    )
}
