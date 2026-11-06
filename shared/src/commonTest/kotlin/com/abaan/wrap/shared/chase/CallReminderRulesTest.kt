package com.abaan.wrap.shared.chase

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.money.Ledger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallReminderRulesTest {
    private val tz = TimeZone.of("Asia/Dubai")

    private fun summary(callTime: LocalTime?, status: JobStatus = JobStatus.UPCOMING) = Ledger.summarize(
        job = Job(
            clientId = 1,
            title = "Shoot",
            startDate = LocalDate(2026, 3, 10),
            endDate = LocalDate(2026, 3, 12),
            callTime = callTime,
            rateType = RateType.DAY,
            rate = 1_000_00,
            dayLengthHours = 10.0,
            otMultiplier = 1.5,
            status = status,
        ),
        client = null,
        timeLogs = emptyList(),
        payments = emptyList(),
        today = LocalDate(2026, 3, 10),
        timeZone = tz,
    )

    @Test
    fun oneReminderPerRemainingJobDayBeforeCallTime() {
        val now = LocalDateTime(2026, 3, 10, 6, 0).toInstant(tz)
        val reminders = CallReminderRules.upcoming(summary(LocalTime(6, 30)), minutesBefore = 60, now = now, timeZone = tz)
        // Today's 5:30 reminder has already passed.
        assertEquals(listOf(LocalDate(2026, 3, 11), LocalDate(2026, 3, 12)), reminders.map { it.date })
        assertEquals(LocalDateTime(2026, 3, 11, 5, 30).toInstant(tz), reminders.first().at)
    }

    @Test
    fun noRemindersWithoutACallTimeWhenOffOrWhenDone() {
        val now = LocalDateTime(2026, 3, 1, 6, 0).toInstant(tz)
        assertTrue(CallReminderRules.upcoming(summary(null), 60, now, tz).isEmpty())
        assertTrue(CallReminderRules.upcoming(summary(LocalTime(7, 0)), 0, now, tz).isEmpty())
        assertTrue(CallReminderRules.upcoming(summary(LocalTime(7, 0), JobStatus.DONE_UNPAID), 60, now, tz).isEmpty())
    }
}
