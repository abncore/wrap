package com.abaan.wrap.shared.money

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurringRulesTest {

    @Test
    fun shortMonthsUseTheirLastDayAndLongerOnesGoBack() {
        assertEquals(LocalDate(2026, 2, 28), RecurringRules.nextAfter(LocalDate(2026, 1, 31), 31))
        assertEquals(LocalDate(2026, 3, 31), RecurringRules.nextAfter(LocalDate(2026, 2, 28), 31))
        assertEquals(LocalDate(2028, 2, 29), RecurringRules.dateInMonth(LocalDate(2028, 2, 1), 30))
    }

    @Test
    fun theYearRollsOver() {
        assertEquals(LocalDate(2027, 1, 5), RecurringRules.nextAfter(LocalDate(2026, 12, 5), 5))
    }

    @Test
    fun dueDatesCatchUpOnMissedMonthsUpToToday() {
        assertEquals(
            listOf(LocalDate(2026, 1, 15), LocalDate(2026, 2, 15), LocalDate(2026, 3, 15)),
            RecurringRules.dueDates(LocalDate(2026, 1, 15), 15, today = LocalDate(2026, 3, 15)),
        )
        assertEquals(emptyList(), RecurringRules.dueDates(LocalDate(2026, 3, 16), 16, today = LocalDate(2026, 3, 15)))
    }
}
