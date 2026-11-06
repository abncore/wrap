package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.CategoryTotal
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import com.abaan.wrap.shared.model.MonthTotals
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.Spend
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Earnings and spending over time. "Earned" is money actually received (payments), so work
 * that hasn't been paid for yet is never counted as earned.
 */
object Insights {
    const val CHART_MONTHS = 6

    fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

    /** Totals for the [count] months ending with [today]'s month, oldest first. */
    fun monthlyTotals(
        payments: List<Payment>,
        spends: List<Spend>,
        today: LocalDate,
        count: Int = CHART_MONTHS,
    ): List<MonthTotals> {
        val earnedByMonth = payments.groupBy { firstOfMonth(it.dateReceived) }
        val spentByMonth = spends.groupBy { firstOfMonth(it.date) }
        val current = firstOfMonth(today)
        return (count - 1 downTo 0).map { back ->
            val month = current.minus(DatePeriod(months = back))
            MonthTotals(
                month = month,
                earned = earnedByMonth[month].orEmpty().sumOf { it.amount },
                spent = spentByMonth[month].orEmpty().sumOf { it.amount },
            )
        }
    }

    fun isInPeriod(date: LocalDate, period: InsightPeriod, today: LocalDate): Boolean = when (period) {
        InsightPeriod.THIS_MONTH -> date.year == today.year && date.month == today.month
        InsightPeriod.THIS_YEAR -> date.year == today.year
        InsightPeriod.ALL_TIME -> true
    }

    /** Spending per category in [period], largest first; categories with nothing spent are left out. */
    fun categoryTotals(spends: List<Spend>, period: InsightPeriod, today: LocalDate): List<CategoryTotal> {
        val inPeriod = spends.filter { isInPeriod(it.date, period, today) }
        val total = inPeriod.sumOf { it.amount }
        if (total == 0L) return emptyList()
        return inPeriod
            .groupBy { it.category }
            .map { (category, items) ->
                val amount = items.sumOf { it.amount }
                CategoryTotal(category, amount, amount.toDouble() / total)
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.amount }.thenBy { it.category.ordinal })
    }

    fun overview(
        payments: List<Payment>,
        spends: List<Spend>,
        owedTotal: Long,
        currency: String,
        period: InsightPeriod,
        today: LocalDate,
    ): MoneyOverview {
        val months = monthlyTotals(payments, spends, today)
        val categories = categoryTotals(spends, period, today)
        return MoneyOverview(
            currency = currency,
            earnedLifetime = payments.sumOf { it.amount },
            spentLifetime = spends.sumOf { it.amount },
            owedTotal = owedTotal,
            thisMonth = months.last(),
            months = months,
            period = period,
            periodSpent = categories.sumOf { it.amount },
            categories = categories,
            spends = spends.sortedWith(compareByDescending<Spend> { it.date }.thenByDescending { it.id }),
        )
    }
}
