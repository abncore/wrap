package com.abaan.wrap.shared.money

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyRulesTest {
    private val dubai = TimeZone.of("Asia/Dubai")

    private fun calc(
        rateType: RateType,
        rate: Long = 1_000_00,
        days: Int = 1,
        dayLengthHours: Double = 10.0,
        otMultiplier: Double = 1.5,
        minutesPerDay: List<Long> = emptyList(),
        kitRental: Long = 0,
        expenses: Long = 0,
        paid: Long = 0,
    ) = MoneyRules.calculate(rateType, rate, days, dayLengthHours, otMultiplier, minutesPerDay, kitRental, expenses, paid)

    @Test
    fun dayRateIsRateTimesDays() {
        assertEquals(1_000_00, calc(RateType.DAY).base)
        assertEquals(3_000_00, calc(RateType.DAY, days = 3).base)
    }

    @Test
    fun halfDayIsHalfTheRatePerDay() {
        assertEquals(500_00, calc(RateType.HALF_DAY).base)
        assertEquals(1_500_00, calc(RateType.HALF_DAY, days = 3).base)
    }

    @Test
    fun halfDayRoundsHalfFilsUp() {
        assertEquals(50, calc(RateType.HALF_DAY, rate = 99).base)
    }

    @Test
    fun hourlyUsesLoggedHours() {
        // 7h 30m at AED 120/h = AED 900
        val result = calc(RateType.HOUR, rate = 120_00, minutesPerDay = listOf(450))
        assertEquals(900_00, result.base)
        assertEquals(450, result.workedMinutes)
    }

    @Test
    fun hourlyWithoutLogsEstimatesFromDayLength() {
        // 2 days x 8h x AED 100 = AED 1,600
        assertEquals(1_600_00, calc(RateType.HOUR, rate = 100_00, days = 2, dayLengthHours = 8.0).base)
    }

    @Test
    fun hourlyRoundsToNearestFils() {
        // 10 minutes at 1.00/h = 0.1666.. -> 17 fils
        assertEquals(17, calc(RateType.HOUR, rate = 100, minutesPerDay = listOf(10)).base)
    }

    @Test
    fun fixedIgnoresDaysAndHours() {
        val result = calc(RateType.FIXED, rate = 5_000_00, days = 4, minutesPerDay = listOf(900, 900))
        assertEquals(5_000_00, result.base)
        assertEquals(0, result.overtime)
    }

    @Test
    fun overtimeIsHoursBeyondDayLengthAtMultipliedHourlyRate() {
        // AED 1,000 / 10h = AED 100/h; 2h OT x 1.5 = AED 300
        val result = calc(RateType.DAY, minutesPerDay = listOf(12 * 60))
        assertEquals(120, result.overtimeMinutes)
        assertEquals(300_00, result.overtime)
        assertEquals(1_300_00, result.total)
    }

    @Test
    fun overtimeIsWorkedOutPerDay() {
        // Day 1: 11h (1h OT), day 2: 9h (no OT, not offset against day 1)
        val result = calc(RateType.DAY, days = 2, minutesPerDay = listOf(11 * 60, 9 * 60))
        assertEquals(60, result.overtimeMinutes)
        assertEquals(150_00, result.overtime)
    }

    @Test
    fun overtimeHandlesPartialHoursAndOddDayLengths() {
        // AED 1,500 over 12h = 125/h; 45m OT x 2.0 = 187.50
        val result = calc(RateType.DAY, rate = 1_500_00, dayLengthHours = 12.0, otMultiplier = 2.0, minutesPerDay = listOf(12 * 60 + 45))
        assertEquals(187_50, result.overtime)
    }

    @Test
    fun noOvertimeForNonDayRates() {
        assertEquals(0, calc(RateType.HALF_DAY, minutesPerDay = listOf(14 * 60)).overtime)
        assertEquals(0, calc(RateType.HOUR, minutesPerDay = listOf(14 * 60)).overtime)
    }

    @Test
    fun noOvertimeWhenDayLengthIsZero() {
        assertEquals(0, calc(RateType.DAY, dayLengthHours = 0.0, minutesPerDay = listOf(600)).overtime)
    }

    @Test
    fun totalAddsKitAndExpenses() {
        val result = calc(RateType.DAY, kitRental = 400_00, expenses = 85_50)
        assertEquals(1_485_50, result.total)
    }

    @Test
    fun owedIsTotalMinusPayments() {
        val result = calc(RateType.DAY, paid = 250_00)
        assertEquals(750_00, result.owed)
        assertEquals(0, result.credit)
    }

    @Test
    fun overpaymentBecomesCreditNotNegativeOwed() {
        val result = calc(RateType.DAY, paid = 1_100_00)
        assertEquals(0, result.owed)
        assertEquals(100_00, result.credit)
    }

    @Test
    fun negativeInputsAreTreatedAsZero() {
        val result = calc(RateType.DAY, rate = -5, kitRental = -10, expenses = -10, paid = -10)
        assertEquals(0, result.total)
        assertEquals(0, result.owed)
    }

    @Test
    fun daysBetweenIsInclusive() {
        assertEquals(1, MoneyRules.daysBetween(LocalDate(2026, 1, 5), LocalDate(2026, 1, 5)))
        assertEquals(3, MoneyRules.daysBetween(LocalDate(2026, 1, 30), LocalDate(2026, 2, 1)))
        assertEquals(1, MoneyRules.daysBetween(LocalDate(2026, 1, 5), LocalDate(2026, 1, 1)))
    }

    @Test
    fun minutesPerDayGroupsByLocalClockInDateAndSkipsOpenLogs() {
        fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime(2026, 3, day, hour, minute).toInstant(dubai)
        val logs = listOf(
            TimeLog(jobId = 1, clockIn = at(10, 6), clockOut = at(10, 12)),
            TimeLog(jobId = 1, clockIn = at(10, 13), clockOut = at(10, 19, 30)),
            // Night shoot past midnight counts toward the day it started
            TimeLog(jobId = 1, clockIn = at(11, 20), clockOut = at(12, 2)),
            TimeLog(jobId = 1, clockIn = at(12, 8), clockOut = null),
        )
        val result = MoneyRules.minutesPerDay(logs, dubai)
        assertEquals(mapOf(LocalDate(2026, 3, 10) to 750L, LocalDate(2026, 3, 11) to 360L), result)
    }

    @Test
    fun breakdownCombinesJobLogsAndPayments() {
        val job = Job(
            id = 1,
            clientId = 1,
            title = "Car commercial",
            startDate = LocalDate(2026, 3, 10),
            endDate = LocalDate(2026, 3, 11),
            rateType = RateType.DAY,
            rate = 1_200_00,
            dayLengthHours = 10.0,
            otMultiplier = 1.5,
            kitRental = 300_00,
            expenses = 50_00,
        )
        val logs = listOf(
            TimeLog(
                jobId = 1,
                clockIn = LocalDateTime(2026, 3, 10, 6, 0).toInstant(dubai),
                clockOut = LocalDateTime(2026, 3, 10, 18, 0).toInstant(dubai),
            ),
        )
        val payments = listOf(
            Payment(jobId = 1, amount = 1_000_00, dateReceived = LocalDate(2026, 3, 20), method = PaymentMethod.BANK),
            Payment(jobId = 1, amount = 500_00, dateReceived = LocalDate(2026, 3, 25), method = PaymentMethod.CASH),
        )
        val result = MoneyRules.breakdown(job, logs, payments, dubai)

        assertEquals(2, result.days)
        assertEquals(2_400_00, result.base)
        // 2h OT at 120/h x 1.5
        assertEquals(360_00, result.overtime)
        assertEquals(3_110_00, result.total)
        assertEquals(1_500_00, result.paid)
        assertEquals(1_610_00, result.owed)
    }
}
