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
