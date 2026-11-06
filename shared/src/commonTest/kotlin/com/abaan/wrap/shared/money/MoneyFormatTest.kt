package com.abaan.wrap.shared.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyFormatTest {
    @Test
    fun formatsWithCurrencyGroupingAndTwoDecimals() {
        assertEquals("AED 0.00", MoneyFormat.format(0, "AED"))
        assertEquals("AED 0.05", MoneyFormat.format(5, "AED"))
        assertEquals("AED 999.99", MoneyFormat.format(999_99, "AED"))
        assertEquals("AED 1,250.50", MoneyFormat.format(1_250_50, "AED"))
        assertEquals("USD 1,234,567.00", MoneyFormat.format(1_234_567_00, "USD"))
        assertEquals("-AED 10.00", MoneyFormat.format(-10_00, "AED"))
    }

    @Test
    fun toInputDropsZeroFraction() {
        assertEquals("1500", MoneyFormat.toInput(1_500_00))
        assertEquals("1500.50", MoneyFormat.toInput(1_500_50))
        assertEquals("0.05", MoneyFormat.toInput(5))
    }

    @Test
    fun parsesCommonInputs() {
        assertEquals(1_500_00, MoneyFormat.parse("1500"))
        assertEquals(1_250_50, MoneyFormat.parse("1,250.5"))
        assertEquals(1_250_55, MoneyFormat.parse(" 1250.55 "))
        assertEquals(75, MoneyFormat.parse(".75"))
        assertEquals(12_00, MoneyFormat.parse("12."))
        assertEquals(0, MoneyFormat.parse("0"))
    }

    @Test
    fun rejectsInvalidInputs() {
        assertNull(MoneyFormat.parse(""))
        assertNull(MoneyFormat.parse("abc"))
        assertNull(MoneyFormat.parse("-5"))
        assertNull(MoneyFormat.parse("1.234"))
        assertNull(MoneyFormat.parse("1.2.3"))
        assertNull(MoneyFormat.parse("."))
        assertNull(MoneyFormat.parse("99999999999999999999"))
    }

    @Test
    fun parseRoundTripsToInput() {
        listOf(0L, 5L, 1_00L, 1_250_50L, 987_654_32L).forEach { minor ->
            assertEquals(minor, MoneyFormat.parse(MoneyFormat.toInput(minor)))
        }
    }
}
