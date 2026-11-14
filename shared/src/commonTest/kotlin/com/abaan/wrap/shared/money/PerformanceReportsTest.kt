package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PerformanceReportsTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private val today = LocalDate(2026, 6, 15)
    private val falcon = Client(id = 1, name = "Falcon Productions")
    private val sandstorm = Client(id = 2, name = "Sandstorm Films")

    private fun job(id: Long, client: Client, start: LocalDate, days: Int, rate: Long, due: LocalDate?, role: String = "DIT", status: JobStatus = JobStatus.DONE_UNPAID) = Job(
        id = id, clientId = client.id, title = "Job $id", role = role, startDate = start,
        endDate = LocalDate.fromEpochDays(start.toEpochDays() + days - 1), rateType = RateType.DAY, rate = rate,
        dayLengthHours = 10.0, otMultiplier = 1.5, dueDate = due, status = status,
    )

    private fun pay(jobId: Long, amount: Long, date: LocalDate) =
        Payment(jobId = jobId, amount = amount, dateReceived = date, method = PaymentMethod.BANK)

    private val jobs = listOf(
        // Falcon: paid 10 days after wrap, on time.
        job(1, falcon, LocalDate(2026, 1, 10), days = 2, rate = 1_500_00, due = LocalDate(2026, 2, 10)),
        // Falcon: paid late, in two parts, 50 days after wrap.
        job(2, falcon, LocalDate(2026, 3, 1), days = 1, rate = 1_000_00, due = LocalDate(2026, 3, 31)),
        // Sandstorm: not paid, 70 days overdue.
        job(3, sandstorm, LocalDate(2026, 3, 5), days = 3, rate = 2_000_00, due = LocalDate(2026, 4, 6), role = "Gaffer"),
        // Sandstorm: owed but not due yet.
        job(4, sandstorm, LocalDate(2026, 6, 1), days = 1, rate = 800_00, due = LocalDate(2026, 7, 1), role = "Gaffer"),
        // Next month: not worked yet.
        job(5, falcon, LocalDate(2026, 7, 1), days = 1, rate = 1_500_00, due = null, status = JobStatus.UPCOMING),
    )
    private val payments = listOf(
        pay(1, 3_000_00, LocalDate(2026, 1, 21)),
        pay(2, 400_00, LocalDate(2026, 3, 20)),
        pay(2, 600_00, LocalDate(2026, 4, 20)),
    )

    private fun summaries() = Ledger.summarizeAll(jobs, listOf(falcon, sandstorm), emptyList(), payments, today, tz)

    @Test
    fun clientPerformanceShowsHowFastEachClientPays() {
        val report = PerformanceReports.clients(listOf(falcon, sandstorm), summaries(), InsightPeriod.ALL_TIME, today)
        assertEquals(listOf("Falcon Productions", "Sandstorm Films"), report.map { it.client.name })

        val f = report[0]
        assertEquals(2, f.jobs)
        assertEquals(3, f.daysWorked)
        assertEquals(4_000_00, f.received)
        assertEquals(1.0, f.shareOfIncome)
        assertEquals(1_333_33, f.earningsPerDay)
        assertEquals(1_250_00, f.averageDayRate)
        // (10 + 50) / 2
        assertEquals(30, f.averageDaysToPay)
        assertEquals(0.5, f.onTimeShare)
        assertEquals(LocalDate(2026, 1, 10), f.firstJob)

        val s = report[1]
        assertEquals(6_800_00, s.owed)
        assertEquals(6_000_00, s.overdue)
        assertNull(s.averageDaysToPay)
        assertNull(s.onTimeShare)
    }

    @Test
    fun agingGroupsWhatIsOwedByHowLate() {
        val aging = PerformanceReports.aging(summaries(), today)
        assertEquals(6_800_00, aging.total)
        assertEquals(6_000_00, aging.byBucket[AgeBucket.DAYS_61_90])
        assertEquals(800_00, aging.byBucket[AgeBucket.NOT_DUE])
        assertEquals(0, aging.byBucket[AgeBucket.OVER_90])
        assertEquals(listOf(3L, 4L), aging.jobs.map { it.summary.job.id })
        assertEquals(70, aging.jobs.first().daysOverdue)
        assertEquals(70, aging.clients.single().oldestDays)
        assertEquals(AgeBucket.OVER_90, PerformanceReports.bucketFor(91))
        assertEquals(AgeBucket.DAYS_1_30, PerformanceReports.bucketFor(1))
    }

    @Test
    fun workReportTotalsDaysRatesAndRoles() {
        val work = PerformanceReports.work(summaries(), InsightPeriod.THIS_YEAR, today)
        assertEquals(4, work.jobs)
        assertEquals(7, work.daysWorked)
        assertEquals(3_000_00 + 1_000_00 + 6_000_00 + 800_00, work.billed)
        assertEquals(listOf("Gaffer", "DIT"), work.byRole.map { it.role })
        assertEquals(12, work.months.size)
        assertEquals(LocalDate(2026, 6, 1), work.months.last().month)
        assertEquals(LocalDate(2026, 3, 1), work.busiestMonth?.month)
        assertEquals(1_542_86, work.earningsPerDay)
    }
}
