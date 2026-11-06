package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportsTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private val falcon = Client(id = 1, name = "Falcon Productions")
    private val sandstorm = Client(id = 2, name = "Sandstorm Films")

    private fun job(id: Long, client: Client, end: LocalDate, rate: Long, vat: Double = 0.0) = Job(
        id = id, clientId = client.id, title = "Job $id", startDate = end, endDate = end, rateType = RateType.FIXED,
        rate = rate, dayLengthHours = 10.0, otMultiplier = 1.5, status = JobStatus.DONE_UNPAID, vatRate = vat,
    )

    private fun pay(jobId: Long, amount: Long, date: LocalDate) =
        Payment(jobId = jobId, amount = amount, dateReceived = date, method = PaymentMethod.BANK)

    private val jobs = listOf(
        job(1, falcon, LocalDate(2025, 12, 20), 2_000_00),
        job(2, falcon, LocalDate(2026, 2, 1), 1_000_00, vat = 0.05),
        job(3, sandstorm, LocalDate(2026, 3, 1), 3_000_00),
    )
    private val payments = listOf(
        // Paid in January for December's job: counts as received in 2026.
        pay(1, 2_000_00, LocalDate(2026, 1, 10)),
        pay(2, 1_050_00, LocalDate(2026, 2, 20)),
        pay(3, 1_000_00, LocalDate(2026, 3, 30)),
    )
    private val spends = listOf(
        Spend(id = 1, amount = 300_00, category = SpendCategory.FUEL, date = LocalDate(2026, 2, 3)),
        Spend(id = 2, amount = 100_00, category = SpendCategory.KIT, date = LocalDate(2026, 3, 3), note = "Batteries, V-mount", jobId = 3),
        Spend(id = 3, amount = 50_00, category = SpendCategory.FOOD, date = LocalDate(2025, 6, 3)),
    )

    private fun summaries() = Ledger.summarizeAll(jobs, listOf(falcon, sandstorm), emptyList(), payments, LocalDate(2026, 11, 7), tz, spends)

    @Test
    fun yearCountsMoneyByWhenItArrivedAndJobsByWhenTheyFinished() {
        val report = Reports.yearReport(2026, summaries(), payments, spends, "AED")
        assertEquals(4_050_00, report.received)
        assertEquals(1_050_00 + 3_100_00, report.invoiced)
        assertEquals(50_00, report.vatCharged)
        assertEquals(400_00, report.spent)
        assertEquals(100_00, report.billedSpent)
        assertEquals(2_100_00, report.owed)
        assertEquals(3_650_00, report.net)
        assertEquals(listOf("Falcon Productions" to 3_050_00L, "Sandstorm Films" to 1_000_00L), report.byClient.map { it.clientName to it.received })
        assertEquals(2, report.byClient.first().jobCount)
        assertEquals(SpendCategory.FUEL, report.byCategory.first().category)
        assertEquals(12, report.months.size)
        assertEquals(1_050_00, report.months[1].earned)
        assertEquals(listOf(2026, 2025), Reports.years(summaries(), payments, spends, 2026))
    }

    @Test
    fun csvRowsListIncomeAndSpendingOldestFirst() {
        val rows = Reports.rows(2026, summaries(), payments, spends)
        assertEquals(listOf(2_000_00L, -300_00L, 1_050_00L, -100_00L, 1_000_00L), rows.map { it.amount })
        assertEquals("Job 3", rows[3].billedToJob)
        assertEquals("\"Batteries, V-mount\"", Csv.field(rows[3].description))
        assertEquals("a,\"say \"\"hi\"\"\"", Csv.line(listOf("a", "say \"hi\"")))
        assertEquals("-1.05", Csv.amount(-105))
        assertEquals("2000.00", Csv.amount(2_000_00))
    }
}
