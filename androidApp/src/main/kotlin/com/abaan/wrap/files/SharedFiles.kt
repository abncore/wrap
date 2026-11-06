package com.abaan.wrap.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.abaan.wrap.R
import java.io.File

/** Files made to hand to other apps (invoices, reports, backups), kept in the cache and shared via FileProvider. */
object SharedFiles {
    private const val DIRECTORY = "shared"

    fun file(context: Context, name: String): File =
        File(File(context.cacheDir, DIRECTORY).apply { mkdirs() }, name)

    /** Opens the share sheet for [file], e.g. to send it on WhatsApp or save it to Drive. */
    fun share(context: Context, file: File, mimeType: String, subject: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(send, chooserTitle))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_LONG).show()
        }
    }
}
