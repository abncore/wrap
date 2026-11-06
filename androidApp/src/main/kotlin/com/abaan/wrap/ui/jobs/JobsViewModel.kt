package com.abaan.wrap.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

enum class JobsTab { UPCOMING, UNPAID, PAID }

data class JobsFilter(
    val tab: JobsTab = JobsTab.UPCOMING,
    val clientId: Long? = null,
    /** First day of the selected month, or null for all months. */
    val month: LocalDate? = null,
)

data class JobsUiState(
    val loading: Boolean = true,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val filter: JobsFilter = JobsFilter(),
    val jobs: List<JobSummary> = emptyList(),
    val counts: Map<JobsTab, Int> = emptyMap(),
    val clients: List<Client> = emptyList(),
    val months: List<LocalDate> = emptyList(),
    val hasAnyJobs: Boolean = false,
)

class JobsViewModel(
    ledger: LedgerRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(JobsFilter())

    val state: StateFlow<JobsUiState> = combine(
        ledger.observeJobSummaries(),
        settings.observeSettings(),
        filter,
    ) { summaries, appSettings, currentFilter ->
        val filtered = summaries.filter { summary ->
            (currentFilter.clientId == null || summary.job.clientId == currentFilter.clientId) &&
                (currentFilter.month == null || summary.job.startDate.monthStart() == currentFilter.month)
        }
        val byTab = filtered.groupBy(::tabOf)
        JobsUiState(
            loading = false,
            currency = appSettings.currency,
            filter = currentFilter,
            jobs = sortForTab(currentFilter.tab, byTab[currentFilter.tab].orEmpty()),
            counts = JobsTab.entries.associateWith { byTab[it]?.size ?: 0 },
            clients = summaries.mapNotNull { it.client }.distinctBy { it.id }.sortedBy { it.name.lowercase() },
            months = summaries.map { it.job.startDate.monthStart() }.distinct().sortedDescending(),
            hasAnyJobs = summaries.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobsUiState())

    fun selectTab(tab: JobsTab) = filter.update { it.copy(tab = tab) }

    fun filterClient(clientId: Long?) = filter.update { it.copy(clientId = clientId) }

    fun filterMonth(month: LocalDate?) = filter.update { it.copy(month = month) }

    private fun tabOf(summary: JobSummary): JobsTab = when {
        summary.isPaid -> JobsTab.PAID
        summary.isUnpaid -> JobsTab.UNPAID
        else -> JobsTab.UPCOMING
    }

    private fun sortForTab(tab: JobsTab, jobs: List<JobSummary>): List<JobSummary> = when (tab) {
        JobsTab.UPCOMING -> jobs.sortedWith(compareBy<JobSummary> { it.job.startDate }.thenBy { it.job.callTime })
        JobsTab.UNPAID -> jobs.sortedWith(
            compareByDescending<JobSummary> { it.isOverdue }
                .thenBy { it.job.dueDate ?: it.job.endDate }
                .thenBy { it.job.startDate },
        )
        JobsTab.PAID -> jobs.sortedByDescending { it.job.endDate }
    }

    private fun LocalDate.monthStart(): LocalDate = LocalDate(year, month, 1)
}
