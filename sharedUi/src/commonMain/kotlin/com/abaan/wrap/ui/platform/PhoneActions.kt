package com.abaan.wrap.ui.platform

import androidx.compose.runtime.Composable
import com.abaan.wrap.shared.chase.PhoneNumbers

/** Hands things to other apps: links, maps, the dialer and the share sheet. */
interface PhoneActions {
    /** Opens [url] in whichever app handles it, showing [noAppMessage] (or a general one) when nothing can. */
    fun openUrl(url: String, noAppMessage: String? = null)

    /** Opens [place] in a maps app: a pasted link as-is, or a search for the address. */
    fun openMaps(place: String)

    fun dial(phone: String)

    /** Opens the share sheet for the file at [path], e.g. to send it on WhatsApp or save it to Drive. */
    fun shareFile(path: String, mimeType: String, subject: String, title: String)

    /** A short message that disappears on its own. */
    fun showMessage(text: String)
}

@Composable
expect fun rememberPhoneActions(): PhoneActions

fun PhoneActions.openWhatsApp(phone: String) {
    val digits = PhoneNumbers.toWhatsAppDigits(phone) ?: return
    openUrl("https://wa.me/$digits")
}

/** True when [place] is a pasted link (e.g. a Google Maps share link) rather than an address. */
fun isLink(place: String): Boolean = place.trim().let { it.startsWith("http://") || it.startsWith("https://") }

/** Percent-encodes [value] for use in a URL query, as UTF-8. */
fun urlEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val c = byte.toInt().toChar()
        if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in "-_.~") {
            append(c)
        } else {
            append('%')
            append(HEX[(byte.toInt() shr 4) and 0xF])
            append(HEX[byte.toInt() and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
