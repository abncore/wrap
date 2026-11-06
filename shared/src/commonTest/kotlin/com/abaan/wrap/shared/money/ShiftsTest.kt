package com.abaan.wrap.shared.money

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ShiftsTest {
    private val tz = TimeZone.of("Asia/Dubai")

    @Test
    fun daysOfIsInclusiveAndCrossesMonths() {
        assertEquals(
            listOf(LocalDate(2026, 1, 30), LocalDate(2026, 1, 31), LocalDate(2026, 2, 1)),
            Shifts.daysOf(LocalDate(2026, 1, 30), LocalDate(2026, 2, 1)),
        )
        assertTrue(Shifts.daysOf(LocalDate(2026, 2, 2), LocalDate(2026, 2, 1)).isEmpty())
    }

    @Test
    fun oneShiftPerDayAtTheStartTime() {
        val shifts = Shifts.fromHoursPerDay(LocalDate(2026, 3, 1), LocalDate(2026, 3, 2), LocalTime(6, 30), 11.5, tz)
        assertEquals(2, shifts.size)
        assertEquals(LocalDateTime(2026, 3, 1, 6, 30).toInstant(tz), shifts[0].first)
        assertEquals(11.hours + 30.minutes, shifts[0].second - shifts[0].first)
        assertEquals(LocalDateTime(2026, 3, 2, 6, 30).toInstant(tz), shifts[1].first)
    }

    @Test
    fun noShiftsForZeroHours() {
        assertTrue(Shifts.fromHoursPerDay(LocalDate(2026, 3, 1), LocalDate(2026, 3, 1), LocalTime(8, 0), 0.0, tz).isEmpty())
    }

    @Test
    fun singleShiftPastMidnightEndsNextDay() {
        val (start, end) = Shifts.single(LocalDate(2026, 3, 1), LocalTime(20, 0), LocalTime(4, 0), tz)
        assertEquals(8.hours, end - start)
        val (dayStart, dayEnd) = Shifts.single(LocalDate(2026, 3, 1), LocalTime(7, 0), LocalTime(19, 15), tz)
        assertEquals(12.hours + 15.minutes, dayEnd - dayStart)
    }
}
