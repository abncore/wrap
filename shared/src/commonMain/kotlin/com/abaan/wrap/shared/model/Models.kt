package com.abaan.wrap.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Id used for entities that have not been saved yet. */
const val NEW_ID: Long = 0L

/**
 * A production company or person the freelancer works for.
 *
 * [paymentTermsDays] and [defaultRate] are optional overrides of [AppSettings];
 * [defaultRate] is in minor units.
 */
data class Client(
    val id: Long = NEW_ID,
    val name: String,
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val paymentTermsDays: Int? = null,
    val defaultRate: Long? = null,
    val notes: String = "",
)

/**
 * A booking. Money fields ([rate], [kitRental], [expenses]) are in minor units. [unitBase] and
 * [parking] come from the call sheet; like [location] they can be an address or a maps link.
 * [vatRate] (0.05 = 5%) is added on top of the job's total. [invoiceNumber] is set once an invoice
 * has been made, and never changed by saving the job.
 * [status] is the stored lifecycle status; see `JobStatusRules` for how it is kept up to date
 * and how the displayed status (including OVERDUE) is derived.
 */
data class Job(
    val id: Long = NEW_ID,
    val clientId: Long,
    val title: String,
    val role: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val callTime: LocalTime? = null,
    val location: String = "",
    val rateType: RateType,
    val rate: Long,
    val dayLengthHours: Double,
    val otMultiplier: Double,
    val kitRental: Long = 0,
    val expenses: Long = 0,
    val dueDate: LocalDate? = null,
    val status: JobStatus = JobStatus.UPCOMING,
    val notes: String = "",
    val unitBase: String = "",
    val parking: String = "",
    val vatRate: Double = 0.0,
    val invoiceNumber: String? = null,
)

/** The freelancer's own details, printed on invoices. */
data class Profile(
    val name: String = "",
    val businessName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    /** UAE Tax Registration Number, when VAT registered. */
    val trn: String = "",
    val bankDetails: String = "",
    /** New jobs add VAT by default. */
    val vatRegistered: Boolean = false,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
) {
    /** The name invoices are from: the business name, or the person's name without one. */
    val displayName: String get() = businessName.ifBlank { name }
}

/** One entry in a job's chase log. */
data class ChaseLog(
    val jobId: Long,
    val at: Instant,
    val kind: ChaseKind,
)

/** Someone on a job's call sheet. */
data class CrewContact(
    val id: Long = NEW_ID,
    val jobId: Long,
    val name: String,
    val role: String = "",
    val phone: String = "",
)

/** A clock-in / clock-out pair. [clockOut] is null while the freelancer is still on the clock. */
data class TimeLog(
    val id: Long = NEW_ID,
    val jobId: Long,
    val clockIn: Instant,
    val clockOut: Instant? = null,
)

/** Money received for a job. [amount] is in minor units. */
data class Payment(
    val id: Long = NEW_ID,
    val jobId: Long,
    val amount: Long,
    val dateReceived: LocalDate,
    val method: PaymentMethod,
    val note: String = "",
)

/**
 * Money spent on work (fuel, kit, food...). [amount] is in minor units. [recurringId] is the monthly
 * cost that added it, if any. A spend with a [jobId] is billed to that job: it is added to the job's
 * total so the client pays it back. [receipt] is the receipt photo's file name, empty when there is none.
 */
data class Spend(
    val id: Long = NEW_ID,
    val amount: Long,
    val category: SpendCategory,
    val date: LocalDate,
    val note: String = "",
    val recurringId: Long? = null,
    val jobId: Long? = null,
    val receipt: String = "",
)

/** A cost that repeats every month on [dayOfMonth]; [nextDate] is when it is next added. */
data class RecurringSpend(
    val id: Long = NEW_ID,
    val amount: Long,
    val category: SpendCategory,
    val note: String = "",
    val dayOfMonth: Int,
    val nextDate: LocalDate,
)

/**
 * App-wide defaults. [defaultRate] and [monthlyBudget] are in minor units; [currency] is an ISO 4217
 * code. A [monthlyBudget] of 0 means no budget.
 */
data class AppSettings(
    val defaultRate: Long = 0,
    val dayLengthHours: Double = 10.0,
    val otMultiplier: Double = 1.5,
    val paymentTermsDays: Int = 30,
    val currency: String = DEFAULT_CURRENCY,
    val remindersEnabled: Boolean = true,
    val monthlyBudget: Long = 0,
    val budgetAlertsEnabled: Boolean = true,
    /** Minutes before call time to send a reminder; 0 means off. */
    val callReminderMinutes: Int = 60,
    /** Remind about overdue payments that haven't been chased for a week. */
    val chaseRemindersEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_CURRENCY = "AED"
        /** Currencies with two decimal places, so stored minor units mean the same thing in each. */
        val SUPPORTED_CURRENCIES = listOf("AED", "SAR", "QAR", "USD", "EUR", "GBP", "INR")
        /** Choices for [callReminderMinutes]. */
        val CALL_REMINDER_OPTIONS = listOf(0, 30, 60, 90, 120, 180)
    }
}
