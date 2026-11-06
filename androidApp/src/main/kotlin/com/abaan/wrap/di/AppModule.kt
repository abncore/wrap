package com.abaan.wrap.di

import com.abaan.wrap.ui.clients.ClientEditViewModel
import com.abaan.wrap.ui.clients.ClientsViewModel
import com.abaan.wrap.ui.jobs.JobDetailViewModel
import com.abaan.wrap.ui.jobs.JobEditViewModel
import com.abaan.wrap.ui.jobs.JobsViewModel
import com.abaan.wrap.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Android-only dependencies: ViewModels and platform services. */
val appModule = module {
    viewModel { ClientsViewModel(get(), get()) }
    viewModel { (clientId: Long) -> ClientEditViewModel(clientId, get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { JobsViewModel(get(), get()) }
    viewModel { (jobId: Long, finished: Boolean) ->
        JobEditViewModel(jobId, finished, get(), get(), get(), get(), get())
    }
    viewModel { (jobId: Long) -> JobDetailViewModel(jobId, get(), get(), get(), get(), get()) }
}
