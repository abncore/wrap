package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.BudgetStatus
import com.abaan.wrap.shared.model.Spend
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/** Monthly spending budget: how much is left and when to warn. A budget of 0 means none is set. */
object BudgetRules {
    /** Share of the budget at which spending counts as "nearly there". */
    const val NEAR_SHARE = 0.8

    fun level(spent: Long, budget: Long): BudgetLevel = when {
        budget <= 0 -> BudgetLevel.UNDER
        spent > budget -> BudgetLevel.OVER
        spent >= budget * NEAR_SHARE -> BudgetLevel.NEAR
        else -> BudgetLevel.UNDER
    }

    /** Days from [today] to the end of its month, counting today. */
    fun daysLeftInMonth(today: LocalDate): Int {
        val nextMonth = Insights.firstOfMonth(today).plus(DatePeriod(months = 1))
        return today.daysUntil(nextMonth)
    }

    fun spentInMonth(spends: List<Spend>, today: LocalDate): Long =
        spends.filter { it.date.year == today.year && it.date.month == today.month }.sumOf { it.amount }

    /** Status of this month's budget, or null when no budget is set. */
    fun status(budget: Long, spent: Long, today: LocalDate): BudgetStatus? {
        if (budget <= 0) return null
        val remaining = budget - spent
        val daysLeft = daysLeftInMonth(today)
        return BudgetStatus(
            budget = budget,
            spent = spent,
            remaining = remaining,
            share = spent.toDouble() / budget,
            level = level(spent, budget),
            daysLeft = daysLeft,
            dailyAllowance = if (remaining > 0) remaining / daysLeft else 0,
        )
    }

    /**
     * The alert to send after this month's spending went from [before] to [after]: NEAR the first
     * time 80% is reached and OVER the first time the budget is passed. Null when nothing new happened.
     */
    fun alertFor(before: Long, after: Long, budget: Long): BudgetLevel? {
        if (budget <= 0 || after <= before) return null
        val was = level(before, budget)
        val now = level(after, budget)
        return now.takeIf { it != BudgetLevel.UNDER && it.ordinal > was.ordinal }
    }
}
