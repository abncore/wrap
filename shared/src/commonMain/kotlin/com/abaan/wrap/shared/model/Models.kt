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
 * A booking. Money fields ([rate], [kitRental], [expenses]) are in minor units.
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

/** Money spent on work (fuel, kit, food...). [amount] is in minor units. */
data class Spend(
    val id: Long = NEW_ID,
    val amount: Long,
    val category: SpendCategory,
    val date: LocalDate,
    val note: String = "",
)

/** App-wide defaults. [defaultRate] is in minor units; [currency] is an ISO 4217 code. */
data class AppSettings(
    val defaultRate: Long = 0,
    val dayLengthHours: Double = 10.0,
    val otMultiplier: Double = 1.5,
    val paymentTermsDays: Int = 30,
    val currency: String = DEFAULT_CURRENCY,
    val remindersEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_CURRENCY = "AED"
        /** Currencies with two decimal places, so stored minor units mean the same thing in each. */
        val SUPPORTED_CURRENCIES = listOf("AED", "SAR", "QAR", "USD", "EUR", "GBP", "INR")
    }
}
