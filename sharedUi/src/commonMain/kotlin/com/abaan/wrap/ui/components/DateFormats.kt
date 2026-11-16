package com.abaan.wrap.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// Dates and times in the phone's language and region.

/** "7 Nov 2026" */
expect fun LocalDate.formatMedium(): String

/** "Sat 7 Nov" */
expect fun LocalDate.formatWeekday(): String

/** "Saturday 7 November" */
expect fun LocalDate.formatFullWeekday(): String

/** "NOV" */
expect fun LocalDate.formatMonthShort(): String

/** "November" */
expect fun LocalDate.formatMonthName(): String

/** "March 2026" for the first day of a month. */
expect fun LocalDate.formatMonthYear(): String

/** "6:30 AM" or "06:30", as the phone prefers. */
expect fun LocalTime.formatShort(): String

/** "7 Nov 2026, 12:50" in the phone's time zone. */
expect fun formatDateTime(epochMillis: Long): String

/** The day weeks start on in the phone's region. */
expect fun firstDayOfWeek(): DayOfWeek

/** One-letter day name for calendar headers, e.g. "M". */
expect fun DayOfWeek.narrowName(): String
