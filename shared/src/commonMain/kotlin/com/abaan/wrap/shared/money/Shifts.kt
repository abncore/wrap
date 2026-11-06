package com.abaan.wrap.shared.money

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Builds time logs for work that wasn't clocked live. */
object Shifts {
    /** Every calendar day from [start] to [end] inclusive. */
    fun daysOf(start: LocalDate, end: LocalDate): List<LocalDate> =
        generateSequence(start) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= end }.toList()

    /**
     * One shift per day from [start] to [end], each starting at [startTime] and lasting
     * [hoursPerDay]. Returns an empty list when [hoursPerDay] is not positive.
     */
    fun fromHoursPerDay(
        start: LocalDate,
        end: LocalDate,
        startTime: LocalTime,
        hoursPerDay: Double,
        timeZone: TimeZone,
    ): List<Pair<Instant, Instant>> {
        val minutes = (hoursPerDay * 60).roundToLong()
        if (minutes <= 0) return emptyList()
        return daysOf(start, end).map { day ->
            val clockIn = LocalDateTime(day, startTime).toInstant(timeZone)
            clockIn to clockIn + minutes.minutes
        }
    }

    /** A single shift on [date] between [from] and [to]; [to] at or before [from] means it ran past midnight. */
    fun single(date: LocalDate, from: LocalTime, to: LocalTime, timeZone: TimeZone): Pair<Instant, Instant> {
        val clockIn = LocalDateTime(date, from).toInstant(timeZone)
        val endDate = if (to <= from) date.plus(DatePeriod(days = 1)) else date
        return clockIn to LocalDateTime(endDate, to).toInstant(timeZone)
    }
}
