package com.abaan.wrap.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

private fun styled(date: NSDateFormatterStyle, time: NSDateFormatterStyle) = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
    dateStyle = date
    timeStyle = time
}

/** A formatter for the phone's locale from a skeleton like "EEEdMMM", ordered as the region prefers. */
private fun skeleton(template: String) = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
    setLocalizedDateFormatFromTemplate(template)
}

private val mediumDate = styled(NSDateFormatterMediumStyle, NSDateFormatterNoStyle)
private val shortTime = styled(NSDateFormatterNoStyle, NSDateFormatterShortStyle)
private val mediumDateTime = styled(NSDateFormatterMediumStyle, NSDateFormatterShortStyle)
private val weekdayDate = skeleton("EEEdMMM")
private val fullWeekdayDate = skeleton("EEEEdMMMM")
private val monthYear = skeleton("MMMMyyyy")
private val monthShort = skeleton("MMM")
private val monthName = skeleton("MMMM")

private fun LocalDate.toNSDate(): NSDate {
    val parts = NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.month.ordinal.toLong() + 1
        day = this@toNSDate.day.toLong()
        hour = 12
    }
    return NSCalendar.currentCalendar.dateFromComponents(parts)!!
}

private fun LocalTime.toNSDate(): NSDate {
    val parts = NSDateComponents().apply {
        year = 2000
        month = 1
        day = 1
        hour = this@toNSDate.hour.toLong()
        minute = this@toNSDate.minute.toLong()
    }
    return NSCalendar.currentCalendar.dateFromComponents(parts)!!
}

actual fun LocalDate.formatMedium(): String = mediumDate.stringFromDate(toNSDate())
actual fun LocalDate.formatWeekday(): String = weekdayDate.stringFromDate(toNSDate())
actual fun LocalDate.formatFullWeekday(): String = fullWeekdayDate.stringFromDate(toNSDate())
actual fun LocalDate.formatMonthShort(): String = monthShort.stringFromDate(toNSDate()).uppercase()
actual fun LocalDate.formatMonthName(): String = monthName.stringFromDate(toNSDate())
actual fun LocalDate.formatMonthYear(): String = monthYear.stringFromDate(toNSDate())
actual fun LocalTime.formatShort(): String = shortTime.stringFromDate(toNSDate())

actual fun formatDateTime(epochMillis: Long): String =
    mediumDateTime.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))

// NSCalendar numbers days from Sunday = 1.
actual fun firstDayOfWeek(): DayOfWeek {
    val sundayBased = NSCalendar.currentCalendar.firstWeekday.toInt()
    return DayOfWeek.entries[(sundayBased + 5) % 7]
}

actual fun DayOfWeek.narrowName(): String {
    val symbols = NSDateFormatter().apply { locale = NSLocale.currentLocale }.veryShortWeekdaySymbols
    return symbols[isoDayNumber % 7] as String
}
