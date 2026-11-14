package com.abaan.wrap.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.money.AgingReport
import com.abaan.wrap.shared.money.ClientPerformance
import com.abaan.wrap.shared.money.WorkReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A report plus the currency to show it in; [data] is null while loading. */
data class ReportState<T>(
    val data: T? = null,
    val period: InsightPeriod = InsightPeriod.THIS_YEAR,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
)

/** Shared plumbing: a selectable period and the report built for it. */
abstract class PeriodReportViewModel<T>(
    settings: SettingsRepository,
    initialPeriod: InsightPeriod,
    report: (Flow<InsightPeriod>) -> Flow<T>,
) : ViewModel() {
    private val period = MutableStateFlow(initialPeriod)

    val state: StateFlow<ReportState<T>> = combine(report(period), period, settings.observeSettings()) { data, selected, appSettings ->
        ReportState(data, selected, appSettings.currency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState(period = initialPeriod))

    fun setPeriod(value: InsightPeriod) {
        period.value = value
    }
}

class ClientPerformanceViewModel(ledger: LedgerRepository, settings: SettingsRepository) :
    PeriodReportViewModel<List<ClientPerformance>>(settings, InsightPeriod.THIS_YEAR, ledger::observeClientPerformance)

class ClientReportViewModel(clientId: Long, ledger: LedgerRepository, settings: SettingsRepository) :
    PeriodReportViewModel<ClientPerformance?>(settings, InsightPeriod.ALL_TIME, { ledger.observeClientReport(clientId, it) })

class WorkReportViewModel(ledger: LedgerRepository, settings: SettingsRepository) :
    PeriodReportViewModel<WorkReport>(settings, InsightPeriod.THIS_YEAR, ledger::observeWorkReport)

class AgingViewModel(ledger: LedgerRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<ReportState<AgingReport>> = combine(ledger.observeAging(), settings.observeSettings()) { data, appSettings ->
        ReportState(data, currency = appSettings.currency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())
}
