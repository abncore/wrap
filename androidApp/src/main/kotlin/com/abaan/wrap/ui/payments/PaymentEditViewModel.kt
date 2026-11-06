package com.abaan.wrap.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.PaymentRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.money.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class PaymentFormError { JOB_REQUIRED, AMOUNT_INVALID }

data class PaymentEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    /** True when the job is fixed (opened from a job, or editing an existing payment). */
    val jobLocked: Boolean = false,
    val jobs: List<JobSummary> = emptyList(),
    val jobId: Long? = null,
    val amount: String = "",
    val date: LocalDate,
    val method: PaymentMethod = PaymentMethod.BANK,
    val note: String = "",
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    /** What is still owed on the selected job, not counting the payment being edited. */
    val owed: Long? = null,
    val errors: Set<PaymentFormError> = emptySet(),
    val done: Boolean = false,
)

class PaymentEditViewModel(
    private val jobId: Long,
    private val paymentId: Long,
    private val ledger: LedgerRepository,
    private val payments: PaymentRepository,
    private val settings: SettingsRepository,
    today: Today,
) : ViewModel() {
    private var editing: Payment? = null

    private val _state = MutableStateFlow(PaymentEditUiState(date = today.date(), isNew = paymentId == NEW_ID))
    val state: StateFlow<PaymentEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val payment = if (paymentId != NEW_ID) payments.getPayment(paymentId) else null
            editing = payment
            val summaries = ledger.observeJobSummaries().first()
            val fixedJobId = payment?.jobId ?: jobId.takeIf { it != NEW_ID }
            // Jobs that can still take money, most urgent first; the fixed job is always included.
            val options = summaries
                .filter { it.money.owed > 0 || it.job.id == fixedJobId }
                .sortedWith(
                    compareByDescending<JobSummary> { it.isOverdue }
                        .thenByDescending { it.workDone }
                        .thenBy { it.job.dueDate ?: it.job.endDate },
                )
            val selected = fixedJobId ?: options.singleOrNull()?.job?.id
            _state.update { state ->
                val base = state.copy(
                    loading = false,
                    jobLocked = fixedJobId != null,
                    jobs = options,
                    currency = appSettings.currency,
                )
                if (payment != null) {
                    base.copy(
                        jobId = payment.jobId,
                        amount = MoneyFormat.toInput(payment.amount),
                        date = payment.dateReceived,
                        method = payment.method,
                        note = payment.note,
                    ).withOwed()
                } else {
                    base.copy(jobId = selected).withOwed().prefillAmount()
                }
            }
        }
    }

    fun selectJob(id: Long) = _state.update { it.copy(jobId = id, errors = emptySet()).withOwed().prefillAmount(force = true) }

    fun setAmount(value: String) = _state.update { it.copy(amount = value, errors = emptySet()) }

    fun setDate(value: LocalDate) = _state.update { it.copy(date = value) }

    fun setMethod(value: PaymentMethod) = _state.update { it.copy(method = value) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun fillOwed() = _state.update { state -> state.owed?.let { state.copy(amount = MoneyFormat.toInput(it)) } ?: state }

    fun save() {
        val state = _state.value
        val amount = MoneyFormat.parse(state.amount)
        val errors = buildSet {
            if (state.jobId == null) add(PaymentFormError.JOB_REQUIRED)
            if (amount == null || amount <= 0) add(PaymentFormError.AMOUNT_INVALID)
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            payments.save(
                Payment(
                    id = editing?.id ?: NEW_ID,
                    jobId = state.jobId!!,
                    amount = amount!!,
                    dateReceived = state.date,
                    method = state.method,
                    note = state.note,
                ),
            )
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val payment = editing ?: return
        viewModelScope.launch {
            payments.delete(payment.id)
            _state.update { it.copy(done = true) }
        }
    }

    private fun PaymentEditUiState.withOwed(): PaymentEditUiState {
        val summary = jobs.firstOrNull { it.job.id == jobId } ?: return copy(owed = null)
        val editingAmount = editing?.takeIf { it.jobId == summary.job.id }?.amount ?: 0
        val owed = (summary.money.total - summary.money.paid + editingAmount).coerceAtLeast(0)
        return copy(owed = owed)
    }

    private fun PaymentEditUiState.prefillAmount(force: Boolean = false): PaymentEditUiState =
        if ((amount.isBlank() || force) && owed != null && owed > 0) copy(amount = MoneyFormat.toInput(owed)) else this
}
