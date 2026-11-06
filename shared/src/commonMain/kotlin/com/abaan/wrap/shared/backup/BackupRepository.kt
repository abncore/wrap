package com.abaan.wrap.shared.backup

import com.abaan.wrap.shared.db.ChaseLogEntity
import com.abaan.wrap.shared.db.ClientEntity
import com.abaan.wrap.shared.db.CrewContactEntity
import com.abaan.wrap.shared.db.JobEntity
import com.abaan.wrap.shared.db.PaymentEntity
import com.abaan.wrap.shared.db.ProfileEntity
import com.abaan.wrap.shared.db.RecurringSpendEntity
import com.abaan.wrap.shared.db.SettingsEntity
import com.abaan.wrap.shared.db.SpendEntity
import com.abaan.wrap.shared.db.TimeLogEntity
import com.abaan.wrap.shared.db.WrapDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** What a backup holds, shown before restoring it. */
data class BackupSummary(
    val createdAt: Long,
    val clients: Int,
    val jobs: Int,
    val payments: Int,
    val spends: Int,
    /** Receipt photo file names the backup refers to. */
    val receipts: List<String>,
)

/** Thrown when a file isn't a Wrap backup, or comes from a newer version of the app. */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Turns the whole database into a JSON backup and back. Receipt photos are handled by the app. */
class BackupRepository(
    private val db: WrapDatabase,
    private val io: CoroutineDispatcher,
    private val clock: Clock,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        // Write every field, so a backup keeps its values even if a default changes later.
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun export(): String = withContext(io) {
        val file = db.transactionWithResult {
            BackupFile(
                schemaVersion = WrapDatabase.Schema.version,
                createdAt = clock.now().toEpochMilliseconds(),
                settings = db.settingsEntityQueries.select().executeAsOne().toRow(),
                profile = db.profileEntityQueries.select().executeAsOne().toRow(),
                clients = db.clientEntityQueries.selectAll().executeAsList().map { it.toRow() },
                jobs = db.jobEntityQueries.selectAll().executeAsList().map { it.toRow() },
                timeLogs = db.timeLogEntityQueries.selectAll().executeAsList().map { it.toRow() },
                payments = db.paymentEntityQueries.selectAll().executeAsList().map { it.toRow() },
                spends = db.spendEntityQueries.selectAll().executeAsList().map { it.toRow() },
                recurringSpends = db.recurringSpendEntityQueries.selectAll().executeAsList().map { it.toRow() },
                crew = db.crewContactEntityQueries.selectAll().executeAsList().map { it.toRow() },
                chaseLog = db.chaseLogEntityQueries.selectAll().executeAsList().map { it.toRow() },
            )
        }
        json.encodeToString(BackupFile.serializer(), file)
    }

    /** Reads [text] without changing anything. */
    fun summarize(text: String): BackupSummary {
        val file = parse(text)
        return BackupSummary(
            createdAt = file.createdAt,
            clients = file.clients.size,
            jobs = file.jobs.size,
            payments = file.payments.size,
            spends = file.spends.size,
            receipts = file.spends.map { it.receipt }.filter { it.isNotBlank() },
        )
    }

    /**
     * Replaces everything with the backup in [text], all at once: if anything fails nothing changes.
     * Links to phone calendar events are dropped, since they belong to the phone that made them.
     */
    suspend fun restore(text: String) = withContext(io) {
        val file = parse(text)
        db.transaction {
            db.chaseLogEntityQueries.deleteAll()
            db.crewContactEntityQueries.deleteAll()
            db.recurringSpendEntityQueries.deleteAll()
            db.spendEntityQueries.deleteAll()
            db.paymentEntityQueries.deleteAll()
            db.timeLogEntityQueries.deleteAll()
            db.jobEntityQueries.deleteAll()
            db.clientEntityQueries.deleteAll()
            db.calendarEventEntityQueries.deleteAll()

            db.settingsEntityQueries.restore(file.settings.toEntity())
            db.profileEntityQueries.restore(file.profile.toEntity())
            file.clients.forEach { db.clientEntityQueries.restore(it.toEntity()) }
            file.jobs.forEach { db.jobEntityQueries.restore(it.toEntity()) }
            file.timeLogs.forEach { db.timeLogEntityQueries.restore(it.toEntity()) }
            file.payments.forEach { db.paymentEntityQueries.restore(it.toEntity()) }
            file.spends.forEach { db.spendEntityQueries.restore(it.toEntity()) }
            file.recurringSpends.forEach { db.recurringSpendEntityQueries.restore(it.toEntity()) }
            file.crew.forEach { db.crewContactEntityQueries.restore(it.toEntity()) }
            file.chaseLog.forEach { db.chaseLogEntityQueries.restore(it.toEntity()) }
        }
    }

    private fun parse(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw InvalidBackupException("Not a Wrap backup", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException("Not a Wrap backup", e)
        }
        if (file.format > BackupFile.FORMAT || file.schemaVersion > WrapDatabase.Schema.version) {
            throw InvalidBackupException("Backup is from a newer version of Wrap")
        }
        return file
    }
}

private fun Boolean.toDb() = if (this) 1L else 0L

private fun SettingsEntity.toRow() = SettingsRow(
    defaultRate, dayLengthHours, otMultiplier, paymentTermsDays, currency, remindersEnabled != 0L,
    monthlyBudget, budgetAlerts != 0L, callReminderMinutes, chaseReminders != 0L, calendarSync != 0L, appLock != 0L,
)

private fun SettingsRow.toEntity() = SettingsEntity(
    1, defaultRate, dayLengthHours, otMultiplier, paymentTermsDays, currency, remindersEnabled.toDb(),
    monthlyBudget, budgetAlerts.toDb(), callReminderMinutes, chaseReminders.toDb(), calendarSync.toDb(), appLock.toDb(),
)

private fun ProfileEntity.toRow() = ProfileRow(
    name, businessName, address, phone, email, trn, bankDetails, vatRegistered != 0L, invoicePrefix, nextInvoiceNumber,
)

private fun ProfileRow.toEntity() = ProfileEntity(
    1, name, businessName, address, phone, email, trn, bankDetails, vatRegistered.toDb(), invoicePrefix, nextInvoiceNumber,
)

private fun ClientEntity.toRow() = ClientRow(id, name, company, phone, email, paymentTermsDays, defaultRate, notes)

private fun ClientRow.toEntity() = ClientEntity(id, name, company, phone, email, paymentTermsDays, defaultRate, notes)

private fun JobEntity.toRow() = JobRow(
    id, clientId, title, role, startDate, endDate, callTime, location, rateType, rate, dayLengthHours, otMultiplier,
    kitRental, expenses, dueDate, status, notes, unitBase, parking, invoiceNumber, vatRate,
)

private fun JobRow.toEntity() = JobEntity(
    id, clientId, title, role, startDate, endDate, callTime, location, rateType, rate, dayLengthHours, otMultiplier,
    kitRental, expenses, dueDate, status, notes, unitBase, parking, invoiceNumber, vatRate,
)

private fun TimeLogEntity.toRow() = TimeLogRow(id, jobId, clockIn, clockOut)

private fun TimeLogRow.toEntity() = TimeLogEntity(id, jobId, clockIn, clockOut)

private fun PaymentEntity.toRow() = PaymentRow(id, jobId, amount, dateReceived, method, note)

private fun PaymentRow.toEntity() = PaymentEntity(id, jobId, amount, dateReceived, method, note)

private fun SpendEntity.toRow() = SpendRow(id, amount, category, date, note, recurringId, jobId, receipt)

private fun SpendRow.toEntity() = SpendEntity(id, amount, category, date, note, recurringId, jobId, receipt)

private fun RecurringSpendEntity.toRow() = RecurringSpendRow(id, amount, category, note, dayOfMonth, nextDate)

private fun RecurringSpendRow.toEntity() = RecurringSpendEntity(id, amount, category, note, dayOfMonth, nextDate)

private fun CrewContactEntity.toRow() = CrewRow(id, jobId, name, role, phone)

private fun CrewRow.toEntity() = CrewContactEntity(id, jobId, name, role, phone)

private fun ChaseLogEntity.toRow() = ChaseLogRow(id, jobId, at, kind)

private fun ChaseLogRow.toEntity() = ChaseLogEntity(id, jobId, at, kind)
