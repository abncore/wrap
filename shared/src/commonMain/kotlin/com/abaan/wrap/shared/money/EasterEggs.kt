package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.CrewContact
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Fun numbers from the freelancer's own data, shown when the credits line is tapped. Null means "no data yet". */
data class BlooperReel(
    val minutesOnSet: Long,
    val daysOnSet: Int,
    val overtimeMinutes: Long,
    /** The longest single clocked shift. */
    val longestShiftMinutes: Long?,
    val longestShiftDate: LocalDate?,
    val longestShiftJob: String?,
    /** The earliest call time on any job. */
    val earliestCall: LocalTime?,
    val earliestCallJob: String?,
    val loyalClient: String?,
    val loyalClientJobs: Int,
    val biggestPayment: Long?,
    val biggestPaymentDate: LocalDate?,
    val topSpendCategory: SpendCategory?,
    val topSpendAmount: Long,
) {
    val isEmpty: Boolean get() = daysOnSet == 0 && minutesOnSet == 0L && biggestPayment == null && topSpendCategory == null
}

/** The end-credits roll, built from the freelancer's own work. */
data class EndCredits(
    /** Clients, most jobs first. */
    val producers: List<String>,
    /** Roles the freelancer has worked, most common first. */
    val roles: List<String>,
    val locations: List<String>,
    val crew: List<String>,
    val jobs: Int,
    val daysOnSet: Int,
    val cateringSpent: Long,
    val transportSpent: Long,
    val kitSpent: Long,
)

object EasterEggs {

    fun blooperReel(summaries: List<JobSummary>, payments: List<Payment>, spends: List<Spend>, timeZone: TimeZone): BlooperReel {
        val worked = summaries.filter { it.workDone }
        val longest = summaries.flatMap { summary -> summary.timeLogs.map { summary to it } }
            .filter { (_, log) -> log.clockOut != null }
            .maxByOrNull { (_, log) -> log.clockOut!! - log.clockIn }
        val earliest = summaries.filter { it.job.callTime != null }.minByOrNull { it.job.callTime!! }
        val loyal = summaries.filter { it.client != null }.groupBy { it.client!!.id }.maxByOrNull { it.value.size }?.value
        val biggest = payments.maxByOrNull { it.amount }
        val topSpend = spends.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }.maxByOrNull { it.value }
        return BlooperReel(
            minutesOnSet = summaries.sumOf { it.money.workedMinutes },
            daysOnSet = worked.sumOf { it.money.days },
            overtimeMinutes = summaries.sumOf { it.money.overtimeMinutes },
            longestShiftMinutes = longest?.let { (_, log) -> (log.clockOut!! - log.clockIn).inWholeMinutes },
            longestShiftDate = longest?.second?.clockIn?.toLocalDateTime(timeZone)?.date,
            longestShiftJob = longest?.first?.job?.title,
            earliestCall = earliest?.job?.callTime,
            earliestCallJob = earliest?.job?.title,
            loyalClient = loyal?.first()?.client?.name,
            loyalClientJobs = loyal?.size ?: 0,
            biggestPayment = biggest?.amount,
            biggestPaymentDate = biggest?.dateReceived,
            topSpendCategory = topSpend?.key,
            topSpendAmount = topSpend?.value ?: 0,
        )
    }

    fun endCredits(summaries: List<JobSummary>, spends: List<Spend>, crew: List<CrewContact>): EndCredits {
        fun spent(vararg categories: SpendCategory) = spends.filter { it.category in categories }.sumOf { it.amount }
        fun mostCommon(values: List<String>) =
            values.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("http") }
                .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        return EndCredits(
            producers = mostCommon(summaries.mapNotNull { it.client?.name }),
            roles = mostCommon(summaries.map { it.job.role }),
            locations = mostCommon(summaries.map { it.job.location }),
            crew = crew.map { contact -> listOf(contact.name.trim(), contact.role.trim()).filter { it.isNotEmpty() }.joinToString(" · ") }
                .filter { it.isNotEmpty() }
                .distinct(),
            jobs = summaries.size,
            daysOnSet = summaries.filter { it.workDone }.sumOf { it.money.days },
            cateringSpent = spent(SpendCategory.FOOD),
            transportSpent = spent(SpendCategory.FUEL, SpendCategory.TRANSPORT),
            kitSpent = spent(SpendCategory.KIT),
        )
    }
}
