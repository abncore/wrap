package com.abaan.wrap.di

import com.abaan.wrap.AppLock
import com.abaan.wrap.BuildConfig
import com.abaan.wrap.files.BackupManager
import com.abaan.wrap.files.InvoicePdf
import com.abaan.wrap.files.ReceiptStore
import com.abaan.wrap.reminders.BudgetNotifications
import com.abaan.wrap.reminders.CalendarSync
import com.abaan.wrap.reminders.ReminderScheduler
import com.abaan.wrap.ui.platform.AppInfo
import com.abaan.wrap.ui.platform.BackupFiles
import com.abaan.wrap.ui.platform.BudgetAlerts
import com.abaan.wrap.ui.platform.InvoiceWriter
import com.abaan.wrap.ui.platform.PhoneCalendar
import com.abaan.wrap.ui.platform.PlatformFeatures
import com.abaan.wrap.ui.platform.ReceiptPhotos
import com.abaan.wrap.ui.platform.ScreenLock
import com.abaan.wrap.widget.WidgetUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Android-only services, also handed to the shared screens through their platform interfaces. */
val appModule = module {
    single { ReminderScheduler(androidContext(), get(), get(), get(), get()) }
    single { BudgetNotifications(androidContext()) } bind BudgetAlerts::class
    single { ReceiptStore(androidContext()) } bind ReceiptPhotos::class
    single { InvoicePdf(androidContext()) } bind InvoiceWriter::class
    single { CalendarSync(androidContext(), get(), get(), get(), get()) } bind PhoneCalendar::class
    single { WidgetUpdater(androidContext(), get(), get()) }
    single { BackupManager(androidContext(), get(), get(), get()) } bind BackupFiles::class
    single<ScreenLock> {
        val context = androidContext()
        ScreenLock { AppLock.isAvailable(context) }
    }
    single { AppInfo(BuildConfig.VERSION_NAME) }
    single {
        PlatformFeatures(
            receipts = true,
            invoices = true,
            backups = true,
            calendarSync = true,
            appLock = true,
            notifications = true,
        )
    }
}
