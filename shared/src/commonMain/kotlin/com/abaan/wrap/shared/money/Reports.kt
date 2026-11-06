package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.CategoryTotal
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.MonthTotals
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.math.abs

/** Money received from one client in a year. [clientName] is empty for jobs whose client was removed. */
data class ClientIncome(
    val clientId: Long?,
    val clientName: String,
    val received: Long,
    val jobCount: Int,
)

/** What a year looked like, for tax time. Amounts are minor units. */
data class YearReport(
    val year: Int,
    val currency: String,
    /** Payments received during the year. */
    val received: Long,
    /** Totals of jobs finishing in the year, whether paid or not. */
    val invoiced: Long,
    /** VAT on jobs finishing in the year. */
    val vatCharged: Long,
    val spent: Long,
    /** Of [spent], what was billed back to clients. */
    val billedSpent: Long,
    /** Still owed on jobs finishing in the year. */
    val owed: Long,
    /** Largest first. */
    val byClient: List<ClientIncome>,
    /** Largest first. */
    val byCategory: List<CategoryTotal>,
    /** January to December. */
    val months: List<MonthTotals>,
) {
    val net: Long get() = received - spent
}

enum class ReportRowType { INCOME, SPEND }

/** One line of the CSV export: a payment received or a spend. */
data class ReportRow(
    val date: LocalDate,
    val type: ReportRowType,
    /** Job title for income, the note for a spend. */
    val description: String,
    /** Client name for income. */
    val clientName: String = "",
    val category: SpendCategory? = null,
    /** Positive for income, negative for spending. */
    val amount: Long,
    val billedToJob: String = "",
)

object Reports {

    /** Years that have any payment, spend or job, newest first; always includes [currentYear]. */
    fun years(summaries: List<JobSummary>, payments: List<Payment>, spends: List<Spend>, currentYear: Int): List<Int> =
        (
            payments.map { it.dateReceived.year } +
                spends.map { it.date.year } +
                summaries.map { it.job.endDate.year } +
                currentYear
            ).distinct().sortedDescending()

    fun yearReport(
        year: Int,
        summaries: List<JobSummary>,
        payments: List<Payment>,
        spends: List<Spend>,
        currency: String,
    ): YearReport {
        val jobsById = summaries.associateBy { it.job.id }
        val yearPayments = payments.filter { it.dateReceived.year == year }
        val yearSpends = spends.filter { it.date.year == year }
        val finished = summaries.filter { it.job.endDate.year == year }

        val byClient = yearPayments
            .groupBy { jobsById[it.jobId]?.client?.id }
            .map { (clientId, list) ->
                ClientIncome(
                    clientId = clientId,
                    clientName = list.firstNotNullOfOrNull { jobsById[it.jobId]?.client?.name }.orEmpty(),
                    received = list.sumOf { it.amount },
                    jobCount = list.map { it.jobId }.distinct().size,
                )
            }
            .sortedByDescending { it.received }

        val spent = yearSpends.sumOf { it.amount }
        return YearReport(
            year = year,
            currency = currency,
            received = yearPayments.sumOf { it.amount },
            invoiced = finished.sumOf { it.money.total },
            vatCharged = finished.sumOf { it.money.vat },
            spent = spent,
            billedSpent = yearSpends.filter { it.jobId != null }.sumOf { it.amount },
            owed = finished.sumOf { it.money.owed },
            byClient = byClient,
            byCategory = Insights.categoryTotals(yearSpends, InsightPeriod.ALL_TIME, LocalDate(year, 12, 31)),
            months = Month.entries.map { month ->
                MonthTotals(
                    month = LocalDate(year, month, 1),
                    earned = yearPayments.filter { it.dateReceived.month == month }.sumOf { it.amount },
                    spent = yearSpends.filter { it.date.month == month }.sumOf { it.amount },
                )
            },
        )
    }

    /** Every payment and spend in [year], oldest first, for the CSV export. */
    fun rows(year: Int, summaries: List<JobSummary>, payments: List<Payment>, spends: List<Spend>): List<ReportRow> {
        val jobsById = summaries.associateBy { it.job.id }
        val income = payments.filter { it.dateReceived.year == year }.map { payment ->
            val summary = jobsById[payment.jobId]
            ReportRow(
                date = payment.dateReceived,
                type = ReportRowType.INCOME,
                description = summary?.job?.title.orEmpty(),
                clientName = summary?.client?.name.orEmpty(),
                amount = payment.amount,
            )
        }
        val spending = spends.filter { it.date.year == year }.map { spend ->
            ReportRow(
                date = spend.date,
                type = ReportRowType.SPEND,
                description = spend.note,
                category = spend.category,
                amount = -spend.amount,
                billedToJob = spend.jobId?.let { jobsById[it]?.job?.title }.orEmpty(),
            )
        }
        return (income + spending).sortedWith(compareBy<ReportRow> { it.date }.thenBy { it.type })
    }
}

/** Comma-separated values that open cleanly in Excel and Google Sheets. */
object Csv {
    /** Quotes a field when it contains a comma, quote or line break. */
    fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${value.replace("\"", "\"\"")}\"" else value

    fun line(values: List<String>): String = values.joinToString(",") { field(it) }

    /** Minor units as a plain number with two decimals: -12345 -> "-123.45". */
    fun amount(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val value = abs(minor)
        return "$sign${value / 100}.${(value % 100).toString().padStart(2, '0')}"
    }
}
