package com.abaan.wrap.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val shortTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val mediumDateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
private val weekdayDate = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullWeekdayDate = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthShort = DateTimeFormatter.ofPattern("MMM")
private val monthName = DateTimeFormatter.ofPattern("MMMM")

actual fun LocalDate.formatMedium(): String = toJavaLocalDate().format(mediumDate)
actual fun LocalDate.formatWeekday(): String = toJavaLocalDate().format(weekdayDate)
actual fun LocalDate.formatFullWeekday(): String = toJavaLocalDate().format(fullWeekdayDate)
actual fun LocalDate.formatMonthShort(): String = toJavaLocalDate().format(monthShort).uppercase()
actual fun LocalDate.formatMonthName(): String = toJavaLocalDate().format(monthName)
actual fun LocalDate.formatMonthYear(): String = toJavaLocalDate().format(monthYear)
actual fun LocalTime.formatShort(): String = toJavaLocalTime().format(shortTime)

actual fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(mediumDateTime)

actual fun firstDayOfWeek(): DayOfWeek = DayOfWeek.entries[WeekFields.of(Locale.getDefault()).firstDayOfWeek.ordinal]

actual fun DayOfWeek.narrowName(): String = toJavaDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault())
