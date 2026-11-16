package com.abaan.wrap.ui.di

import com.abaan.wrap.ui.clients.ClientEditViewModel
import com.abaan.wrap.ui.clients.ClientsViewModel
import com.abaan.wrap.ui.home.HomeViewModel
import com.abaan.wrap.ui.jobs.CalendarViewModel
import com.abaan.wrap.ui.jobs.JobDetailViewModel
import com.abaan.wrap.ui.jobs.JobEditViewModel
import com.abaan.wrap.ui.jobs.JobsViewModel
import com.abaan.wrap.ui.money.MoneyViewModel
import com.abaan.wrap.ui.money.ReportViewModel
import com.abaan.wrap.ui.money.SpendEditViewModel
import com.abaan.wrap.ui.payments.PaymentEditViewModel
import com.abaan.wrap.ui.platform.KeyValueFiles
import com.abaan.wrap.ui.reports.AgingViewModel
import com.abaan.wrap.ui.reports.ClientPerformanceViewModel
import com.abaan.wrap.ui.reports.ClientReportViewModel
import com.abaan.wrap.ui.reports.WorkReportViewModel
import com.abaan.wrap.ui.settings.BackupViewModel
import com.abaan.wrap.ui.settings.CreditsViewModel
import com.abaan.wrap.ui.settings.NightShoot
import com.abaan.wrap.ui.settings.ProfileViewModel
import com.abaan.wrap.ui.settings.SettingsViewModel
import com.abaan.wrap.ui.tutorial.Onboarding
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Platform services for the shared screens: see `com.abaan.wrap.ui.platform`. */
expect val uiPlatformModule: Module

/** The shared screens' ViewModels and state. Needs [uiPlatformModule] and the app's own services. */
val uiModule = module {
    includes(uiPlatformModule)

    single { Onboarding(get<KeyValueFiles>().open("onboarding")) }
    single { NightShoot(get<KeyValueFiles>().open("easter_eggs")) }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ClientsViewModel(get(), get()) }
    viewModel { (clientId: Long) -> ClientEditViewModel(clientId, get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { BackupViewModel(get()) }
    viewModel { CreditsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { JobsViewModel(get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get()) }
    viewModel { (jobId: Long, finished: Boolean) ->
        JobEditViewModel(jobId, finished, get(), get(), get(), get(), get(), get())
    }
    viewModel { (jobId: Long) -> JobDetailViewModel(jobId, get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (jobId: Long, paymentId: Long) ->
        PaymentEditViewModel(jobId, paymentId, get(), get(), get(), get())
    }
    viewModel { MoneyViewModel(get(), get(), get()) }
    viewModel { ReportViewModel(get(), get()) }
    viewModel { ClientPerformanceViewModel(get(), get()) }
    viewModel { (clientId: Long) -> ClientReportViewModel(clientId, get(), get()) }
    viewModel { AgingViewModel(get(), get()) }
    viewModel { WorkReportViewModel(get(), get()) }
    viewModel { (spendId: Long) -> SpendEditViewModel(spendId, get(), get(), get(), get(), get(), get(), get()) }
}
