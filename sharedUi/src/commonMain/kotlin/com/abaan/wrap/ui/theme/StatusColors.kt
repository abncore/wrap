package com.abaan.wrap.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colours Material 3 doesn't define: paid (success) and part-paid (warning). */
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
    success = Color(0xFF1B6C3A),
    successContainer = Color(0xFFC6EFCF),
    onSuccessContainer = Color(0xFF00210D),
    warning = Color(0xFFB26A00),
    warningContainer = Color(0xFFFFE08A),
    onWarningContainer = Color(0xFF251A00),
)

internal val DarkStatusColors = StatusColors(
    success = Color(0xFF8FD8A2),
    successContainer = Color(0xFF00522A),
    onSuccessContainer = Color(0xFFC6EFCF),
    warning = Color(0xFFFFB951),
    warningContainer = Color(0xFF5A4300),
    onWarningContainer = Color(0xFFFFE08A),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
