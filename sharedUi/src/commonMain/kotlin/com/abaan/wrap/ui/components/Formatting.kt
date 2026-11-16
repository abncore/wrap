package com.abaan.wrap.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.shared.money.MoneyFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun money(minor: Long, currency: String): String = MoneyFormat.format(minor, currency)

fun Instant.formatTime(timeZone: TimeZone): String = toLocalDateTime(timeZone).time.formatShort()

@Composable
fun formatDateRange(start: LocalDate, end: LocalDate): String =
    if (start == end) {
        start.formatWeekday()
    } else {
        stringResource(Res.string.date_range, start.formatWeekday(), end.formatWeekday())
    }

@Composable
fun formatDuration(minutes: Long): String =
    stringResource(Res.string.duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toString().padStart(2, '0'))

/** Hours for display without a trailing ".0", e.g. 10 or 10.5. */
fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

fun JobStatus.labelRes(): StringResource = when (this) {
    JobStatus.UPCOMING -> Res.string.status_upcoming
    JobStatus.DONE_UNPAID -> Res.string.status_done_unpaid
    JobStatus.PART_PAID -> Res.string.status_part_paid
    JobStatus.PAID -> Res.string.status_paid
    JobStatus.OVERDUE -> Res.string.status_overdue
}

fun RateType.labelRes(): StringResource = when (this) {
    RateType.DAY -> Res.string.rate_day
    RateType.HALF_DAY -> Res.string.rate_half_day
    RateType.HOUR -> Res.string.rate_hour
    RateType.FIXED -> Res.string.rate_fixed
}

fun RateType.fieldLabelRes(): StringResource = when (this) {
    RateType.DAY -> Res.string.rate_label_day
    RateType.HALF_DAY -> Res.string.rate_label_half_day
    RateType.HOUR -> Res.string.rate_label_hour
    RateType.FIXED -> Res.string.rate_label_fixed
}

fun PaymentMethod.labelRes(): StringResource = when (this) {
    PaymentMethod.CASH -> Res.string.method_cash
    PaymentMethod.BANK -> Res.string.method_bank
    PaymentMethod.CHEQUE -> Res.string.method_cheque
}

fun SpendCategory.labelRes(): StringResource = when (this) {
    SpendCategory.FUEL -> Res.string.category_fuel
    SpendCategory.TRANSPORT -> Res.string.category_transport
    SpendCategory.FOOD -> Res.string.category_food
    SpendCategory.KIT -> Res.string.category_kit
    SpendCategory.PHONE -> Res.string.category_phone
    SpendCategory.SOFTWARE -> Res.string.category_software
    SpendCategory.PERMITS -> Res.string.category_permits
    SpendCategory.OTHER -> Res.string.category_other
}

fun InsightPeriod.labelRes(): StringResource = when (this) {
    InsightPeriod.THIS_MONTH -> Res.string.period_this_month
    InsightPeriod.THIS_YEAR -> Res.string.period_this_year
    InsightPeriod.ALL_TIME -> Res.string.period_all_time
}
