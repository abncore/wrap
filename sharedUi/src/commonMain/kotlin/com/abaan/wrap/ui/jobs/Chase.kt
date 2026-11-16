package com.abaan.wrap.ui.jobs

import com.abaan.wrap.shared.chase.PhoneNumbers
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.platform.PhoneActions
import com.abaan.wrap.ui.platform.urlEncode
import com.abaan.wrap.ui.resources.Res
import com.abaan.wrap.ui.resources.chase_message
import com.abaan.wrap.ui.resources.chase_message_due
import com.abaan.wrap.ui.resources.chase_no_app
import org.jetbrains.compose.resources.getString

/**
 * Opens WhatsApp (via a wa.me link) with a polite reminder about what [summary] still owes.
 * Without a usable phone number WhatsApp opens its contact picker with the message ready.
 */
suspend fun launchChase(actions: PhoneActions, summary: JobSummary, currency: String) {
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
        getString(Res.string.chase_message_due, greetingName, job.title, job.startDate.formatMedium(), amount, due.formatMedium())
    } ?: getString(Res.string.chase_message, greetingName, job.title, job.startDate.formatMedium(), amount)

    val digits = client?.phone?.let(PhoneNumbers::toWhatsAppDigits).orEmpty()
    actions.openUrl("https://wa.me/$digits?text=${urlEncode(message)}", getString(Res.string.chase_no_app))
}
