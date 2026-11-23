package com.abaan.wrap.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberTakePhoto(onResult: (taken: Boolean) -> Unit): (target: String) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onResult)
    return { target -> launcher.launch(Uri.parse(target)) }
}

@Composable
actual fun rememberPickPhoto(onPicked: (uri: String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
actual fun rememberCreateDocument(mimeType: String, onCreated: (uri: String) -> Unit): (suggestedName: String) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        uri?.let { onCreated(it.toString()) }
    }
    return launcher::launch
}

@Composable
actual fun rememberOpenDocument(onOpened: (uri: String) -> Unit): (mimeTypes: List<String>) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onOpened(it.toString()) }
    }
    return { mimeTypes -> launcher.launch(mimeTypes.toTypedArray()) }
}

@Composable
actual fun rememberPermissionsRequest(onResult: (granted: Boolean) -> Unit): (permissions: List<String>) -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        onResult(results.values.all { it })
    }
    return { permissions -> launcher.launch(permissions.toTypedArray()) }
}

@Composable
actual fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
actual fun is24HourClock(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
