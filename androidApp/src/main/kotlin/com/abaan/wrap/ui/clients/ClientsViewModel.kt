package com.abaan.wrap.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.ClientSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ClientsUiState(
    val loading: Boolean = true,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val clients: List<ClientSummary> = emptyList(),
)

class ClientsViewModel(
    ledger: LedgerRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ClientsUiState> = combine(
        ledger.observeClientSummaries(),
        settings.observeSettings(),
    ) { clients, appSettings ->
        ClientsUiState(loading = false, currency = appSettings.currency, clients = clients)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientsUiState())
}
