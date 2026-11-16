package com.abaan.wrap.ui.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// Screens and pickers owned by the phone. Each returns a function that opens it; the result
// comes back through the callback. Files are passed around as URI strings.

/** The camera, saving a photo to a target from [ReceiptPhotos.newCameraTarget]. */
@Composable
expect fun rememberTakePhoto(onResult: (taken: Boolean) -> Unit): (target: String) -> Unit

/** The photo picker, images only. */
@Composable
expect fun rememberPickPhoto(onPicked: (uri: String) -> Unit): () -> Unit

/** Lets the user choose where to save a new file of [mimeType], starting from a suggested name. */
@Composable
expect fun rememberCreateDocument(mimeType: String, onCreated: (uri: String) -> Unit): (suggestedName: String) -> Unit

/** Lets the user pick an existing file of one of the given types. */
@Composable
expect fun rememberOpenDocument(onOpened: (uri: String) -> Unit): (mimeTypes: List<String>) -> Unit

/** Asks for runtime permissions; reports whether all were granted. */
@Composable
expect fun rememberPermissionsRequest(onResult: (granted: Boolean) -> Unit): (permissions: List<String>) -> Unit

/** Asks once, on first launch, to show notifications where the phone requires permission. */
@Composable
expect fun RequestNotificationPermission()

/** True when the phone shows times on a 24-hour clock. */
@Composable
expect fun is24HourClock(): Boolean

/** Colours taken from the wallpaper where the phone supports it, otherwise null. */
@Composable
expect fun wallpaperColorScheme(dark: Boolean): ColorScheme?
