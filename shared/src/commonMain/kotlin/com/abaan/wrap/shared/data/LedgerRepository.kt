package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.model.ClientSummary
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.money.Insights
import com.abaan.wrap.shared.money.Ledger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

    fun observeClientSummaries(): Flow<List<ClientSummary>> = combine(
        clients.observeClients(),
        observeJobSummaries(),
    ) { clientList, summaries ->
        Ledger.clientSummaries(clientList, summaries)
    }
}
