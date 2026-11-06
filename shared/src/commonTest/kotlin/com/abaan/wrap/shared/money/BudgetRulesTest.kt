package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.BudgetLevel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BudgetRulesTest {
    private val budget = 2_000_00L

    @Test
    fun levelsAtEightyPercentAndOver() {
        assertEquals(BudgetLevel.UNDER, BudgetRules.level(1_599_99, budget))
        assertEquals(BudgetLevel.NEAR, BudgetRules.level(1_600_00, budget))
        assertEquals(BudgetLevel.NEAR, BudgetRules.level(2_000_00, budget))
        assertEquals(BudgetLevel.OVER, BudgetRules.level(2_000_01, budget))
        assertEquals(BudgetLevel.UNDER, BudgetRules.level(5_000_00, 0))
    }

    @Test
    fun daysLeftCountsTodayAndHandlesFebruary() {
        assertEquals(25, BudgetRules.daysLeftInMonth(LocalDate(2026, 11, 6)))
        assertEquals(1, BudgetRules.daysLeftInMonth(LocalDate(2026, 12, 31)))
        assertEquals(29, BudgetRules.daysLeftInMonth(LocalDate(2028, 2, 1)))
    }

    @Test
    fun statusGivesRemainingAndDailyAllowance() {
        val status = BudgetRules.status(budget, 250_00, LocalDate(2026, 11, 6))!!
        assertEquals(1_750_00, status.remaining)
        assertEquals(70_00, status.dailyAllowance)
        assertEquals(0.125, status.share, 1e-9)

        val over = BudgetRules.status(budget, 2_100_00, LocalDate(2026, 11, 6))!!
        assertEquals(-100_00, over.remaining)
        assertEquals(0, over.dailyAllowance)
        assertEquals(BudgetLevel.OVER, over.level)

        assertNull(BudgetRules.status(0, 250_00, LocalDate(2026, 11, 6)))
    }

    @Test
    fun alertsOnlyWhenALevelIsCrossedUpwards() {
        assertEquals(BudgetLevel.NEAR, BudgetRules.alertFor(1_500_00, 1_700_00, budget))
        assertEquals(BudgetLevel.OVER, BudgetRules.alertFor(1_700_00, 2_100_00, budget))
        // Straight past both thresholds: one alert, for going over.
        assertEquals(BudgetLevel.OVER, BudgetRules.alertFor(100_00, 2_500_00, budget))
        // Already near or over: no repeat.
        assertNull(BudgetRules.alertFor(1_700_00, 1_800_00, budget))
        assertNull(BudgetRules.alertFor(2_100_00, 2_200_00, budget))
        // Spending went down (edit or delete), or no budget.
        assertNull(BudgetRules.alertFor(2_100_00, 1_000_00, budget))
        assertNull(BudgetRules.alertFor(0, 9_000_00, 0))
    }
}
