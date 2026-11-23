package com.abaan.wrap.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colours Material 3 doesn't define: paid (success, green) and owed (warning, amber). */
@Immutable
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val LightStatusColors = StatusColors(
    success = PaidGreenDeep,
    successContainer = Color(0xFFDDF3E6),
    onSuccessContainer = Color(0xFF0B5132),
    warning = OwedAmberDeep,
    warningContainer = Color(0xFFFCEBD2),
    onWarningContainer = Color(0xFF6E4000),
)

internal val DarkStatusColors = StatusColors(
    success = PaidGreen,
    successContainer = Color(0xFF1E3329),
    onSuccessContainer = PaidGreen,
    warning = OwedAmber,
    warningContainer = Color(0xFF3A2D1A),
    onWarningContainer = OwedAmber,
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
