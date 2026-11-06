package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes

/** One square of the month grid. [inMonth] is false for the days padding out the first and last week. */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val jobs: List<JobSummary>,
) {
    /** Two or more bookings on the same day. */
    val clash: Boolean get() = jobs.size > 1
}

/** When a job sits in the phone's calendar: all day when it has no call time. Times are epoch milliseconds. */
data class CalendarEventTimes(val allDay: Boolean, val start: Long, val end: Long)

object CalendarRules {

    /** Whole weeks covering [month]'s month, starting on [firstDayOfWeek], with the jobs on each day. */
    fun monthGrid(month: LocalDate, firstDayOfWeek: DayOfWeek, summaries: List<JobSummary>): List<CalendarDay> {
        val first = Insights.firstOfMonth(month)
        val last = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val leading = (first.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
        val trailing = (firstDayOfWeek.isoDayNumber + 6 - last.dayOfWeek.isoDayNumber + 7) % 7
        val start = first.minus(DatePeriod(days = leading))
        val end = last.plus(DatePeriod(days = trailing))
        return generateSequence(start) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it <= end }
            .map { date -> CalendarDay(date, date.month == first.month, jobsOn(date, summaries)) }
            .toList()
    }

    /** Jobs booked on [date], by call time. */
    fun jobsOn(date: LocalDate, summaries: List<JobSummary>): List<JobSummary> =
        summaries.filter { date in it.job.startDate..it.job.endDate }
            .sortedWith(compareBy(nullsLast()) { it.job.callTime })

    /** Days in [month]'s month with two or more bookings. */
    fun clashDays(month: LocalDate, summaries: List<JobSummary>): List<LocalDate> {
        val first = Insights.firstOfMonth(month)
        return generateSequence(first) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it.month == first.month }
            .filter { jobsOn(it, summaries).size > 1 }
            .toList()
    }

    /**
     * From call time on the first day to the end of the working day on the last; all-day events
     * (which calendars keep at UTC midnight) when there is no call time.
     */
    fun eventTimes(job: Job, timeZone: TimeZone): CalendarEventTimes {
        val callTime = job.callTime
        return if (callTime == null) {
            CalendarEventTimes(
                allDay = true,
                start = job.startDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                end = job.endDate.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            )
        } else {
            val start = LocalDateTime(job.startDate, callTime).toInstant(timeZone)
            val lastCall = LocalDateTime(job.endDate, callTime).toInstant(timeZone)
            val end = lastCall + (job.dayLengthHours * 60).roundToLong().coerceAtLeast(60).minutes
            CalendarEventTimes(allDay = false, start = start.toEpochMilliseconds(), end = end.toEpochMilliseconds())
        }
    }
}
