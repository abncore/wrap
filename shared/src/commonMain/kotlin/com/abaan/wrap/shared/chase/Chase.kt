package com.abaan.wrap.shared.chase

import com.abaan.wrap.shared.model.JobSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** Phone number handling for wa.me links. */
object PhoneNumbers {
    /** UAE country code, used for local numbers like 050 123 4567. */
    const val DEFAULT_COUNTRY_CODE = "971"

    /**
     * Turns what the user typed into the digits wa.me expects (country code, no "+", no
     * leading zero). Returns null when there are too few digits to be a phone number.
     *
     * - "+971 50 123 4567" / "00971501234567" -> "971501234567"
     * - "050 123 4567" (local, leading zero) -> "971501234567"
     */
    fun toWhatsAppDigits(input: String, defaultCountryCode: String = DEFAULT_COUNTRY_CODE): String? {
        val trimmed = input.trim()
        var digits = trimmed.filter { it.isDigit() }
        if (digits.length < MIN_DIGITS) return null
        digits = when {
            trimmed.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> defaultCountryCode + digits.drop(1)
            digits.length <= LOCAL_NUMBER_MAX -> defaultCountryCode + digits
            else -> digits
        }
        return digits.takeIf { it.length >= MIN_DIGITS }
    }

    private const val MIN_DIGITS = 7
    /** Local UAE numbers without the leading zero have 8–9 digits. */
    private const val LOCAL_NUMBER_MAX = 9
}

object ChaseRules {
    /** A job can be chased once the work is done and money is still owed. */
    fun canChase(summary: JobSummary): Boolean = summary.workDone && summary.money.owed > 0
}

/** When to remind the freelancer about a job: 8 PM the night before. */
object ReminderRules {
    val REMINDER_TIME = LocalTime(20, 0)

    fun reminderAt(startDate: LocalDate, timeZone: TimeZone): Instant =
        LocalDateTime(startDate.minus(DatePeriod(days = 1)), REMINDER_TIME).toInstant(timeZone)

    /** Only jobs that haven't been worked yet and whose reminder time is still ahead. */
    fun shouldRemind(summary: JobSummary, now: Instant, timeZone: TimeZone): Boolean =
        !summary.workDone && reminderAt(summary.job.startDate, timeZone) > now
}
