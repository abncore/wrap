package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.Profile
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.datetime.LocalDate

/** What an invoice line is for; the app turns this into words in the user's language. */
enum class InvoiceLineKind { BASE, OVERTIME, KIT_RENTAL, EXPENSES, SPEND }

/**
 * One line on an invoice. [quantity] is days (DAY / HALF_DAY), hours (HOUR, OVERTIME) or null;
 * [unitPrice] goes with it. SPEND lines carry the spend's [note], [category] and [date].
 */
data class InvoiceLine(
    val kind: InvoiceLineKind,
    val amount: Long,
    val quantity: Double? = null,
    val unitPrice: Long? = null,
    val note: String = "",
    val category: SpendCategory? = null,
    val date: LocalDate? = null,
)

/** Everything printed on an invoice for one job. Amounts are minor units. */
data class Invoice(
    val number: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate?,
    val currency: String,
    val profile: Profile,
    val client: Client?,
    val job: Job,
    val lines: List<InvoiceLine>,
    val subtotal: Long,
    /** 0 when the job has no VAT. */
    val vatRate: Double,
    val vat: Long,
    val total: Long,
    val paid: Long,
    val balanceDue: Long,
)

object InvoiceRules {
    /** UAE standard VAT rate. */
    const val UAE_VAT_RATE = 0.05

    /** "INV-" and 7 -> "INV-0007". */
    fun formatNumber(prefix: String, number: Long): String = prefix + number.toString().padStart(4, '0')

    fun build(summary: JobSummary, profile: Profile, number: String, issueDate: LocalDate, currency: String): Invoice {
        val job = summary.job
        val money = summary.money
        val base = when (job.rateType) {
            RateType.DAY -> InvoiceLine(InvoiceLineKind.BASE, money.base, quantity = money.days.toDouble(), unitPrice = job.rate)
            RateType.HALF_DAY -> InvoiceLine(InvoiceLineKind.BASE, money.base, quantity = money.days.toDouble(), unitPrice = job.rate / 2)
            RateType.HOUR -> {
                val minutes = if (money.workedMinutes > 0) money.workedMinutes else (money.days * job.dayLengthHours * 60).toLong()
                InvoiceLine(InvoiceLineKind.BASE, money.base, quantity = minutes / 60.0, unitPrice = job.rate)
            }
            RateType.FIXED -> InvoiceLine(InvoiceLineKind.BASE, money.base)
        }
        val lines = buildList {
            add(base)
            if (money.overtime > 0) add(InvoiceLine(InvoiceLineKind.OVERTIME, money.overtime, quantity = money.overtimeMinutes / 60.0))
            if (money.kitRental > 0) add(InvoiceLine(InvoiceLineKind.KIT_RENTAL, money.kitRental))
            if (money.expenses > 0) add(InvoiceLine(InvoiceLineKind.EXPENSES, money.expenses))
            summary.billedSpends.forEach { spend ->
                add(InvoiceLine(InvoiceLineKind.SPEND, spend.amount, note = spend.note, category = spend.category, date = spend.date))
            }
        }
        return Invoice(
            number = number,
            issueDate = issueDate,
            dueDate = job.dueDate,
            currency = currency,
            profile = profile,
            client = summary.client,
            job = job,
            lines = lines,
            subtotal = money.subtotal,
            vatRate = job.vatRate,
            vat = money.vat,
            total = money.total,
            paid = money.paid,
            balanceDue = money.owed,
        )
    }
}
