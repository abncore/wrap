package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.RateType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** How one client has been to work for. Amounts are minor units; only finished work counts. */
data class ClientPerformance(
    val client: Client,
    val jobs: Int,
    val daysWorked: Int,
    /** Totals of finished jobs, VAT included. */
    val billed: Long,
    val received: Long,
    val owed: Long,
    val overdue: Long,
    /** Share of all money received in the period, 0..1. */
    val shareOfIncome: Double,
    /** Billed before VAT, per day worked; null with no days. */
    val earningsPerDay: Long?,
    /** Average day rate on day-rate jobs; null without any. */
    val averageDayRate: Long?,
    /** Average days from a job's last day to it being paid in full; null when nothing is fully paid. */
    val averageDaysToPay: Int?,
    /** Share of fully paid jobs (with a due date) that were paid by it, 0..1; null when none. */
    val onTimeShare: Double?,
    val firstJob: LocalDate?,
    val lastJob: LocalDate?,
    /** The client's finished jobs in the period, newest first. */
    val jobSummaries: List<JobSummary>,
)

/** How overdue money owed is. NOT_DUE is owed but still within the payment terms. */
enum class AgeBucket { NOT_DUE, DAYS_1_30, DAYS_31_60, DAYS_61_90, OVER_90 }

data class OwedJob(val summary: JobSummary, val bucket: AgeBucket, val daysOverdue: Int)

data class ClientOwed(
    val client: Client?,
    val clientName: String,
    val total: Long,
    val overdue: Long,
    /** Longest any of their jobs is overdue, in days. */
    val oldestDays: Int,
)

/** Who owes you and for how long. */
data class AgingReport(
    val total: Long,
    val byBucket: Map<AgeBucket, Long>,
    /** Most overdue first. */
    val clients: List<ClientOwed>,
    /** Most overdue first. */
    val jobs: List<OwedJob>,
)

data class RoleTotal(val role: String, val jobs: Int, val days: Int, val billed: Long)

/** Days and earnings worked per month (first day of each month), oldest first. */
data class MonthWork(val month: LocalDate, val days: Int, val billed: Long)

/** How much work there was, and what it paid, over a period. Only finished jobs count. */
data class WorkReport(
    val period: InsightPeriod,
    val jobs: Int,
    val daysWorked: Int,
    val minutesLogged: Long,
    val overtimeMinutes: Long,
    val overtimePay: Long,
    /** Before VAT. */
    val billed: Long,
    val averageDayRate: Long?,
    val earningsPerDay: Long?,
    val averagePerJob: Long?,
    /** Largest first; jobs without a role are grouped under an empty role. */
    val byRole: List<RoleTotal>,
    /** The last 12 months, oldest first, for the chart. */
    val months: List<MonthWork>,
    val busiestMonth: MonthWork?,
)

object PerformanceReports {

    /** The date [summary] was paid in full, or null while money is still owed. */
    fun paidInFullOn(summary: JobSummary): LocalDate? {
        if (summary.money.owed > 0 || summary.payments.isEmpty()) return null
        var paid = 0L
        summary.payments.sortedBy { it.dateReceived }.forEach { payment ->
            paid += payment.amount
            if (paid >= summary.money.total) return payment.dateReceived
        }
        return null
    }

    fun clients(
        clients: List<Client>,
        summaries: List<JobSummary>,
        period: InsightPeriod,
        today: LocalDate,
    ): List<ClientPerformance> {
        val finished = finishedIn(summaries, period, today)
        val totalReceived = finished.sumOf { it.money.paid }
        return clients.map { client -> client(client, finished.filter { it.job.clientId == client.id }, totalReceived, today) }
            .filter { it.jobs > 0 }
            .sortedWith(compareByDescending<ClientPerformance> { it.received }.thenByDescending { it.billed })
    }

    /** Report for a single client over [period]; jobs with no work done yet are left out. */
    fun client(client: Client, summaries: List<JobSummary>, period: InsightPeriod, today: LocalDate): ClientPerformance {
        val finished = finishedIn(summaries, period, today)
        return client(client, finished.filter { it.job.clientId == client.id }, finished.sumOf { it.money.paid }, today)
    }

    private fun client(client: Client, jobs: List<JobSummary>, totalReceived: Long, today: LocalDate): ClientPerformance {
        val days = jobs.sumOf { it.money.days }
        val dayRates = jobs.filter { it.job.rateType == RateType.DAY }.map { it.job.rate }
        val paid = jobs.mapNotNull { summary -> paidInFullOn(summary)?.let { summary to it } }
        val withDueDate = paid.filter { (summary, _) -> summary.job.dueDate != null }
        val received = jobs.sumOf { it.money.paid }
        return ClientPerformance(
            client = client,
            jobs = jobs.size,
            daysWorked = days,
            billed = jobs.sumOf { it.money.total },
            received = received,
            owed = jobs.sumOf { it.money.owed },
            overdue = jobs.filter { it.isOverdue }.sumOf { it.money.owed },
            shareOfIncome = if (totalReceived > 0) received.toDouble() / totalReceived else 0.0,
            earningsPerDay = jobs.sumOf { it.money.subtotal }.takeIf { days > 0 }?.let { divideRounded(it, days) },
            averageDayRate = dayRates.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            averageDaysToPay = paid.takeIf { it.isNotEmpty() }
                ?.map { (summary, on) -> summary.job.endDate.daysUntil(on).coerceAtLeast(0) }
                ?.average()?.roundToInt(),
            onTimeShare = withDueDate.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { (summary, on) -> on <= summary.job.dueDate!! }.toDouble() / list.size },
            firstJob = jobs.minOfOrNull { it.job.startDate },
            lastJob = jobs.maxOfOrNull { it.job.endDate },
            jobSummaries = jobs.sortedByDescending { it.job.startDate },
        )
    }

    fun bucketFor(daysOverdue: Int): AgeBucket = when {
        daysOverdue <= 0 -> AgeBucket.NOT_DUE
        daysOverdue <= 30 -> AgeBucket.DAYS_1_30
        daysOverdue <= 60 -> AgeBucket.DAYS_31_60
        daysOverdue <= 90 -> AgeBucket.DAYS_61_90
        else -> AgeBucket.OVER_90
    }

    /** Everything owed on finished work, sorted by how long it has been overdue. */
    fun aging(summaries: List<JobSummary>, today: LocalDate): AgingReport {
        val jobs = summaries.filter { Ledger.owedOf(it) > 0 }.map { summary ->
            val days = summary.job.dueDate?.daysUntil(today)?.coerceAtLeast(0) ?: 0
            OwedJob(summary, bucketFor(days), days)
        }.sortedWith(compareByDescending<OwedJob> { it.daysOverdue }.thenByDescending { it.summary.money.owed })
        val clients = jobs.groupBy { it.summary.job.clientId }.map { (_, list) ->
            ClientOwed(
                client = list.first().summary.client,
                clientName = list.first().summary.client?.name.orEmpty(),
                total = list.sumOf { it.summary.money.owed },
                overdue = list.filter { it.bucket != AgeBucket.NOT_DUE }.sumOf { it.summary.money.owed },
                oldestDays = list.maxOf { it.daysOverdue },
            )
        }.sortedWith(compareByDescending<ClientOwed> { it.oldestDays }.thenByDescending { it.total })
        return AgingReport(
            total = jobs.sumOf { it.summary.money.owed },
            byBucket = AgeBucket.entries.associateWith { bucket -> jobs.filter { it.bucket == bucket }.sumOf { it.summary.money.owed } },
            clients = clients,
            jobs = jobs,
        )
    }

    fun work(summaries: List<JobSummary>, period: InsightPeriod, today: LocalDate): WorkReport {
        val finished = finishedIn(summaries, period, today)
        val days = finished.sumOf { it.money.days }
        val billed = finished.sumOf { it.money.subtotal }
        val dayRates = finished.filter { it.job.rateType == RateType.DAY }.map { it.job.rate }
        val thisMonth = Insights.firstOfMonth(today)
        val allFinished = summaries.filter { it.workDone }
        val months = (11 downTo 0).map { back ->
            val month = thisMonth.minus(DatePeriod(months = back))
            val inMonth = allFinished.filter { Insights.firstOfMonth(it.job.endDate) == month }
            MonthWork(month, inMonth.sumOf { it.money.days }, inMonth.sumOf { it.money.subtotal })
        }
        return WorkReport(
            period = period,
            jobs = finished.size,
            daysWorked = days,
            minutesLogged = finished.sumOf { it.money.workedMinutes },
            overtimeMinutes = finished.sumOf { it.money.overtimeMinutes },
            overtimePay = finished.sumOf { it.money.overtime },
            billed = billed,
            averageDayRate = dayRates.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            earningsPerDay = billed.takeIf { days > 0 }?.let { divideRounded(it, days) },
            averagePerJob = billed.takeIf { finished.isNotEmpty() }?.let { divideRounded(it, finished.size) },
            byRole = finished.groupBy { it.job.role.trim() }
                .map { (role, list) -> RoleTotal(role, list.size, list.sumOf { it.money.days }, list.sumOf { it.money.subtotal }) }
                .sortedByDescending { it.billed },
            months = months,
            busiestMonth = months.filter { it.days > 0 }.maxByOrNull { it.days },
        )
    }

    private fun divideRounded(value: Long, by: Int): Long = (value + by / 2) / by

    /** Finished jobs whose last day falls in [period]. */
    private fun finishedIn(summaries: List<JobSummary>, period: InsightPeriod, today: LocalDate): List<JobSummary> =
        summaries.filter { it.workDone && Insights.isInPeriod(it.job.endDate, period, today) }
}
