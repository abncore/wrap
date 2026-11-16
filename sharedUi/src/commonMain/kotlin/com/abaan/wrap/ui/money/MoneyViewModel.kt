package com.abaan.wrap.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.RecurringSpendRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import com.abaan.wrap.shared.model.RecurringSpend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoneyUiState(
    val loading: Boolean = true,
    val overview: MoneyOverview? = null,
    val recurring: List<RecurringSpend> = emptyList(),
)

class MoneyViewModel(
    ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val recurringSpends: RecurringSpendRepository,
) : ViewModel() {
    private val period = MutableStateFlow(InsightPeriod.THIS_MONTH)

    val state: StateFlow<MoneyUiState> = combine(
        ledger.observeMoneyOverview(period),
        recurringSpends.observeAll(),
    ) { overview, recurring -> MoneyUiState(loading = false, overview = overview, recurring = recurring) }
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

    fun stopRecurring(id: Long) {
        viewModelScope.launch { recurringSpends.stop(id) }
    }
}
