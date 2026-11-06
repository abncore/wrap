package com.abaan.wrap.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.JobRepository
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

data class JobDetailUiState(
    val loading: Boolean = true,
    val summary: JobSummary? = null,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    val deleted: Boolean = false,
)

class JobDetailViewModel(
    private val jobId: Long,
    ledger: LedgerRepository,
    private val jobs: JobRepository,
    settings: SettingsRepository,
    private val today: Today,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<JobDetailUiState> = combine(
        ledger.observeJobSummary(jobId),
        settings.observeSettings(),
        deleted,
    ) { summary, appSettings, isDeleted ->
        JobDetailUiState(
            loading = false,
            summary = summary,
            currency = appSettings.currency,
            timeZone = today.timeZone,
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobDetailUiState())

    fun setWorkDone(done: Boolean) {
        viewModelScope.launch { jobs.setWorkDone(jobId, done) }
    }

    fun delete() {
        viewModelScope.launch {
            jobs.delete(jobId)
            deleted.value = true
        }
    }
}
