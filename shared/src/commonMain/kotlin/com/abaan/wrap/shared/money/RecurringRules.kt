package com.abaan.wrap.shared.money

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Monthly repeating costs. A cost set for the 31st lands on the last day of shorter months
 * and goes back to the 31st when the month has one.
 */
object RecurringRules {

    /** [dayOfMonth] in the month containing [month], moved back to the month's last day if needed. */
    fun dateInMonth(month: LocalDate, dayOfMonth: Int): LocalDate {
        val first = Insights.firstOfMonth(month)
        val last = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        return LocalDate(first.year, first.month, dayOfMonth.coerceIn(1, last.day))
    }

    /** The occurrence in the month after [date]. */
    fun nextAfter(date: LocalDate, dayOfMonth: Int): LocalDate =
        dateInMonth(Insights.firstOfMonth(date).plus(DatePeriod(months = 1)), dayOfMonth)

    /** Every occurrence from [nextDate] up to and including [today], one per month, oldest first. */
    fun dueDates(nextDate: LocalDate, dayOfMonth: Int, today: LocalDate): List<LocalDate> =
        generateSequence(nextDate) { nextAfter(it, dayOfMonth) }.takeWhile { it <= today }.toList()
}
