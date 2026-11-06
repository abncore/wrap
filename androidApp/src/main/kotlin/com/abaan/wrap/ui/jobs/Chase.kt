package com.abaan.wrap.ui.jobs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.abaan.wrap.R
import com.abaan.wrap.shared.chase.PhoneNumbers
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.money

/**
 * Opens WhatsApp (via a wa.me link) with a polite reminder about what [summary] still owes.
 * Without a usable phone number WhatsApp opens its contact picker with the message ready.
 */
fun launchChase(context: Context, summary: JobSummary, currency: String) {
    val client = summary.client
    // "Sara Ahmed" at a company -> "Hi Sara"; a bare company name is used as-is.
    val greetingName = when {
        client == null -> ""
        client.company.isNotBlank() -> client.name.trim().substringBefore(' ')
        else -> client.name.trim()
    }
    val job = summary.job
    val amount = money(summary.money.owed, currency)
    val message = job.dueDate?.let { due ->
        context.getString(R.string.chase_message_due, greetingName, job.title, job.startDate.formatMedium(), amount, due.formatMedium())
    } ?: context.getString(R.string.chase_message, greetingName, job.title, job.startDate.formatMedium(), amount)

    val digits = client?.phone?.let(PhoneNumbers::toWhatsAppDigits).orEmpty()
    val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.chase_no_app, Toast.LENGTH_LONG).show()
    }
}
