package com.abaan.wrap.shared.data

import com.abaan.wrap.shared.model.ClientSummary
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.JobSummary
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
    private val today: Today,
) {
    fun observeJobSummaries(): Flow<List<JobSummary>> = combine(
        jobs.observeJobs(),
        clients.observeClients(),
        timeLogs.observeAll(),
        payments.observeAll(),
        today.observe(),
    ) { jobList, clientList, logList, paymentList, date ->
        Ledger.summarizeAll(jobList, clientList, logList, paymentList, date, today.timeZone)
    }

    fun observeJobSummary(jobId: Long): Flow<JobSummary?> =
        observeJobSummaries().map { list -> list.firstOrNull { it.job.id == jobId } }.distinctUntilChanged()

    fun observeDashboard(): Flow<Dashboard> = combine(
        observeJobSummaries(),
        payments.observeAll(),
        timeLogs.observeOpenLog(),
        settings.observeSettings(),
        today.observe(),
    ) { summaries, paymentList, openLog, appSettings, date ->
        Ledger.dashboard(summaries, paymentList, openLog, appSettings, date)
    }

    fun observeClientSummaries(): Flow<List<ClientSummary>> = combine(
        clients.observeClients(),
        observeJobSummaries(),
    ) { clientList, summaries ->
        Ledger.clientSummaries(clientList, summaries)
    }
}
