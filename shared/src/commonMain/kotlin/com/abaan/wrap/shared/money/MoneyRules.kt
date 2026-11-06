package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

/** Everything the UI shows about a job's money. All amounts are minor units (fils). */
data class MoneyBreakdown(
    /** Calendar days the job spans (start to end, inclusive). */
    val days: Int,
    /** Minutes from completed time logs. */
    val workedMinutes: Long,
    /** Minutes beyond the day length, summed over each worked day (DAY rate only). */
    val overtimeMinutes: Long,
    val base: Long,
    val overtime: Long,
    val kitRental: Long,
    val expenses: Long,
    /** Spends billed to this job, charged back to the client. */
    val billedSpends: Long,
    /** Everything before VAT. */
    val subtotal: Long,
    val vat: Long,
    /** subtotal + VAT: what the client pays. */
    val total: Long,
    val paid: Long,
    /** What is still owed; never negative. */
    val owed: Long,
    /** Amount paid beyond the total, if the client overpaid. */
    val credit: Long,
)

/**
 * The money rules:
 * - DAY: rate per day; HALF_DAY: rate / 2 per day; HOUR: hours x rate; FIXED: rate.
 * - Overtime (DAY only): hours beyond the day length x (rate / day length) x OT multiplier,
 *   worked out per calendar day from time logs.
 * - subtotal = base + overtime + kit rental + expenses + spends billed to the job.
 * - total = subtotal + VAT at the job's rate (rounded to the nearest fils); owed = total - payments.
 *
 * HOUR jobs with no time logs yet are estimated from the planned day length.
 */
object MoneyRules {

    fun breakdown(
        job: Job,
        timeLogs: List<TimeLog>,
        payments: List<Payment>,
        timeZone: TimeZone,
        billedSpends: List<Spend> = emptyList(),
    ): MoneyBreakdown =
        calculate(
            rateType = job.rateType,
            rate = job.rate,
            days = daysBetween(job.startDate, job.endDate),
            dayLengthHours = job.dayLengthHours,
            otMultiplier = job.otMultiplier,
            minutesPerDay = minutesPerDay(timeLogs, timeZone).values.toList(),
            kitRental = job.kitRental,
            expenses = job.expenses,
            paid = payments.sumOf { it.amount },
            billedSpends = billedSpends.sumOf { it.amount },
            vatRate = job.vatRate,
        )

    /**
     * Pure calculation used both for saved jobs and for the live preview while a job is being
     * entered. [minutesPerDay] holds worked minutes for each calendar day that has time logs.
     */
    fun calculate(
        rateType: RateType,
        rate: Long,
        days: Int,
        dayLengthHours: Double,
        otMultiplier: Double,
        minutesPerDay: List<Long>,
        kitRental: Long,
        expenses: Long,
        paid: Long,
        billedSpends: Long = 0,
        vatRate: Double = 0.0,
    ): MoneyBreakdown {
        val safeRate = rate.coerceAtLeast(0)
        val safeDays = days.coerceAtLeast(1)
        val dayLengthMinutes = (dayLengthHours * 60).roundToLong().coerceAtLeast(0)
        val workedMinutes = minutesPerDay.sumOf { it.coerceAtLeast(0) }

        val base = when (rateType) {
            RateType.DAY -> safeRate * safeDays
            RateType.HALF_DAY -> divideRounded(safeRate * safeDays, 2)
            RateType.HOUR -> {
                val minutes = if (minutesPerDay.isNotEmpty()) workedMinutes else safeDays * dayLengthMinutes
                divideRounded(safeRate * minutes, 60)
            }
            RateType.FIXED -> safeRate
        }

        val overtimeMinutes = if (rateType == RateType.DAY && dayLengthMinutes > 0) {
            minutesPerDay.sumOf { (it - dayLengthMinutes).coerceAtLeast(0) }
        } else {
            0L
        }
        val overtime = if (overtimeMinutes > 0) {
            overtimePay(safeRate, dayLengthMinutes, otMultiplier, overtimeMinutes)
        } else {
            0L
        }

        val safeKit = kitRental.coerceAtLeast(0)
        val safeExpenses = expenses.coerceAtLeast(0)
        val safeBilled = billedSpends.coerceAtLeast(0)
        val subtotal = base + overtime + safeKit + safeExpenses + safeBilled
        val vat = (subtotal * vatRate.coerceIn(0.0, 1.0)).roundToLong()
        val total = subtotal + vat
        val safePaid = paid.coerceAtLeast(0)

        return MoneyBreakdown(
            days = safeDays,
            workedMinutes = workedMinutes,
            overtimeMinutes = overtimeMinutes,
            base = base,
            overtime = overtime,
            kitRental = safeKit,
            expenses = safeExpenses,
            billedSpends = safeBilled,
            subtotal = subtotal,
            vat = vat,
            total = total,
            paid = safePaid,
            owed = (total - safePaid).coerceAtLeast(0),
            credit = (safePaid - total).coerceAtLeast(0),
        )
    }

    /** overtime = (minutes / 60) x (rate / dayLengthHours) x multiplier, rounded to the nearest fils. */
    fun overtimePay(rate: Long, dayLengthMinutes: Long, otMultiplier: Double, overtimeMinutes: Long): Long {
        if (dayLengthMinutes <= 0 || overtimeMinutes <= 0) return 0
        val multiplier = otMultiplier.coerceAtLeast(0.0)
        return (overtimeMinutes.toDouble() * rate * multiplier / dayLengthMinutes).roundToLong()
    }

    /** Calendar days from [start] to [end] inclusive; at least 1. */
    fun daysBetween(start: LocalDate, end: LocalDate): Int = (start.daysUntil(end) + 1).coerceAtLeast(1)

    /** Worked minutes per local calendar day (by clock-in date), from completed logs only. */
    fun minutesPerDay(timeLogs: List<TimeLog>, timeZone: TimeZone): Map<LocalDate, Long> =
        timeLogs
            .filter { it.clockOut != null && it.clockOut > it.clockIn }
            .groupBy { it.clockIn.toLocalDateTime(timeZone).date }
            .mapValues { (_, logs) -> logs.sumOf { (it.clockOut!! - it.clockIn).inWholeMinutes } }

    private fun divideRounded(value: Long, divisor: Long): Long = (value + divisor / 2) / divisor
}
