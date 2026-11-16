package com.abaan.wrap.ui.di

import com.abaan.wrap.ui.platform.AppInfo
import com.abaan.wrap.ui.platform.BackupFiles
import com.abaan.wrap.ui.platform.BudgetAlerts
import com.abaan.wrap.ui.platform.ExportFiles
import com.abaan.wrap.ui.platform.InvoiceWriter
import com.abaan.wrap.ui.platform.KeyValueFiles
import com.abaan.wrap.ui.platform.KeyValues
import com.abaan.wrap.ui.platform.PendingRestore
import com.abaan.wrap.ui.platform.PhoneCalendar
import com.abaan.wrap.ui.platform.PlatformFeatures
import com.abaan.wrap.ui.platform.ReceiptPhotos
import com.abaan.wrap.ui.platform.ScreenLock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.writeToFile
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Everything the screens need on iOS. Features iOS doesn't have yet are switched off in
 * [PlatformFeatures], so their stand-ins below are never reached.
 */
actual val uiPlatformModule: Module = module {
    single {
        PlatformFeatures(
            receipts = false,
            invoices = false,
            backups = false,
            calendarSync = false,
            appLock = false,
            notifications = false,
        )
    }
    single {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        AppInfo(version.orEmpty())
    }
    single<KeyValueFiles> { KeyValueFiles { name -> UserDefaultsValues(name) } }
    single<ExportFiles> { ExportFiles { name, text -> writeTemporaryFile(name, text) } }

    single<ReceiptPhotos> { NotOnIos }
    single<InvoiceWriter> { InvoiceWriter { notOnIos() } }
    single<BackupFiles> { NotOnIos }
    single<PhoneCalendar> { NotOnIos }
    single<ScreenLock> { ScreenLock { false } }
    single<BudgetAlerts> { BudgetAlerts { _, _, _ -> } }
}

/** Keys are prefixed with the set's name, like Android's separate preference files. */
private class UserDefaultsValues(private val name: String) : KeyValues {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey("$name.$key") == null) default else defaults.boolForKey("$name.$key")

    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, forKey = "$name.$key")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun writeTemporaryFile(name: String, text: String): String {
    val path = NSTemporaryDirectory() + name
    val written = NSString.create(string = text).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    check(written) { "Couldn't write $path" }
    return path
}

private fun notOnIos(): Nothing = error("Not available on iOS yet")

private object NotOnIos : ReceiptPhotos, BackupFiles, PhoneCalendar {
    override fun newCameraTarget(): String = notOnIos()
    override suspend fun import(uri: String): String? = null
    override suspend fun load(name: String, maxSide: Int) = null
    override fun delete(name: String) = Unit

    override val mimeType: String = "application/zip"
    override val lastBackupAt: Long? = null
    override suspend fun write(uri: String) = notOnIos()
    override suspend fun read(uri: String): PendingRestore = notOnIos()
    override suspend fun restore(pending: PendingRestore) = notOnIos()

    override val permissions: List<String> = emptyList()
    override fun hasPermission(): Boolean = false
    override fun onPermissionChanged() = Unit
}
