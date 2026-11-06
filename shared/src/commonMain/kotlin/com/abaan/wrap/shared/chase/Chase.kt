package com.abaan.wrap.shared.chase

import com.abaan.wrap.shared.model.JobSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
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
    /** Days past the due date before the first reminder to chase. */
    const val NUDGE_AFTER_DAYS = 3
    /** A job isn't nudged again within this many days of being chased or nudged. */
    const val NUDGE_EVERY_DAYS = 7

    /** A job can be chased once the work is done and money is still owed. */
    fun canChase(summary: JobSummary): Boolean = summary.workDone && summary.money.owed > 0

    /**
     * Whether to remind the freelancer to chase [summary] today: it is overdue by at least
     * [NUDGE_AFTER_DAYS], and they haven't chased it, nor been reminded, in the last [NUDGE_EVERY_DAYS].
     */
    fun needsNudge(summary: JobSummary, lastChased: LocalDate?, lastNudged: LocalDate?, today: LocalDate): Boolean {
        val due = summary.job.dueDate ?: return false
        fun quietSince(date: LocalDate?) = date == null || date.plus(DatePeriod(days = NUDGE_EVERY_DAYS)) <= today
        return summary.isOverdue &&
            due.plus(DatePeriod(days = NUDGE_AFTER_DAYS)) <= today &&
            quietSince(lastChased) &&
            quietSince(lastNudged)
    }

    /** Whole days [summary] is past its due date (0 when it isn't due yet). */
    fun daysOverdue(summary: JobSummary, today: LocalDate): Int =
        summary.job.dueDate?.daysUntil(today)?.coerceAtLeast(0) ?: 0
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

/** A reminder shortly before call time on each day of a job. */
data class CallReminder(val date: LocalDate, val at: Instant)

object CallReminderRules {
    /** Reminders further ahead than this are scheduled later, as the date gets closer. */
    const val DAYS_AHEAD = 14

    fun callAt(date: LocalDate, callTime: LocalTime, timeZone: TimeZone): Instant =
        LocalDateTime(date, callTime).toInstant(timeZone)

    /**
     * The reminders still ahead for [summary]: one per job day, [minutesBefore] its call time.
     * None when the job has no call time, the work is done, or reminders are off ([minutesBefore] 0).
     */
    fun upcoming(summary: JobSummary, minutesBefore: Int, now: Instant, timeZone: TimeZone): List<CallReminder> {
        val callTime = summary.job.callTime ?: return emptyList()
        if (minutesBefore <= 0 || summary.workDone) return emptyList()
        val horizon = now + DAYS_AHEAD.days
        return generateSequence(summary.job.startDate) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it <= summary.job.endDate }
            .map { CallReminder(it, callAt(it, callTime, timeZone) - minutesBefore.minutes) }
            .filter { it.at > now && it.at < horizon }
            .toList()
    }
}
