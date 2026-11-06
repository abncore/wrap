package com.abaan.wrap.shared.chase

import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.money.Ledger
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChaseTest {
    private val tz = TimeZone.of("Asia/Dubai")

    @Test
    fun normalisesInternationalNumbers() {
        assertEquals("971501234567", PhoneNumbers.toWhatsAppDigits("+971 50 123 4567"))
        assertEquals("971501234567", PhoneNumbers.toWhatsAppDigits("00971501234567"))
        assertEquals("447700900123", PhoneNumbers.toWhatsAppDigits("+44 7700 900123"))
        assertEquals("971501234567", PhoneNumbers.toWhatsAppDigits("971-50-123-4567"))
    }

    @Test
    fun localNumbersGetTheDefaultCountryCode() {
        assertEquals("971501234567", PhoneNumbers.toWhatsAppDigits("050 123 4567"))
        assertEquals("971501234567", PhoneNumbers.toWhatsAppDigits("50 123 4567"))
        assertEquals("966501234567", PhoneNumbers.toWhatsAppDigits("0501234567", defaultCountryCode = "966"))
    }

    @Test
    fun rejectsNonNumbers() {
        assertNull(PhoneNumbers.toWhatsAppDigits(""))
        assertNull(PhoneNumbers.toWhatsAppDigits("ask producer"))
        assertNull(PhoneNumbers.toWhatsAppDigits("12345"))
    }

    private fun summary(start: LocalDate, status: JobStatus = JobStatus.UPCOMING, paid: Long = 0, today: LocalDate) =
        Ledger.summarize(
            job = Job(
                id = 1,
                clientId = 1,
                title = "Shoot",
                startDate = start,
                endDate = start,
                rateType = RateType.DAY,
                rate = 1_000_00,
                dayLengthHours = 10.0,
                otMultiplier = 1.5,
                status = status,
            ),
            client = null,
            timeLogs = emptyList(),
            payments = if (paid > 0) {
                listOf(Payment(jobId = 1, amount = paid, dateReceived = start, method = PaymentMethod.BANK))
            } else {
                emptyList()
            },
            today = today,
            timeZone = tz,
        )

    @Test
    fun chaseOnlyWhenDoneAndOwed() {
        val today = LocalDate(2026, 5, 15)
        assertFalse(ChaseRules.canChase(summary(LocalDate(2026, 5, 20), today = today)))
        assertTrue(ChaseRules.canChase(summary(LocalDate(2026, 5, 10), today = today)))
        assertTrue(ChaseRules.canChase(summary(LocalDate(2026, 5, 10), paid = 400_00, today = today)))
        assertFalse(ChaseRules.canChase(summary(LocalDate(2026, 5, 10), paid = 1_000_00, today = today)))
    }

    @Test
    fun reminderIsEightPmTheNightBefore() {
        assertEquals(
            LocalDateTime(2026, 5, 19, 20, 0).toInstant(tz),
            ReminderRules.reminderAt(LocalDate(2026, 5, 20), tz),
        )
        assertEquals(
            LocalDateTime(2026, 2, 28, 20, 0).toInstant(tz),
            ReminderRules.reminderAt(LocalDate(2026, 3, 1), tz),
        )
    }

    @Test
    fun remindOnlyForFutureUpcomingJobs() {
        val today = LocalDate(2026, 5, 15)
        val evening = LocalDateTime(2026, 5, 15, 19, 0).toInstant(tz)
        val lateNight = LocalDateTime(2026, 5, 15, 21, 0).toInstant(tz)
        val tomorrow = summary(LocalDate(2026, 5, 16), today = today)

        assertTrue(ReminderRules.shouldRemind(tomorrow, evening, tz))
        assertFalse(ReminderRules.shouldRemind(tomorrow, lateNight, tz))
        assertFalse(ReminderRules.shouldRemind(summary(LocalDate(2026, 5, 20), status = JobStatus.DONE_UNPAID, today = today), evening, tz))
    }
}
