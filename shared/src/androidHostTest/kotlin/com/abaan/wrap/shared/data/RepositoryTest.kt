package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class RepositoryTest {
    private val db = inMemoryDatabase()
    private val io = Dispatchers.Unconfined
    private val fixedClock = object : Clock {
        override fun now() = Instant.parse("2026-03-10T06:00:00Z")
    }

    private val clients = ClientRepository(db, io)
    private val jobs = JobRepository(db, io)
    private val payments = PaymentRepository(db, io)
    private val timeLogs = TimeLogRepository(db, io, fixedClock)
    private val settings = SettingsRepository(db, io)

    private fun job(clientId: Long) = Job(
        clientId = clientId,
        title = "Desert car commercial",
        role = "DIT",
        startDate = LocalDate(2026, 3, 10),
        endDate = LocalDate(2026, 3, 11),
        callTime = LocalTime(6, 30),
        location = "Al Qudra",
        rateType = RateType.DAY,
        rate = 1_500_00,
        dayLengthHours = 10.0,
        otMultiplier = 1.5,
    )

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
        val original = job(clientId).copy(kitRental = 350_00, expenses = 75_50, dueDate = LocalDate(2026, 4, 10), notes = "Bring RED kit")
        val id = jobs.save(original)
        assertEquals(original.copy(id = id), jobs.getJob(id))
    }

    @Test
    fun deletingJobRemovesItsLogsAndPayments() = runTest {
        val clientId = clients.save(Client(name = "Falcon Productions"))
        val jobId = jobs.save(job(clientId))
        timeLogs.clockIn(jobId)
        payments.save(Payment(jobId = jobId, amount = 500_00, dateReceived = LocalDate(2026, 3, 12), method = PaymentMethod.BANK))

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
}
