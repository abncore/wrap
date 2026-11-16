package com.abaan.wrap.files

import com.abaan.wrap.ui.platform.PendingRestore
import com.abaan.wrap.ui.platform.BackupFiles
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.abaan.wrap.reminders.CalendarSync
import com.abaan.wrap.shared.backup.BackupRepository
import com.abaan.wrap.shared.backup.BackupSummary
import com.abaan.wrap.shared.backup.InvalidBackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A backup that has been read and checked, waiting for the user to confirm the restore. */
private class ZipRestore(override val summary: BackupSummary, val json: String, val receiptsDir: File) : PendingRestore

/**
 * Backups are a .zip holding wrap.json (all the data) and the receipt photos, saved wherever the user
 * picks: Google Drive, the phone's Downloads, or sent to themselves on WhatsApp.
 */
class BackupManager(
    private val context: Context,
    private val backups: BackupRepository,
    private val receipts: ReceiptStore,
    private val calendarSync: CalendarSync,
) : BackupFiles {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val mimeType: String = MIME_TYPE

    /** When the last backup was made on this phone (epoch milliseconds), or null. */
    override val lastBackupAt: Long?
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 }

    /** Writes a backup to [uri] (from the system "save file" picker). */
    override suspend fun write(uri: String) = write(Uri.parse(uri))

    private suspend fun write(uri: Uri) = withContext(Dispatchers.IO) {
        val json = backups.export()
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Can't write to $uri")
        ZipOutputStream(stream.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.toByteArray())
            zip.closeEntry()
            receipts.directory.listFiles()?.filter { it.isFile }?.forEach { file ->
                zip.putNextEntry(ZipEntry(RECEIPTS_PREFIX + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        prefs.edit { putLong(KEY_LAST_BACKUP, System.currentTimeMillis()) }
    }

    /** Reads and checks the backup at [uri] without changing anything. */
    override suspend fun read(uri: String): PendingRestore = read(Uri.parse(uri))

    private suspend fun read(uri: Uri): PendingRestore = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, STAGING).apply {
            deleteRecursively()
            mkdirs()
        }
        var json: String? = null
        val stream = context.contentResolver.openInputStream(uri) ?: throw InvalidBackupException("Can't open $uri")
        ZipInputStream(stream.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.name == DATA_ENTRY -> json = zip.readBytes().decodeToString()
                    entry.name.startsWith(RECEIPTS_PREFIX) && !entry.isDirectory -> {
                        // Only plain file names, so a crafted zip can't write outside the folder.
                        val name = File(entry.name).name
                        if (name.isNotBlank() && !name.startsWith(".")) {
                            File(staging, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        }
        val text = json ?: throw InvalidBackupException("No $DATA_ENTRY in the backup")
        ZipRestore(backups.summarize(text), text, staging)
    }

    /** Replaces everything on this phone with [pending]. */
    override suspend fun restore(pending: PendingRestore): Unit = withContext(Dispatchers.IO) {
        pending as ZipRestore
        // Events this phone added belong to the data being replaced.
        calendarSync.removeAllEvents()
        backups.restore(pending.json)
        receipts.directory.listFiles()?.forEach { it.delete() }
        pending.receiptsDir.listFiles()?.forEach { it.copyTo(File(receipts.directory, it.name), overwrite = true) }
        pending.receiptsDir.deleteRecursively()
    }

    companion object {
        const val MIME_TYPE = "application/zip"
        private const val DATA_ENTRY = "wrap.json"
        private const val RECEIPTS_PREFIX = "receipts/"
        private const val STAGING = "restore"
        private const val PREFS = "backup"
        private const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
