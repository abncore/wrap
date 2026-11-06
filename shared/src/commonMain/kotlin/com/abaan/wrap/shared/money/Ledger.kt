package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.ClientSummary
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Pure functions that turn stored rows into what the screens show. */
object Ledger {

    fun summarize(
        job: Job,
        client: Client?,
        timeLogs: List<TimeLog>,
        payments: List<Payment>,
        today: LocalDate,
        timeZone: TimeZone,
    ): JobSummary {
        val money = MoneyRules.breakdown(job, timeLogs, payments, timeZone)
        val workDone = JobStatusRules.isWorkDone(job.status, job.endDate, timeLogs, today, timeZone)
        val status = JobStatusRules.storedStatus(workDone, money.total, money.paid)
        return JobSummary(
            job = job,
            client = client,
            money = money,
            status = JobStatusRules.displayStatus(status, money.owed, job.dueDate, today),
            workDone = workDone,
            workDoneAutomatically = JobStatusRules.isWorkDone(JobStatus.UPCOMING, job.endDate, timeLogs, today, timeZone),
            timeLogs = timeLogs,
            payments = payments,
            isOnToday = today in job.startDate..job.endDate,
            openLog = timeLogs.firstOrNull { it.clockOut == null },
        )
    }

    fun summarizeAll(
        jobs: List<Job>,
        clients: List<Client>,
        timeLogs: List<TimeLog>,
        payments: List<Payment>,
        today: LocalDate,
        timeZone: TimeZone,
    ): List<JobSummary> {
        val clientsById = clients.associateBy { it.id }
        val logsByJob = timeLogs.groupBy { it.jobId }
        val paymentsByJob = payments.groupBy { it.jobId }
        return jobs.map { job ->
            summarize(
                job = job,
                client = clientsById[job.clientId],
                timeLogs = logsByJob[job.id].orEmpty(),
                payments = paymentsByJob[job.id].orEmpty(),
                today = today,
                timeZone = timeZone,
            )
        }
    }

    /** Owed money only counts once the work is done; deposits on upcoming jobs are not "owed". */
    fun owedOf(summary: JobSummary): Long = if (summary.workDone) summary.money.owed else 0

    fun dashboard(
        summaries: List<JobSummary>,
        payments: List<Payment>,
        openLog: TimeLog?,
        settings: AppSettings,
        today: LocalDate,
    ): Dashboard {
        val overdue = summaries.filter { it.isOverdue }
        val todayJob = summaries
            .filter { it.canClock }
            .sortedWith(compareBy<JobSummary> { it.openLog == null }.thenBy { it.job.callTime })
            .firstOrNull()
        val nextJob = summaries
            .filter { !it.workDone && it.job.endDate >= today }
            .minWithOrNull(compareBy<JobSummary> { it.job.startDate }.thenBy { it.job.callTime })
        return Dashboard(
            currency = settings.currency,
            owedTotal = summaries.sumOf(::owedOf),
            unpaidCount = summaries.count { owedOf(it) > 0 },
            receivedThisMonth = payments
                .filter { it.dateReceived.year == today.year && it.dateReceived.month == today.month }
                .sumOf { it.amount },
            overdueCount = overdue.size,
            overdueTotal = overdue.sumOf { it.money.owed },
            todayJob = todayJob,
            nextJob = nextJob,
            openLog = openLog,
            hasAnyJobs = summaries.isNotEmpty(),
        )
    }

    fun clientSummaries(clients: List<Client>, summaries: List<JobSummary>): List<ClientSummary> {
        val byClient = summaries.groupBy { it.job.clientId }
        return clients.map { client ->
            val jobs = byClient[client.id].orEmpty()
            ClientSummary(
                client = client,
                owed = jobs.sumOf(::owedOf),
                overdue = jobs.any { it.isOverdue },
                jobCount = jobs.size,
            )
        }
    }
}
