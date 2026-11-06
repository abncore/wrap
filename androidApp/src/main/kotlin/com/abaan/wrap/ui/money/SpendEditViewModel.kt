package com.abaan.wrap.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.reminders.BudgetNotifications
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.SpendRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.shared.money.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class SpendFormError { AMOUNT_INVALID }

data class SpendEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val amount: String = "",
    val category: SpendCategory = SpendCategory.OTHER,
    val date: LocalDate,
    val note: String = "",
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val errors: Set<SpendFormError> = emptySet(),
    val done: Boolean = false,
)

class SpendEditViewModel(
    private val spendId: Long,
    private val spends: SpendRepository,
    private val settings: SettingsRepository,
    private val today: Today,
    private val budgetNotifications: BudgetNotifications,
) : ViewModel() {
    private var editing: Spend? = null

    private val _state = MutableStateFlow(SpendEditUiState(date = today.date(), isNew = spendId == NEW_ID))
    val state: StateFlow<SpendEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val spend = if (spendId != NEW_ID) spends.getSpend(spendId) else null
            editing = spend
            // New entries start on the category used last, which is usually the one needed again.
            val lastCategory = spends.observeAll().first().firstOrNull()?.category
            _state.update { state ->
                val base = state.copy(loading = false, currency = appSettings.currency)
                if (spend != null) {
                    base.copy(
                        amount = MoneyFormat.toInput(spend.amount),
                        category = spend.category,
                        date = spend.date,
                        note = spend.note,
                    )
                } else {
                    base.copy(category = lastCategory ?: base.category)
                }
            }
        }
    }

    fun setAmount(value: String) = _state.update { it.copy(amount = value, errors = emptySet()) }

    fun setCategory(value: SpendCategory) = _state.update { it.copy(category = value) }

    fun setDate(value: LocalDate) = _state.update { it.copy(date = value) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun save() {
        val state = _state.value
        val amount = MoneyFormat.parse(state.amount)
        if (amount == null || amount <= 0) {
            _state.update { it.copy(errors = setOf(SpendFormError.AMOUNT_INVALID)) }
            return
        }
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val result = spends.saveAndCheckBudget(
                spend = Spend(
                    id = editing?.id ?: NEW_ID,
                    amount = amount,
                    category = state.category,
                    date = state.date,
                    note = state.note,
                ),
                monthlyBudget = appSettings.monthlyBudget,
                today = today.date(),
            )
            val level = result.level
            val status = result.status
            if (appSettings.budgetAlertsEnabled && level != null && status != null) {
                budgetNotifications.show(level, status, appSettings.currency)
            }
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val spend = editing ?: return
        viewModelScope.launch {
            spends.delete(spend.id)
            _state.update { it.copy(done = true) }
        }
    }
}
