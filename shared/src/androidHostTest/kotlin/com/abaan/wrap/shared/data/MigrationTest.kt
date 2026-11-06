package com.abaan.wrap.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class MigrationTest {
    private val io = Dispatchers.Unconfined

    @Test
    fun upgradingFromVersionOneKeepsDataAndAddsSpendingAndBudget() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Build the current schema, then remove what later versions added to get a version 1 database.
        WrapDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE SpendEntity", 0)
        driver.execute(null, "DROP TABLE RecurringSpendEntity", 0)
        driver.execute(null, "DROP TABLE CrewContactEntity", 0)
        driver.execute(null, "DROP TABLE ChaseLogEntity", 0)
        driver.execute(null, "DROP TABLE ProfileEntity", 0)
        driver.execute(null, "DROP TABLE CalendarEventEntity", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN calendarSync", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN appLock", 0)
        driver.execute(null, "ALTER TABLE JobEntity DROP COLUMN invoiceNumber", 0)
        driver.execute(null, "ALTER TABLE JobEntity DROP COLUMN vatRate", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN chaseReminders", 0)
        driver.execute(null, "ALTER TABLE JobEntity DROP COLUMN unitBase", 0)
        driver.execute(null, "ALTER TABLE JobEntity DROP COLUMN parking", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN callReminderMinutes", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN monthlyBudget", 0)
        driver.execute(null, "ALTER TABLE SettingsEntity DROP COLUMN budgetAlerts", 0)
        driver.execute(null, "UPDATE SettingsEntity SET currency = 'USD', defaultRate = 120000 WHERE id = 1", 0)
        driver.execute(null, "INSERT INTO ClientEntity(name, company, phone, email, notes) VALUES ('Sandstorm Films', '', '', '', '')", 0)

        WrapDatabase.Schema.migrate(driver, 1, WrapDatabase.Schema.version)

        val db = WrapDatabase(driver)
        assertEquals(10L, WrapDatabase.Schema.version)
        assertEquals("Sandstorm Films", ClientRepository(db, io).observeClients().first().single().name)

        val settings = SettingsRepository(db, io)
        assertEquals(AppSettings(defaultRate = 1_200_00, currency = "USD"), settings.getSettings())
        settings.save(settings.getSettings().copy(monthlyBudget = 3_000_00, budgetAlertsEnabled = false))
        assertEquals(3_000_00, settings.getSettings().monthlyBudget)

        val spends = SpendRepository(db, io, StatusUpdater(db, Today(Clock.System, TimeZone.UTC)))
        spends.save(Spend(amount = 99_00, category = SpendCategory.KIT, date = LocalDate(2026, 3, 1)))
        assertEquals(99_00, spends.observeAll().first().single().amount)

        assertEquals("INV-0001", ProfileRepository(db, io).getProfile().let { "${it.invoicePrefix}000${it.nextInvoiceNumber}" })

        val links = CalendarEventRepository(db, io)
        links.save(CalendarLink(jobId = 1, eventId = 77, signature = "a"))
        links.save(CalendarLink(jobId = 1, eventId = 78, signature = "b"))
        assertEquals(listOf(CalendarLink(1, 78, "b")), links.all())

        val recurring = RecurringSpendRepository(db, io)
        recurring.setRepeating(spends.observeAll().first().single().id, repeat = true, today = LocalDate(2026, 4, 1))
        assertEquals(2, spends.observeAll().first().size)
    }
}
