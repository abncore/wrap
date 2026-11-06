package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.RateType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarRulesTest {
    private val tz = TimeZone.of("Asia/Dubai")

    private fun job(id: Long, start: LocalDate, end: LocalDate = start, call: LocalTime? = null) = Job(
        id = id, clientId = 1, title = "Job $id", startDate = start, endDate = end, callTime = call,
        rateType = RateType.DAY, rate = 1_000_00, dayLengthHours = 10.0, otMultiplier = 1.5,
    )

    private fun summaries(vararg jobs: Job) =
        Ledger.summarizeAll(jobs.toList(), emptyList(), emptyList(), emptyList(), LocalDate(2026, 11, 1), tz)

    @Test
    fun gridCoversWholeWeeksAndMarksClashes() {
        val list = summaries(
            job(1, LocalDate(2026, 11, 9), LocalDate(2026, 11, 11)),
            job(2, LocalDate(2026, 11, 11), call = LocalTime(6, 0)),
        )
        // November 2026 starts on a Sunday; with Monday-first weeks the grid starts on 26 October.
        val grid = CalendarRules.monthGrid(LocalDate(2026, 11, 20), DayOfWeek.MONDAY, list)
        assertEquals(LocalDate(2026, 10, 26), grid.first().date)
        assertEquals(LocalDate(2026, 12, 6), grid.last().date)
        assertEquals(0, grid.size % 7)
        assertFalse(grid.first().inMonth)
        val eleventh = grid.single { it.date == LocalDate(2026, 11, 11) }
        assertTrue(eleventh.clash)
        assertEquals(listOf(2L, 1L), eleventh.jobs.map { it.job.id })
        assertEquals(listOf(LocalDate(2026, 11, 11)), CalendarRules.clashDays(LocalDate(2026, 11, 1), list))

        val sundayFirst = CalendarRules.monthGrid(LocalDate(2026, 11, 1), DayOfWeek.SUNDAY, list)
        assertEquals(LocalDate(2026, 11, 1), sundayFirst.first().date)
    }

    @Test
    fun eventTimesAreAllDayWithoutACallTime() {
        val allDay = CalendarRules.eventTimes(job(1, LocalDate(2026, 11, 9), LocalDate(2026, 11, 10)), tz)
        assertTrue(allDay.allDay)
        assertEquals(LocalDate(2026, 11, 9).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), allDay.start)
        assertEquals(LocalDate(2026, 11, 11).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), allDay.end)

        val timed = CalendarRules.eventTimes(job(2, LocalDate(2026, 11, 9), LocalDate(2026, 11, 10), LocalTime(6, 30)), tz)
        assertFalse(timed.allDay)
        assertEquals(LocalDateTime(2026, 11, 9, 6, 30).toInstant(tz).toEpochMilliseconds(), timed.start)
        assertEquals(LocalDateTime(2026, 11, 10, 16, 30).toInstant(tz).toEpochMilliseconds(), timed.end)
    }
}
