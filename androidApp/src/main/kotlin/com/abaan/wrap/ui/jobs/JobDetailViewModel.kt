package com.abaan.wrap.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.ChaseRepository
import com.abaan.wrap.shared.data.CrewRepository
import com.abaan.wrap.shared.data.JobRepository
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.TimeLogRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.ChaseLog
import com.abaan.wrap.shared.model.CrewContact
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.money.Shifts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

data class JobDetailUiState(
    val loading: Boolean = true,
    val summary: JobSummary? = null,
    val crew: List<CrewContact> = emptyList(),
    /** Times the client was chased, newest first. */
    val chases: List<ChaseLog> = emptyList(),
    val currency: String = AppSettings.DEFAULT_CURRENCY,
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    val today: LocalDate = LocalDate(1970, 1, 1),
    val deleted: Boolean = false,
)

class JobDetailViewModel(
    private val jobId: Long,
    ledger: LedgerRepository,
    private val jobs: JobRepository,
    private val timeLogs: TimeLogRepository,
    settings: SettingsRepository,
    private val today: Today,
    private val crewContacts: CrewRepository,
    private val chaseLog: ChaseRepository,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<JobDetailUiState> = combine(
        ledger.observeJobSummary(jobId),
        settings.observeSettings(),
        deleted,
        crewContacts.observeForJob(jobId),
        chaseLog.observeChases(jobId),
    ) { summary, appSettings, isDeleted, crew, chases ->
        JobDetailUiState(
            loading = false,
            summary = summary,
            crew = crew,
            chases = chases,
            currency = appSettings.currency,
            timeZone = today.timeZone,
            today = today.date(),
            deleted = isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobDetailUiState())

    fun setWorkDone(done: Boolean) {
        viewModelScope.launch { jobs.setWorkDone(jobId, done) }
    }

    fun clockIn() {
        viewModelScope.launch { timeLogs.clockIn(jobId) }
    }

    fun clockOut() {
        viewModelScope.launch { timeLogs.clockOut() }
    }

    /** Adds a shift on [date]; a [to] at or before [from] means it ran past midnight. */
    fun addTimeLog(date: LocalDate, from: LocalTime, to: LocalTime) {
        val (clockIn, clockOut) = Shifts.single(date, from, to, today.timeZone)
        viewModelScope.launch { timeLogs.addLog(jobId, clockIn, clockOut) }
    }

    fun deleteTimeLog(id: Long) {
        viewModelScope.launch { timeLogs.delete(id) }
    }

    fun recordChase() {
        viewModelScope.launch { chaseLog.recordChase(jobId) }
    }

    fun addCrew(name: String, role: String, phone: String) {
        viewModelScope.launch { crewContacts.add(CrewContact(jobId = jobId, name = name, role = role, phone = phone)) }
    }

    fun deleteCrew(id: Long) {
        viewModelScope.launch { crewContacts.delete(id) }
    }

    fun delete() {
        viewModelScope.launch {
            jobs.delete(jobId)
            deleted.value = true
        }
    }
}
