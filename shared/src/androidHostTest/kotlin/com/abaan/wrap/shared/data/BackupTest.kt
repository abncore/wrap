package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.backup.BackupRepository
import com.abaan.wrap.shared.backup.InvalidBackupException
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.CrewContact
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.Profile
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock

class BackupTest {

    private class Repos(db: com.abaan.wrap.shared.db.WrapDatabase) {
        val io = Dispatchers.Unconfined
        val today = Today(Clock.System, TimeZone.of("Asia/Dubai"))
        val status = StatusUpdater(db, today)
        val clients = ClientRepository(db, io)
        val jobs = JobRepository(db, io, status)
        val payments = PaymentRepository(db, io, status)
        val spends = SpendRepository(db, io, status)
        val recurring = RecurringSpendRepository(db, io)
        val crew = CrewRepository(db, io)
        val settings = SettingsRepository(db, io)
        val profiles = ProfileRepository(db, io)
        val backup = BackupRepository(db, io, Clock.System)
    }

    @Test
    fun backupRestoresEverythingIntoAnotherPhone() = runTest {
        val phone = Repos(inMemoryDatabase())
        phone.settings.save(AppSettings(currency = "USD", monthlyBudget = 2_000_00, callReminderMinutes = 90))
        phone.profiles.save(Profile(name = "Abaan", bankDetails = "IBAN AE07", vatRegistered = true, nextInvoiceNumber = 12))
        val clientId = phone.clients.save(Client(name = "Falcon Productions", phone = "050 123 4567"))
        val jobId = phone.jobs.save(
            Job(
                clientId = clientId, title = "Desert shoot", startDate = LocalDate(2026, 3, 10), endDate = LocalDate(2026, 3, 11),
                callTime = LocalTime(6, 30), rateType = RateType.DAY, rate = 1_500_00, dayLengthHours = 10.0, otMultiplier = 1.5,
                unitBase = "Gate 3", vatRate = 0.05,
            ),
        )
        phone.payments.save(Payment(jobId = jobId, amount = 500_00, dateReceived = LocalDate(2026, 3, 20), method = PaymentMethod.BANK))
        val fuel = phone.spends.save(
            Spend(amount = 120_00, category = SpendCategory.FUEL, date = LocalDate(2026, 3, 10), jobId = jobId, receipt = "r.jpg"),
        )
        phone.recurring.setRepeating(fuel, repeat = true, today = LocalDate(2026, 3, 12))
        phone.crew.add(CrewContact(jobId = jobId, name = "Omar", role = "1st AD"))

        val text = phone.backup.export()
        val summary = phone.backup.summarize(text)
        assertEquals(1, summary.jobs)
        assertEquals(listOf("r.jpg"), summary.receipts)

        // A new phone that already has something in it: the backup replaces it.
        val newPhone = Repos(inMemoryDatabase())
        newPhone.clients.save(Client(name = "Old client"))
        newPhone.backup.restore(text)

        assertEquals(listOf("Falcon Productions"), newPhone.clients.observeClients().first().map { it.name })
        val job = newPhone.jobs.getJob(jobId)!!
        assertEquals("Gate 3", job.unitBase)
        assertEquals(clientId, job.clientId)
        assertEquals(0.05, job.vatRate)
        assertEquals(500_00, newPhone.payments.observeForJob(jobId).first().single().amount)
        assertEquals(jobId, newPhone.spends.getSpend(fuel)!!.jobId)
        assertEquals(1, newPhone.recurring.observeAll().first().size)
        assertEquals("Omar", newPhone.crew.observeForJob(jobId).first().single().name)
        assertEquals("USD", newPhone.settings.getSettings().currency)
        assertEquals(90, newPhone.settings.getSettings().callReminderMinutes)
        assertEquals(12, newPhone.profiles.getProfile().nextInvoiceNumber)
        // New rows after a restore don't collide with restored ids.
        assertTrue(newPhone.clients.save(Client(name = "Sandstorm")) > clientId)
    }

    @Test
    fun rejectsFilesThatAreNotBackups() = runTest {
        val repos = Repos(inMemoryDatabase())
        assertFailsWith<InvalidBackupException> { repos.backup.summarize("{\"hello\": 1}") }
        assertFailsWith<InvalidBackupException> { repos.backup.summarize("not json") }
        val future = repos.backup.export().replace("\"schemaVersion\": 10", "\"schemaVersion\": 99")
        assertFailsWith<InvalidBackupException> { repos.backup.restore(future) }
    }
}
