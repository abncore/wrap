package com.abaan.wrap.shared.model

import kotlinx.datetime.LocalDate

/** Earned (payments received) and spent in one calendar month. [month] is its first day. */
data class MonthTotals(
    val month: LocalDate,
    val earned: Long,
    val spent: Long,
) {
    val net: Long get() = earned - spent
}

/** This month's spending against the monthly budget. Amounts are minor units. */
data class BudgetStatus(
    val budget: Long,
    val spent: Long,
    /** Negative when over budget. */
    val remaining: Long,
    /** spent / budget; above 1 when over. */
    val share: Double,
    val level: BudgetLevel,
    /** Days left in the month, counting today. */
    val daysLeft: Int,
    /** What can still be spent per day for the rest of the month; 0 when nothing is left. */
    val dailyAllowance: Long,
)

/** Spending in one category over the selected period, with its share of the period's total. */
data class CategoryTotal(
    val category: SpendCategory,
    val amount: Long,
    /** 0..1 */
    val share: Double,
)

/** Everything the Money screen shows. Amounts are minor units. */
data class MoneyOverview(
    val currency: String,
    val earnedLifetime: Long,
    val spentLifetime: Long,
    /** Money still owed on finished jobs: earned but not received yet. */
    val owedTotal: Long,
    val thisMonth: MonthTotals,
    /** Null when no monthly budget is set. */
    val budget: BudgetStatus?,
    /** The last few months, oldest first, ending with the current month. */
    val months: List<MonthTotals>,
    val period: InsightPeriod,
    val periodSpent: Long,
    /** Categories with spending in [period], largest first. */
    val categories: List<CategoryTotal>,
    /** All spends, newest first. */
    val spends: List<Spend>,
) {
    val netLifetime: Long get() = earnedLifetime - spentLifetime
}
