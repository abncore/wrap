package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.CrewContact
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EasterEggsTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private val falcon = Client(id = 1, name = "Falcon Productions")
    private val sandstorm = Client(id = 2, name = "Sandstorm Films")

    private fun job(id: Long, client: Client, day: Int, role: String, location: String, call: LocalTime?) = Job(
        id = id, clientId = client.id, title = "Job $id", role = role, location = location,
        startDate = LocalDate(2026, 3, day), endDate = LocalDate(2026, 3, day), callTime = call,
        rateType = RateType.DAY, rate = 1_000_00, dayLengthHours = 10.0, otMultiplier = 1.5, status = JobStatus.DONE_UNPAID,
    )

    private fun log(jobId: Long, day: Int, from: Int, to: Int) = TimeLog(
        jobId = jobId,
        clockIn = LocalDateTime(2026, 3, day, from, 0).toInstant(tz),
        clockOut = LocalDateTime(2026, 3, day, to, 0).toInstant(tz),
    )

    private val jobs = listOf(
        job(1, falcon, 2, "DIT", "Al Qudra", LocalTime(5, 30)),
        job(2, falcon, 3, "DIT", "Al Qudra", LocalTime(7, 0)),
        job(3, sandstorm, 4, "Gaffer", "https://maps.app.goo.gl/x", null),
    )
    private val logs = listOf(log(1, 2, 5, 21), log(2, 3, 7, 17))
    private val payments = listOf(
        Payment(jobId = 1, amount = 1_500_00, dateReceived = LocalDate(2026, 3, 20), method = PaymentMethod.BANK),
        Payment(jobId = 2, amount = 900_00, dateReceived = LocalDate(2026, 3, 21), method = PaymentMethod.CASH),
    )
    private val spends = listOf(
        Spend(amount = 40_00, category = SpendCategory.FOOD, date = LocalDate(2026, 3, 2)),
        Spend(amount = 150_00, category = SpendCategory.FUEL, date = LocalDate(2026, 3, 2)),
        Spend(amount = 60_00, category = SpendCategory.TRANSPORT, date = LocalDate(2026, 3, 3)),
    )

    private fun summaries() =
        Ledger.summarizeAll(jobs, listOf(falcon, sandstorm), logs, payments, LocalDate(2026, 4, 1), tz)

    @Test
    fun blooperReelPicksOutTheHighlights() {
        val reel = EasterEggs.blooperReel(summaries(), payments, spends, tz)
        assertEquals(26 * 60L, reel.minutesOnSet)
        assertEquals(3, reel.daysOnSet)
        assertEquals(16 * 60L, reel.longestShiftMinutes)
        assertEquals("Job 1", reel.longestShiftJob)
        assertEquals(LocalTime(5, 30), reel.earliestCall)
        assertEquals("Falcon Productions", reel.loyalClient)
        assertEquals(2, reel.loyalClientJobs)
        assertEquals(1_500_00, reel.biggestPayment)
        assertEquals(SpendCategory.FUEL, reel.topSpendCategory)
        assertTrue(EasterEggs.blooperReel(emptyList(), emptyList(), emptyList(), tz).isEmpty)
    }

    @Test
    fun endCreditsComeFromYourOwnWork() {
        val credits = EasterEggs.endCredits(summaries(), spends, listOf(CrewContact(jobId = 1, name = "Omar", role = "1st AD")))
        assertEquals(listOf("Falcon Productions", "Sandstorm Films"), credits.producers)
        assertEquals(listOf("DIT", "Gaffer"), credits.roles)
        // Pasted map links aren't place names.
        assertEquals(listOf("Al Qudra"), credits.locations)
        assertEquals(listOf("Omar · 1st AD"), credits.crew)
        assertEquals(40_00, credits.cateringSpent)
        assertEquals(210_00, credits.transportSpent)
        assertEquals(3, credits.jobs)
    }
}
