package com.abaan.wrap.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.ui.platform.BudgetAlerts
import com.abaan.wrap.ui.platform.ReceiptPhotos
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.RecurringSpendRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.SpendRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.JobSummary
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

enum class SpendFormError { AMOUNT_INVALID, RECEIPT_UNREADABLE }

data class SpendEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val amount: String = "",
    val category: SpendCategory = SpendCategory.OTHER,
    val date: LocalDate,
    val note: String = "",
    /** Whether the spend repeats every month; [wasRepeating] is how it was when the form opened. */
    val repeatMonthly: Boolean = false,
    val wasRepeating: Boolean = false,
    /** The job this is billed to, if any; [jobs] are the ones to pick from, newest first. */
    val jobId: Long? = null,
    val jobs: List<JobSummary> = emptyList(),
    /** Receipt photo file name, empty when there is none. */
    val receipt: String = "",
    val importingReceipt: Boolean = false,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val errors: Set<SpendFormError> = emptySet(),
    val done: Boolean = false,
)

class SpendEditViewModel(
    private val spendId: Long,
    private val spends: SpendRepository,
    private val recurringSpends: RecurringSpendRepository,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val today: Today,
    private val budgetNotifications: BudgetAlerts,
    private val receipts: ReceiptPhotos,
) : ViewModel() {
    private var editing: Spend? = null
    private var saved = false

    private val _state = MutableStateFlow(SpendEditUiState(date = today.date(), isNew = spendId == NEW_ID))
    val state: StateFlow<SpendEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val spend = if (spendId != NEW_ID) spends.getSpend(spendId) else null
            editing = spend
            // New entries start on the category used last, which is usually the one needed again.
            val lastCategory = spends.observeAll().first().firstOrNull()?.category
            val repeating = spend?.recurringId != null &&
                recurringSpends.observeAll().first().any { it.id == spend.recurringId }
            val jobs = ledger.observeJobSummaries().first().sortedByDescending { it.job.startDate }
            _state.update { state ->
                val base = state.copy(loading = false, currency = appSettings.currency, jobs = jobs)
                if (spend != null) {
                    base.copy(
                        amount = MoneyFormat.toInput(spend.amount),
                        category = spend.category,
                        date = spend.date,
                        note = spend.note,
                        repeatMonthly = repeating,
                        wasRepeating = repeating,
                        jobId = spend.jobId,
                        receipt = spend.receipt,
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

    fun setRepeatMonthly(value: Boolean) = _state.update { it.copy(repeatMonthly = value) }

    fun setJob(value: Long?) = _state.update { it.copy(jobId = value) }

    /** Where the camera app saves a new receipt photo. */
    fun cameraTarget(): String = receipts.newCameraTarget()

    /** Takes the photo at [uri] (from the camera or the photo picker) as this spend's receipt. */
    fun attachReceipt(uri: String) {
        _state.update { it.copy(importingReceipt = true, errors = emptySet()) }
        viewModelScope.launch {
            val name = receipts.import(uri)
            _state.update { state ->
                if (name == null) {
                    state.copy(importingReceipt = false, errors = setOf(SpendFormError.RECEIPT_UNREADABLE))
                } else {
                    discardUnsavedReceipt(state.receipt)
                    state.copy(importingReceipt = false, receipt = name)
                }
            }
        }
    }

    fun removeReceipt() = _state.update { state ->
        discardUnsavedReceipt(state.receipt)
        state.copy(receipt = "")
    }

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
                    recurringId = editing?.recurringId,
                    jobId = state.jobId,
                    receipt = state.receipt,
                ),
                monthlyBudget = appSettings.monthlyBudget,
                today = today.date(),
            )
            saved = true
            editing?.receipt?.takeIf { it != state.receipt }?.let(receipts::delete)
            val level = result.level
            val status = result.status
            if (appSettings.budgetAlertsEnabled && level != null && status != null) {
                budgetNotifications.show(level, status, appSettings.currency)
            }
            if (state.repeatMonthly || state.wasRepeating) {
                recurringSpends.setRepeating(result.spendId, state.repeatMonthly, today.date())
            }
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val spend = editing ?: return
        viewModelScope.launch {
            spends.delete(spend.id)
            saved = true
            receipts.delete(spend.receipt)
            discardUnsavedReceipt(_state.value.receipt)
            _state.update { it.copy(done = true) }
        }
    }

    override fun onCleared() {
        // A photo attached but never saved would otherwise be left behind.
        if (!saved) discardUnsavedReceipt(_state.value.receipt)
    }

    private fun discardUnsavedReceipt(name: String) {
        if (name.isNotBlank() && name != editing?.receipt) receipts.delete(name)
    }
}
