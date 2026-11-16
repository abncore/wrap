package com.abaan.wrap.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.abaan.wrap.shared.backup.BackupSummary
import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.BudgetStatus
import com.abaan.wrap.shared.money.Invoice

/**
 * Which phone features this platform has so far. Screens hide what isn't there yet; the iOS
 * app gains these one at a time.
 */
data class PlatformFeatures(
    val receipts: Boolean,
    val invoices: Boolean,
    val backups: Boolean,
    val calendarSync: Boolean,
    val appLock: Boolean,
    val notifications: Boolean,
)

/** The app's version, shown in Settings. */
data class AppInfo(val versionName: String)

/** Tells the user when spending nears or passes the monthly budget. */
fun interface BudgetAlerts {
    fun show(level: BudgetLevel, status: BudgetStatus, currency: String)
}

/** Receipt photos, stored privately by file name. */
interface ReceiptPhotos {
    /** A place for the camera to save a new photo; pass it to [rememberTakePhoto]. */
    fun newCameraTarget(): String

    /** Copies the photo at [uri] in as a receipt and returns its name, or null when it can't be read. */
    suspend fun import(uri: String): String?

    /** The receipt scaled to at most [maxSide] pixels on its longest side. */
    suspend fun load(name: String, maxSide: Int): ImageBitmap?

    fun delete(name: String)
}

/** Writes an invoice as a PDF ready to share and returns its path. */
fun interface InvoiceWriter {
    suspend fun write(invoice: Invoice): String
}

/** A backup that was read and is waiting for the user to confirm replacing everything. */
interface PendingRestore {
    val summary: BackupSummary
}

/** Backups of all data to a file the user picks. */
interface BackupFiles {
    val mimeType: String

    /** When the last backup was saved, in epoch milliseconds. */
    val lastBackupAt: Long?

    suspend fun write(uri: String)

    suspend fun read(uri: String): PendingRestore

    suspend fun restore(pending: PendingRestore)
}

/** Copying jobs into the phone's calendar. */
interface PhoneCalendar {
    val permissions: List<String>

    fun hasPermission(): Boolean

    fun onPermissionChanged()
}

/** The phone's fingerprint, face or screen lock. */
fun interface ScreenLock {
    fun isAvailable(): Boolean
}

/** Files made to hand to other apps, such as report exports. */
fun interface ExportFiles {
    /** Writes [text] to a file called [name] and returns its path. */
    suspend fun writeText(name: String, text: String): String
}

/** Small values remembered on this phone, such as whether the tutorial was seen. */
interface KeyValues {
    fun getBoolean(key: String, default: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

/** Opens a named set of [KeyValues]. */
fun interface KeyValueFiles {
    fun open(name: String): KeyValues
}
