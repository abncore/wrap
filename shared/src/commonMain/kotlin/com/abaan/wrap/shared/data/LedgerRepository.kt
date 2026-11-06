package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.model.ClientSummary
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.money.Insights
import com.abaan.wrap.shared.money.Ledger
import com.abaan.wrap.shared.money.ReportRow
import com.abaan.wrap.shared.money.Reports
import com.abaan.wrap.shared.money.YearReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A year's report plus the years there is anything to report on, newest first. */
data class YearReportView(val years: List<Int>, val report: YearReport)

/** Live, fully calculated views over all jobs, built from the raw repositories. */
class LedgerRepository(
    private val clients: ClientRepository,
    private val jobs: JobRepository,
    private val timeLogs: TimeLogRepository,
    private val payments: PaymentRepository,
    private val settings: SettingsRepository,
    private val spends: SpendRepository,
    private val today: Today,
) {
    fun observeJobSummaries(): Flow<List<JobSummary>> = combine(
        jobs.observeJobs(),
        clients.observeClients(),
        combine(timeLogs.observeAll(), payments.observeAll(), ::Pair),
        spends.observeAll(),
        today.observe(),
    ) { jobList, clientList, (logList, paymentList), spendList, date ->
        Ledger.summarizeAll(jobList, clientList, logList, paymentList, date, today.timeZone, spendList)
    }

    fun observeJobSummary(jobId: Long): Flow<JobSummary?> =
        observeJobSummaries().map { list -> list.firstOrNull { it.job.id == jobId } }.distinctUntilChanged()

    fun observeDashboard(): Flow<Dashboard> = combine(
        observeJobSummaries(),
        combine(payments.observeAll(), spends.observeAll(), ::Pair),
        timeLogs.observeOpenLog(),
        settings.observeSettings(),
        today.observe(),
    ) { summaries, (paymentList, spendList), openLog, appSettings, date ->
        Ledger.dashboard(summaries, paymentList, spendList, openLog, appSettings, date)
    }

    /** Lifetime earned vs spent, the monthly chart and spending by category for [period]. */
    fun observeMoneyOverview(period: Flow<InsightPeriod>): Flow<MoneyOverview> = combine(
        observeJobSummaries(),
        combine(payments.observeAll(), spends.observeAll(), ::Pair),
        settings.observeSettings(),
        period,
        today.observe(),
    ) { summaries, (paymentList, spendList), appSettings, selected, date ->
        Insights.overview(
            payments = paymentList,
            spends = spendList,
            owedTotal = summaries.sumOf(Ledger::owedOf),
            currency = appSettings.currency,
            monthlyBudget = appSettings.monthlyBudget,
            period = selected,
            today = date,
        )
    }

    /** The report for [year], or for the current year while [year] is null. */
    fun observeYearReport(year: Flow<Int?>): Flow<YearReportView> = combine(
        observeJobSummaries(),
        combine(payments.observeAll(), spends.observeAll(), ::Pair),
        settings.observeSettings(),
        year,
        today.observe(),
    ) { summaries, (paymentList, spendList), appSettings, selected, date ->
        val chosen = selected ?: date.year
        YearReportView(
            years = Reports.years(summaries, paymentList, spendList, date.year),
            report = Reports.yearReport(chosen, summaries, paymentList, spendList, appSettings.currency),
        )
    }

    /** Every payment and spend in [year], for the CSV export. */
    suspend fun reportRows(year: Int): List<ReportRow> =
        Reports.rows(year, observeJobSummaries().first(), payments.observeAll().first(), spends.observeAll().first())

    fun observeClientSummaries(): Flow<List<ClientSummary>> = combine(
        clients.observeClients(),
        observeJobSummaries(),
    ) { clientList, summaries ->
        Ledger.clientSummaries(clientList, summaries)
    }
}
