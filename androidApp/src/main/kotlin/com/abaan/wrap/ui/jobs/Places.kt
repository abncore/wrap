package com.abaan.wrap.ui.jobs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.abaan.wrap.R
import com.abaan.wrap.shared.chase.PhoneNumbers

/** True when [place] is a pasted link (e.g. a Google Maps share link) rather than an address. */
fun isLink(place: String): Boolean = place.trim().let { it.startsWith("http://") || it.startsWith("https://") }

/** Intent that opens [place] in a maps app: the link itself, or a search for the address. */
fun mapsIntent(place: String): Intent {
    val trimmed = place.trim()
    val uri = if (isLink(trimmed)) Uri.parse(trimmed) else Uri.parse("geo:0,0?q=${Uri.encode(trimmed)}")
    return Intent(Intent.ACTION_VIEW, uri)
}

fun openMaps(context: Context, place: String) {
    try {
        context.startActivity(mapsIntent(place))
    } catch (_: ActivityNotFoundException) {
        // No maps app: fall back to Google Maps in the browser.
        val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place.trim())}")
        startOrToast(context, Intent(Intent.ACTION_VIEW, web))
    }
}

fun dial(context: Context, phone: String) {
    startOrToast(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone.trim())}")))
}

fun openWhatsApp(context: Context, phone: String) {
    val digits = PhoneNumbers.toWhatsAppDigits(phone) ?: return
    startOrToast(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
}

private fun startOrToast(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_LONG).show()
    }
}
