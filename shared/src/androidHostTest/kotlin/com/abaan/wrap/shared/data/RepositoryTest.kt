package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class RepositoryTest {
    private val tz = TimeZone.of("Asia/Dubai")
    private var now: Instant = LocalDateTime(2026, 3, 10, 6, 0).toInstant(tz)
    private val clock = object : Clock {
        override fun now() = this@RepositoryTest.now
    }

    private val db = inMemoryDatabase()
    private val io = Dispatchers.Unconfined
    private val today = Today(clock, tz)
    private val statusUpdater = StatusUpdater(db, today)

    private val clients = ClientRepository(db, io)
    private val jobs = JobRepository(db, io, statusUpdater)
    private val payments = PaymentRepository(db, io, statusUpdater)
    private val timeLogs = TimeLogRepository(db, io, clock, statusUpdater)
    private val settings = SettingsRepository(db, io)
    private val ledger = LedgerRepository(clients, jobs, timeLogs, payments, settings, today)

    private fun job(clientId: Long) = Job(
        clientId = clientId,
        title = "Desert car commercial",
        role = "DIT",
        startDate = LocalDate(2026, 3, 10),
        endDate = LocalDate(2026, 3, 10),
        callTime = LocalTime(6, 30),
        location = "Al Qudra",
        rateType = RateType.DAY,
        rate = 1_500_00,
        dayLengthHours = 10.0,
        otMultiplier = 1.5,
    )

    private fun payment(jobId: Long, amount: Long) =
        Payment(jobId = jobId, amount = amount, dateReceived = LocalDate(2026, 3, 12), method = PaymentMethod.BANK)

    @Test
    fun settingsHaveDefaultsAndCanBeSaved() = runTest {
        assertEquals(AppSettings(), settings.getSettings())
        val updated = AppSettings(defaultRate = 2_000_00, dayLengthHours = 12.0, currency = "USD", remindersEnabled = false)
        settings.save(updated)
        assertEquals(updated, settings.observeSettings().first())
    }

    @Test
    fun clientRoundTripsAndUpdates() = runTest {
        val id = clients.save(Client(name = " Sandstorm Films ", phone = "050 123 4567", paymentTermsDays = 45))
        val saved = clients.getClient(id)!!
        assertEquals("Sandstorm Films", saved.name)
        assertEquals(45, saved.paymentTermsDays)
        assertNull(saved.defaultRate)

        clients.save(saved.copy(defaultRate = 1_800_00))
        assertEquals(1_800_00, clients.observeClients().first().single().defaultRate)
    }

    @Test
    fun clientWithJobsCannotBeDeleted() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId))
        assertFalse(clients.delete(clientId))

        jobs.delete(jobId)
        assertTrue(clients.delete(clientId))
        assertTrue(clients.observeClients().first().isEmpty())
    }

    @Test
    fun jobRoundTripsAllFields() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val original = job(clientId).copy(
            endDate = LocalDate(2026, 3, 11),
            kitRental = 350_00,
            expenses = 75_50,
            dueDate = LocalDate(2026, 4, 10),
            notes = "Bring RED kit",
        )
        val id = jobs.save(original)
        assertEquals(original.copy(id = id), jobs.getJob(id))
    }

    @Test
    fun deletingJobRemovesItsLogsAndPayments() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId))
        timeLogs.clockIn(jobId)
        payments.save(payment(jobId, 500_00))

        jobs.delete(jobId)

        assertNull(jobs.getJob(jobId))
        assertTrue(timeLogs.observeAll().first().isEmpty())
        assertTrue(payments.observeAll().first().isEmpty())
    }

    @Test
    fun clockInClosesAnyRunningLog() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val first = jobs.save(job(clientId))
        val second = jobs.save(job(clientId).copy(title = "Second job"))

        timeLogs.clockIn(first)
        timeLogs.clockIn(second)

        val logs = timeLogs.observeAll().first()
        assertEquals(2, logs.size)
        assertEquals(1, logs.count { it.clockOut == null })
        assertEquals(second, timeLogs.observeOpenLog().first()?.jobId)

        assertEquals(second, timeLogs.clockOut())
        assertNull(timeLogs.observeOpenLog().first())
        assertNull(timeLogs.clockOut())
    }

    @Test
    fun statusMovesFromUpcomingToPaidAutomatically() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId))
        assertEquals(JobStatus.UPCOMING, jobs.getJob(jobId)!!.status)

        // 06:00 - 18:00: 12h on a 10h day = 2h OT at 150/h x 1.5 = 450
        timeLogs.clockIn(jobId)
        now = LocalDateTime(2026, 3, 10, 18, 0).toInstant(tz)
        timeLogs.clockOut()
        assertEquals(JobStatus.DONE_UNPAID, jobs.getJob(jobId)!!.status)
        val summary = ledger.observeJobSummary(jobId).first()!!
        assertEquals(1_950_00, summary.money.total)

        payments.save(payment(jobId, 1_000_00))
        assertEquals(JobStatus.PART_PAID, jobs.getJob(jobId)!!.status)

        val lastId = payments.save(payment(jobId, 950_00))
        assertEquals(JobStatus.PAID, jobs.getJob(jobId)!!.status)

        payments.delete(lastId)
        assertEquals(JobStatus.PART_PAID, jobs.getJob(jobId)!!.status)
    }

    @Test
    fun loggingAFinishedJobStoresShiftsAndStatus() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val start = LocalDateTime(2026, 3, 1, 7, 0).toInstant(tz)
        val end = LocalDateTime(2026, 3, 1, 18, 0).toInstant(tz)
        val jobId = jobs.saveFinished(
            job(clientId).copy(startDate = LocalDate(2026, 3, 1), endDate = LocalDate(2026, 3, 1)),
            listOf(start to end),
        )
        assertEquals(JobStatus.DONE_UNPAID, jobs.getJob(jobId)!!.status)
        assertEquals(1, timeLogs.observeForJob(jobId).first().size)
        // 1h OT at 150/h x 1.5 = 225
        assertEquals(225_00, ledger.observeJobSummary(jobId).first()!!.money.overtime)
    }

    @Test
    fun markingNotDoneReturnsToUpcoming() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId).copy(startDate = LocalDate(2026, 3, 20), endDate = LocalDate(2026, 3, 20)))
        jobs.setWorkDone(jobId, true)
        assertEquals(JobStatus.DONE_UNPAID, jobs.getJob(jobId)!!.status)
        jobs.setWorkDone(jobId, false)
        assertEquals(JobStatus.UPCOMING, jobs.getJob(jobId)!!.status)
    }

    @Test
    fun dashboardReflectsLiveData() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId))
        jobs.setWorkDone(jobId, true)
        payments.save(payment(jobId, 500_00))

        val dashboard = ledger.observeDashboard().first()
        assertEquals(1_000_00, dashboard.owedTotal)
        assertEquals(500_00, dashboard.receivedThisMonth)

        val clientSummary = ledger.observeClientSummaries().first().single()
        assertEquals(1_000_00, clientSummary.owed)
    }
}
