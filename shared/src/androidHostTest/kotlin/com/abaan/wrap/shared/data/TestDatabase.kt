package com.abaan.wrap.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.abaan.wrap.shared.db.WrapDatabase

/** A fresh in-memory database with the schema created. */
fun inMemoryDatabase(): WrapDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    WrapDatabase.Schema.create(driver)
    return WrapDatabase(driver)
}
