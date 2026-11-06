package com.abaan.wrap.di

import com.abaan.wrap.ui.clients.ClientEditViewModel
import com.abaan.wrap.ui.clients.ClientsViewModel
import com.abaan.wrap.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Android-only dependencies: ViewModels and platform services. */
val appModule = module {
    viewModel { ClientsViewModel(get(), get()) }
    viewModel { params -> ClientEditViewModel(params.get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
