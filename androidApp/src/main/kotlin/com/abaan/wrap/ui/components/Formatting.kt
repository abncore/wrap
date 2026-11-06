package com.abaan.wrap.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.shared.money.MoneyFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val shortTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val weekdayDate = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullWeekdayDate = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthShort = DateTimeFormatter.ofPattern("MMM")

fun money(minor: Long, currency: String): String = MoneyFormat.format(minor, currency)

fun LocalDate.formatMedium(): String = toJavaLocalDate().format(mediumDate)
fun LocalDate.formatWeekday(): String = toJavaLocalDate().format(weekdayDate)
fun LocalDate.formatFullWeekday(): String = toJavaLocalDate().format(fullWeekdayDate)
fun LocalDate.formatMonthShort(): String = toJavaLocalDate().format(monthShort).uppercase()
fun LocalTime.formatShort(): String = toJavaLocalTime().format(shortTime)

/** "March 2026" for the first day of a month. */
fun LocalDate.formatMonthYear(): String = toJavaLocalDate().format(monthYear)

fun Instant.formatTime(timeZone: TimeZone): String = toLocalDateTime(timeZone).time.formatShort()

@Composable
fun formatDateRange(start: LocalDate, end: LocalDate): String =
    if (start == end) {
        start.formatWeekday()
    } else {
        stringResource(R.string.date_range, start.formatWeekday(), end.formatWeekday())
    }

@Composable
fun formatDuration(minutes: Long): String =
    stringResource(R.string.duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())

/** Hours for display without a trailing ".0", e.g. 10 or 10.5. */
fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@StringRes
fun JobStatus.labelRes(): Int = when (this) {
    JobStatus.UPCOMING -> R.string.status_upcoming
    JobStatus.DONE_UNPAID -> R.string.status_done_unpaid
    JobStatus.PART_PAID -> R.string.status_part_paid
    JobStatus.PAID -> R.string.status_paid
    JobStatus.OVERDUE -> R.string.status_overdue
}

@StringRes
fun RateType.labelRes(): Int = when (this) {
    RateType.DAY -> R.string.rate_day
    RateType.HALF_DAY -> R.string.rate_half_day
    RateType.HOUR -> R.string.rate_hour
    RateType.FIXED -> R.string.rate_fixed
}

@StringRes
fun RateType.fieldLabelRes(): Int = when (this) {
    RateType.DAY -> R.string.rate_label_day
    RateType.HALF_DAY -> R.string.rate_label_half_day
    RateType.HOUR -> R.string.rate_label_hour
    RateType.FIXED -> R.string.rate_label_fixed
}

@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.method_cash
    PaymentMethod.BANK -> R.string.method_bank
    PaymentMethod.CHEQUE -> R.string.method_cheque
}

@StringRes
fun SpendCategory.labelRes(): Int = when (this) {
    SpendCategory.FUEL -> R.string.category_fuel
    SpendCategory.TRANSPORT -> R.string.category_transport
    SpendCategory.FOOD -> R.string.category_food
    SpendCategory.KIT -> R.string.category_kit
    SpendCategory.PHONE -> R.string.category_phone
    SpendCategory.SOFTWARE -> R.string.category_software
    SpendCategory.PERMITS -> R.string.category_permits
    SpendCategory.OTHER -> R.string.category_other
}

@StringRes
fun InsightPeriod.labelRes(): Int = when (this) {
    InsightPeriod.THIS_MONTH -> R.string.period_this_month
    InsightPeriod.THIS_YEAR -> R.string.period_this_year
    InsightPeriod.ALL_TIME -> R.string.period_all_time
}
