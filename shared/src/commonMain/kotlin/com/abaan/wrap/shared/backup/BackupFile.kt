package com.abaan.wrap.shared.backup

import kotlinx.serialization.Serializable

/**
 * Everything the app stores, as written to a backup. Rows keep their database ids so links between
 * them (job -> client, payment -> job...) survive a restore. Dates are epoch days, times epoch
 * milliseconds and money minor units, exactly as in the database. Fields added in later versions
 * need defaults so older backups still load.
 */
@Serializable
data class BackupFile(
    val format: Int = FORMAT,
    /** Database schema version of the app that wrote it. */
    val schemaVersion: Long,
    /** Epoch milliseconds. */
    val createdAt: Long,
    val settings: SettingsRow,
    val profile: ProfileRow = ProfileRow(),
    val clients: List<ClientRow> = emptyList(),
    val jobs: List<JobRow> = emptyList(),
    val timeLogs: List<TimeLogRow> = emptyList(),
    val payments: List<PaymentRow> = emptyList(),
    val spends: List<SpendRow> = emptyList(),
    val recurringSpends: List<RecurringSpendRow> = emptyList(),
    val crew: List<CrewRow> = emptyList(),
    val chaseLog: List<ChaseLogRow> = emptyList(),
) {
    companion object {
        const val FORMAT = 1
    }
}

@Serializable
data class SettingsRow(
    val defaultRate: Long,
    val dayLengthHours: Double,
    val otMultiplier: Double,
    val paymentTermsDays: Long,
    val currency: String,
    val remindersEnabled: Boolean,
    val monthlyBudget: Long = 0,
    val budgetAlerts: Boolean = true,
    val callReminderMinutes: Long = 60,
    val chaseReminders: Boolean = true,
    val calendarSync: Boolean = false,
    val appLock: Boolean = false,
)

@Serializable
data class ProfileRow(
    val name: String = "",
    val businessName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val trn: String = "",
    val bankDetails: String = "",
    val vatRegistered: Boolean = false,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
)

@Serializable
data class ClientRow(
    val id: Long,
    val name: String,
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val paymentTermsDays: Long? = null,
    val defaultRate: Long? = null,
    val notes: String = "",
)

@Serializable
data class JobRow(
    val id: Long,
    val clientId: Long,
    val title: String,
    val role: String = "",
    val startDate: Long,
    val endDate: Long,
    val callTime: Long? = null,
    val location: String = "",
    val rateType: String,
    val rate: Long,
    val dayLengthHours: Double,
    val otMultiplier: Double,
    val kitRental: Long = 0,
    val expenses: Long = 0,
    val dueDate: Long? = null,
    val status: String,
    val notes: String = "",
    val unitBase: String = "",
    val parking: String = "",
    val invoiceNumber: String? = null,
    val vatRate: Double = 0.0,
)

@Serializable
data class TimeLogRow(val id: Long, val jobId: Long, val clockIn: Long, val clockOut: Long? = null)

@Serializable
data class PaymentRow(
    val id: Long,
    val jobId: Long,
    val amount: Long,
    val dateReceived: Long,
    val method: String,
    val note: String = "",
)

@Serializable
data class SpendRow(
    val id: Long,
    val amount: Long,
    val category: String,
    val date: Long,
    val note: String = "",
    val recurringId: Long? = null,
    val jobId: Long? = null,
    /** File name of the receipt photo, stored next to the JSON in the backup. */
    val receipt: String = "",
)

@Serializable
data class RecurringSpendRow(
    val id: Long,
    val amount: Long,
    val category: String,
    val note: String = "",
    val dayOfMonth: Long,
    val nextDate: Long,
)

@Serializable
data class CrewRow(val id: Long, val jobId: Long, val name: String, val role: String = "", val phone: String = "")

@Serializable
data class ChaseLogRow(val id: Long, val jobId: Long, val at: Long, val kind: String)
