package com.abaan.wrap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.ProfileRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val profile: Profile = Profile(),
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    profiles: ProfileRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(repository.observeSettings(), profiles.observeProfile()) { settings, profile ->
        SettingsUiState(loading = false, settings = settings, profile = profile)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.save(transform(repository.getSettings()))
        }
    }
}
