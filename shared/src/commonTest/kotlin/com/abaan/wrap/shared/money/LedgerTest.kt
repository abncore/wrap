package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private val today = LocalDate(2026, 5, 15)
    private val clientA = Client(id = 1, name = "Sandstorm Films")
    private val clientB = Client(id = 2, name = "Falcon Productions")

    private fun job(
        id: Long,
        clientId: Long,
        start: LocalDate,
        end: LocalDate = start,
        status: JobStatus = JobStatus.UPCOMING,
        rate: Long = 1_000_00,
        dueDate: LocalDate? = null,
        callTime: LocalTime? = null,
    ) = Job(
        id = id,
        clientId = clientId,
        title = "Job $id",
        startDate = start,
        endDate = end,
        callTime = callTime,
        rateType = RateType.DAY,
        rate = rate,
        dayLengthHours = 10.0,
        otMultiplier = 1.5,
        dueDate = dueDate,
        status = status,
    )

    private fun pay(jobId: Long, amount: Long, date: LocalDate) =
        Payment(jobId = jobId, amount = amount, dateReceived = date, method = PaymentMethod.BANK)

    @Test
    fun summaryDerivesStatusFromDataAndDate() {
        val past = job(1, 1, LocalDate(2026, 5, 1), dueDate = LocalDate(2026, 5, 10))
        val summary = Ledger.summarize(past, clientA, emptyList(), emptyList(), today, tz)
        assertTrue(summary.workDone)
        assertEquals(JobStatus.OVERDUE, summary.status)
        assertTrue(summary.isUnpaid)
        assertTrue(summary.workDoneAutomatically)
    }

    @Test
    fun manuallyFinishedFutureJobCanBeUndone() {
        val future = job(1, 1, LocalDate(2026, 5, 20), status = JobStatus.DONE_UNPAID)
        val summary = Ledger.summarize(future, clientA, emptyList(), emptyList(), today, tz)
        assertTrue(summary.workDone)
        assertFalse(summary.workDoneAutomatically)
        assertFalse(summary.canClock)
    }

    @Test
    fun dashboardTotals() {
        val jobs = listOf(
            // Done, part paid, overdue: owes 600
            job(1, 1, LocalDate(2026, 4, 1), dueDate = LocalDate(2026, 5, 1), status = JobStatus.PART_PAID),
            // Done, unpaid, not due yet: owes 1,000
            job(2, 2, LocalDate(2026, 5, 10), dueDate = LocalDate(2026, 6, 10), status = JobStatus.DONE_UNPAID),
            // Paid in full
            job(3, 1, LocalDate(2026, 5, 2), status = JobStatus.PAID),
            // Today, upcoming with a deposit: deposit doesn't count as owed
            job(4, 2, today, callTime = LocalTime(7, 0)),
            // Next week
            job(5, 1, LocalDate(2026, 5, 22)),
        )
        val payments = listOf(
            pay(1, 400_00, LocalDate(2026, 4, 20)),
            pay(3, 1_000_00, LocalDate(2026, 5, 3)),
            pay(4, 200_00, LocalDate(2026, 5, 14)),
        )
        val summaries = Ledger.summarizeAll(jobs, listOf(clientA, clientB), emptyList(), payments, today, tz)
        val dashboard = Ledger.dashboard(summaries, payments, emptyList(), null, AppSettings(), today)

        assertEquals(1_600_00, dashboard.owedTotal)
        assertEquals(2, dashboard.unpaidCount)
        assertEquals(1_200_00, dashboard.receivedThisMonth)
        assertEquals(1, dashboard.overdueCount)
        assertEquals(600_00, dashboard.overdueTotal)
        assertEquals(4L, dashboard.todayJob?.job?.id)
        assertEquals(4L, dashboard.nextJob?.job?.id)
        assertEquals("AED", dashboard.currency)
        assertTrue(dashboard.hasAnyJobs)
    }

    @Test
    fun nextJobSkipsFinishedJobs() {
        val jobs = listOf(
            job(1, 1, today, status = JobStatus.DONE_UNPAID),
            job(2, 1, LocalDate(2026, 5, 20)),
        )
        val summaries = Ledger.summarizeAll(jobs, listOf(clientA), emptyList(), emptyList(), today, tz)
        val dashboard = Ledger.dashboard(summaries, emptyList(), emptyList(), null, AppSettings(), today)
        assertEquals(2L, dashboard.nextJob?.job?.id)
        // Today's job is already done, so there's nothing to clock in on.
        assertNull(dashboard.todayJob)
    }

    @Test
    fun emptyDashboard() {
        val dashboard = Ledger.dashboard(emptyList(), emptyList(), emptyList(), null, AppSettings(currency = "USD"), today)
        assertEquals(0, dashboard.owedTotal)
        assertNull(dashboard.nextJob)
        assertNull(dashboard.todayJob)
        assertFalse(dashboard.hasAnyJobs)
        assertEquals("USD", dashboard.currency)
    }

    @Test
    fun clientSummariesTotalOwedPerClient() {
        val jobs = listOf(
            job(1, 1, LocalDate(2026, 4, 1), dueDate = LocalDate(2026, 5, 1)),
            job(2, 1, LocalDate(2026, 5, 2), rate = 500_00),
            job(3, 2, LocalDate(2026, 6, 1)),
        )
        val payments = listOf(pay(2, 100_00, LocalDate(2026, 5, 5)))
        val summaries = Ledger.summarizeAll(jobs, listOf(clientA, clientB), emptyList(), payments, today, tz)
        val result = Ledger.clientSummaries(listOf(clientA, clientB), summaries).associateBy { it.client.id }

        assertEquals(1_400_00, result.getValue(1).owed)
        assertTrue(result.getValue(1).overdue)
        assertEquals(2, result.getValue(1).jobCount)
        assertEquals(0, result.getValue(2).owed)
        assertFalse(result.getValue(2).overdue)
    }
}
