package com.abaan.wrap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
)

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = repository.observeSettings()
        .map { SettingsUiState(loading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.save(transform(repository.getSettings()))
        }
    }
}
