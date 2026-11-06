package com.abaan.wrap.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.abaan.wrap.shared.db.WrapDatabase
import com.abaan.wrap.shared.model.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
) {
    fun observeSettings(): Flow<AppSettings> =
        db.settingsEntityQueries.select().asFlow().mapToOne(io).map { it.toModel() }

    suspend fun getSettings(): AppSettings = withContext(io) {
        db.settingsEntityQueries.select().executeAsOne().toModel()
    }

    suspend fun save(settings: AppSettings) = withContext(io) {
        db.settingsEntityQueries.update(
            defaultRate = settings.defaultRate,
            dayLengthHours = settings.dayLengthHours,
            otMultiplier = settings.otMultiplier,
            paymentTermsDays = settings.paymentTermsDays.toLong(),
            currency = settings.currency,
            remindersEnabled = if (settings.remindersEnabled) 1L else 0L,
            monthlyBudget = settings.monthlyBudget.coerceAtLeast(0),
            budgetAlerts = if (settings.budgetAlertsEnabled) 1L else 0L,
            callReminderMinutes = settings.callReminderMinutes.coerceAtLeast(0).toLong(),
        )
    }
}
