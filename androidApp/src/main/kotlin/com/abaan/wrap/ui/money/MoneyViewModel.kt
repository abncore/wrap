package com.abaan.wrap.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoneyUiState(
    val loading: Boolean = true,
    val overview: MoneyOverview? = null,
)

class MoneyViewModel(
    ledger: LedgerRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val period = MutableStateFlow(InsightPeriod.THIS_MONTH)

    val state: StateFlow<MoneyUiState> = ledger.observeMoneyOverview(period)
        .map { MoneyUiState(loading = false, overview = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyUiState())

    fun setPeriod(value: InsightPeriod) {
        period.value = value
    }

    /** [amount] in minor units; 0 removes the budget. */
    fun setBudget(amount: Long) {
        viewModelScope.launch {
            settings.save(settings.getSettings().copy(monthlyBudget = amount))
        }
    }
}
