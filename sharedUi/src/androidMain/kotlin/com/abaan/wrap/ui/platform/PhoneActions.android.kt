package com.abaan.wrap.ui.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.abaan.wrap.files.SharedFiles
import com.abaan.wrap.ui.resources.Res
import com.abaan.wrap.ui.resources.no_app_to_open
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
actual fun rememberPhoneActions(): PhoneActions {
    val context = LocalContext.current
    val noApp = stringResource(Res.string.no_app_to_open)
    return remember(context, noApp) { AndroidPhoneActions(context, noApp) }
}

private class AndroidPhoneActions(private val context: Context, private val noApp: String) : PhoneActions {
    override fun openUrl(url: String, noAppMessage: String?) =
        startOrToast(Intent(Intent.ACTION_VIEW, Uri.parse(url)), noAppMessage ?: noApp)

    override fun openMaps(place: String) {
        try {
            context.startActivity(mapsIntent(place))
        } catch (_: ActivityNotFoundException) {
            // No maps app: fall back to Google Maps in the browser.
            openUrl("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.trim())}")
        }
    }

    override fun dial(phone: String) = startOrToast(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone.trim())}")))

    override fun shareFile(path: String, mimeType: String, subject: String, title: String) {
        if (!SharedFiles.share(context, File(path), mimeType, subject, title)) showMessage(noApp)
    }

    override fun showMessage(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    private fun startOrToast(intent: Intent, noAppMessage: String = noApp) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage(noAppMessage)
        }
    }
}

/** Intent that opens [place] in a maps app: the link itself, or a search for the address. */
fun mapsIntent(place: String): Intent {
    val trimmed = place.trim()
    val uri = if (isLink(trimmed)) Uri.parse(trimmed) else Uri.parse("geo:0,0?q=${Uri.encode(trimmed)}")
    return Intent(Intent.ACTION_VIEW, uri)
}
