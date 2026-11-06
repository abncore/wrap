package com.abaan.wrap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.TimeLogRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.Dashboard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

data class HomeUiState(
    val loading: Boolean = true,
    val dashboard: Dashboard? = null,
    val today: LocalDate? = null,
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
)

class HomeViewModel(
    ledger: LedgerRepository,
    private val timeLogs: TimeLogRepository,
    private val today: Today,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = ledger.observeDashboard()
        .map { HomeUiState(loading = false, dashboard = it, today = today.date(), timeZone = today.timeZone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun clockIn(jobId: Long) {
        viewModelScope.launch { timeLogs.clockIn(jobId) }
    }

    fun clockOut() {
        viewModelScope.launch { timeLogs.clockOut() }
    }
}
