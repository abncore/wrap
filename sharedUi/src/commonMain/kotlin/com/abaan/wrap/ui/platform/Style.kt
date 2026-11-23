package com.abaan.wrap.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** Which platform's look the screens follow: Material on Android, Apple's on iOS. */
enum class WrapStyle { MATERIAL, IOS }

expect val platformStyle: WrapStyle

val LocalWrapStyle = staticCompositionLocalOf { platformStyle }
