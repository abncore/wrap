package com.abaan.wrap.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationTest {
    @Test
    fun upgradingFromVersionOneKeepsDataAndAddsSpending() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Build the current schema, then remove what version 2 added to get a version 1 database.
        WrapDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE SpendEntity", 0)
        val v1 = WrapDatabase(driver)
        val clients = ClientRepository(v1, Dispatchers.Unconfined)
        clients.save(Client(name = "Sandstorm Films"))

        WrapDatabase.Schema.migrate(driver, 1, WrapDatabase.Schema.version)

        val db = WrapDatabase(driver)
        assertEquals(2L, WrapDatabase.Schema.version)
        assertEquals("Sandstorm Films", ClientRepository(db, Dispatchers.Unconfined).observeClients().first().single().name)
        val spends = SpendRepository(db, Dispatchers.Unconfined)
        spends.save(Spend(amount = 99_00, category = SpendCategory.KIT, date = LocalDate(2026, 3, 1)))
        assertEquals(99_00, spends.observeAll().first().single().amount)
    }
}
