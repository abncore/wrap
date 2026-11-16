package com.abaan.wrap.ui.components

import androidx.compose.runtime.saveable.Saver
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Saves a [LocalDate] across configuration changes as its ISO string. */
val localDateSaver = Saver<LocalDate, String>(save = { it.toString() }, restore = LocalDate::parse)

/** Saves a [LocalTime] across configuration changes as its ISO string. */
val localTimeSaver = Saver<LocalTime, String>(save = { it.toString() }, restore = LocalTime::parse)
