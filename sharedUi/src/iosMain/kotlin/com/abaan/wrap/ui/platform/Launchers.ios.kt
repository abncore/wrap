package com.abaan.wrap.ui.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

// The camera, photo and file pickers come with receipts and backups on iOS. Until then the
// screens that use them are hidden by PlatformFeatures, so these do nothing.

@Composable
actual fun rememberTakePhoto(onResult: (taken: Boolean) -> Unit): (target: String) -> Unit = { onResult(false) }

@Composable
actual fun rememberPickPhoto(onPicked: (uri: String) -> Unit): () -> Unit = {}

@Composable
actual fun rememberCreateDocument(mimeType: String, onCreated: (uri: String) -> Unit): (suggestedName: String) -> Unit = {}

@Composable
actual fun rememberOpenDocument(onOpened: (uri: String) -> Unit): (mimeTypes: List<String>) -> Unit = {}

@Composable
actual fun rememberPermissionsRequest(onResult: (granted: Boolean) -> Unit): (permissions: List<String>) -> Unit =
    { onResult(false) }

// Reminders aren't on iOS yet, so there is nothing to ask for.
@Composable
actual fun RequestNotificationPermission() = Unit

/** The region's hour format has no AM/PM marker on a 24-hour clock. */
@Composable
actual fun is24HourClock(): Boolean =
    NSDateFormatter.dateFormatFromTemplate("j", options = 0u, locale = NSLocale.currentLocale)?.contains('a') == false

// iOS has no wallpaper colours; Wrap uses its own palette.
@Composable
actual fun wallpaperColorScheme(dark: Boolean): ColorScheme? = null
