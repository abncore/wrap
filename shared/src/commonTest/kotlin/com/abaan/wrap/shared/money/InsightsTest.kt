package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.model.SpendCategory
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsTest {
    private val today = LocalDate(2026, 3, 15)

    private fun pay(amount: Long, date: LocalDate) =
        Payment(jobId = 1, amount = amount, dateReceived = date, method = PaymentMethod.BANK)

    private fun spend(id: Long, amount: Long, category: SpendCategory, date: LocalDate) =
        Spend(id = id, amount = amount, category = category, date = date)

    @Test
    fun monthlyTotalsCoverSixMonthsOldestFirstAcrossYearBoundary() {
        val months = Insights.monthlyTotals(
            payments = listOf(pay(1_000_00, LocalDate(2026, 3, 2)), pay(500_00, LocalDate(2025, 10, 31))),
            spends = listOf(spend(1, 200_00, SpendCategory.FUEL, LocalDate(2026, 1, 9))),
            today = today,
        )
        assertEquals(
            listOf(LocalDate(2025, 10, 1), LocalDate(2025, 11, 1), LocalDate(2025, 12, 1), LocalDate(2026, 1, 1), LocalDate(2026, 2, 1), LocalDate(2026, 3, 1)),
            months.map { it.month },
        )
        assertEquals(500_00, months.first().earned)
        assertEquals(200_00, months[3].spent)
        assertEquals(1_000_00, months.last().earned)
        assertEquals(1_000_00, months.last().net)
    }

    @Test
    fun monthsOutsideTheChartAreIgnored() {
        val months = Insights.monthlyTotals(listOf(pay(1_000_00, LocalDate(2025, 9, 30))), emptyList(), today)
        assertTrue(months.all { it.earned == 0L })
    }

    @Test
    fun categoryTotalsAreLargestFirstWithShares() {
        val spends = listOf(
            spend(1, 100_00, SpendCategory.FOOD, LocalDate(2026, 3, 1)),
            spend(2, 250_00, SpendCategory.FUEL, LocalDate(2026, 3, 3)),
            spend(3, 150_00, SpendCategory.FUEL, LocalDate(2026, 3, 4)),
            spend(4, 100_00, SpendCategory.KIT, LocalDate(2026, 2, 20)),
        )
        val month = Insights.categoryTotals(spends, InsightPeriod.THIS_MONTH, today)
        assertEquals(listOf(SpendCategory.FUEL, SpendCategory.FOOD), month.map { it.category })
        assertEquals(400_00, month[0].amount)
        assertEquals(0.8, month[0].share, 1e-9)

        val year = Insights.categoryTotals(spends, InsightPeriod.THIS_YEAR, today)
        assertEquals(3, year.size)
        // Ties keep category order: FOOD comes before KIT.
        assertEquals(listOf(SpendCategory.FUEL, SpendCategory.FOOD, SpendCategory.KIT), year.map { it.category })
    }

    @Test
    fun noSpendingGivesNoCategories() {
        assertEquals(emptyList(), Insights.categoryTotals(emptyList(), InsightPeriod.ALL_TIME, today))
    }

    @Test
    fun overviewTotalsLifetimeAndPeriod() {
        val overview = Insights.overview(
            payments = listOf(pay(1_000_00, LocalDate(2024, 6, 1)), pay(2_000_00, LocalDate(2026, 3, 10))),
            spends = listOf(
                spend(1, 300_00, SpendCategory.PERMITS, LocalDate(2025, 1, 5)),
                spend(2, 50_00, SpendCategory.FOOD, LocalDate(2026, 3, 10)),
                spend(3, 20_00, SpendCategory.FOOD, LocalDate(2026, 3, 10)),
            ),
            owedTotal = 750_00,
            currency = "AED",
            period = InsightPeriod.ALL_TIME,
            today = today,
        )
        assertEquals(3_000_00, overview.earnedLifetime)
        assertEquals(370_00, overview.spentLifetime)
        assertEquals(2_630_00, overview.netLifetime)
        assertEquals(370_00, overview.periodSpent)
        assertEquals(2_000_00, overview.thisMonth.earned)
        assertEquals(70_00, overview.thisMonth.spent)
        assertEquals(750_00, overview.owedTotal)
        // Newest first; same day falls back to newest id.
        assertEquals(listOf(3L, 2L, 1L), overview.spends.map { it.id })
    }
}
