package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobStatusRulesTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private val start = LocalDate(2026, 3, 10)
    private val end = LocalDate(2026, 3, 11)

    private fun log(day: Int, closed: Boolean = true) = TimeLog(
        jobId = 1,
        clockIn = LocalDateTime(2026, 3, day, 7, 0).toInstant(tz),
        clockOut = if (closed) LocalDateTime(2026, 3, day, 17, 0).toInstant(tz) else null,
    )

    @Test
    fun upcomingJobIsNotDone() {
        assertFalse(JobStatusRules.isWorkDone(JobStatus.UPCOMING, end, emptyList(), start, tz))
    }

    @Test
    fun clockingOutBeforeTheLastDayDoesNotFinishAMultiDayJob() {
        assertFalse(JobStatusRules.isWorkDone(JobStatus.UPCOMING, end, listOf(log(10)), start, tz))
    }

    @Test
    fun clockingOutOnTheLastDayFinishesTheJob() {
        assertTrue(JobStatusRules.isWorkDone(JobStatus.UPCOMING, end, listOf(log(10), log(11)), end, tz))
    }

    @Test
    fun stillClockedInOnTheLastDayIsNotDone() {
        assertFalse(JobStatusRules.isWorkDone(JobStatus.UPCOMING, end, listOf(log(11, closed = false)), end, tz))
    }

    @Test
    fun jobIsDoneOnceItsEndDateHasPassed() {
        assertTrue(JobStatusRules.isWorkDone(JobStatus.UPCOMING, end, emptyList(), LocalDate(2026, 3, 12), tz))
    }

    @Test
    fun jobMarkedDoneStaysDone() {
        assertTrue(JobStatusRules.isWorkDone(JobStatus.DONE_UNPAID, end, emptyList(), start, tz))
    }

    @Test
    fun storedStatusFollowsPayments() {
        assertEquals(JobStatus.UPCOMING, JobStatusRules.storedStatus(workDone = false, total = 1000, paid = 0))
        assertEquals(JobStatus.UPCOMING, JobStatusRules.storedStatus(workDone = false, total = 1000, paid = 1000))
        assertEquals(JobStatus.DONE_UNPAID, JobStatusRules.storedStatus(workDone = true, total = 1000, paid = 0))
        assertEquals(JobStatus.PART_PAID, JobStatusRules.storedStatus(workDone = true, total = 1000, paid = 400))
        assertEquals(JobStatus.PAID, JobStatusRules.storedStatus(workDone = true, total = 1000, paid = 1000))
        assertEquals(JobStatus.PAID, JobStatusRules.storedStatus(workDone = true, total = 1000, paid = 1200))
    }

    @Test
    fun doneJobWorthNothingIsPaid() {
        assertEquals(JobStatus.PAID, JobStatusRules.storedStatus(workDone = true, total = 0, paid = 0))
    }

    @Test
    fun overdueOnlyAfterTheDueDateWithMoneyOwed() {
        val due = LocalDate(2026, 4, 10)
        assertFalse(JobStatusRules.isOverdue(JobStatus.DONE_UNPAID, 500, due, due))
        assertTrue(JobStatusRules.isOverdue(JobStatus.DONE_UNPAID, 500, due, LocalDate(2026, 4, 11)))
        assertTrue(JobStatusRules.isOverdue(JobStatus.PART_PAID, 500, due, LocalDate(2026, 4, 11)))
        assertFalse(JobStatusRules.isOverdue(JobStatus.PAID, 0, due, LocalDate(2026, 4, 11)))
        assertFalse(JobStatusRules.isOverdue(JobStatus.UPCOMING, 500, due, LocalDate(2026, 4, 11)))
        assertFalse(JobStatusRules.isOverdue(JobStatus.DONE_UNPAID, 500, null, LocalDate(2026, 4, 11)))
    }

    @Test
    fun displayStatusSwapsInOverdue() {
        val due = LocalDate(2026, 4, 10)
        assertEquals(JobStatus.OVERDUE, JobStatusRules.displayStatus(JobStatus.PART_PAID, 100, due, LocalDate(2026, 5, 1)))
        assertEquals(JobStatus.PART_PAID, JobStatusRules.displayStatus(JobStatus.PART_PAID, 100, due, LocalDate(2026, 4, 1)))
    }

    @Test
    fun defaultDueDateAddsPaymentTerms() {
        assertEquals(LocalDate(2026, 4, 10), JobStatusRules.defaultDueDate(LocalDate(2026, 3, 11), 30))
        assertEquals(LocalDate(2026, 3, 11), JobStatusRules.defaultDueDate(LocalDate(2026, 3, 11), -5))
    }
}
