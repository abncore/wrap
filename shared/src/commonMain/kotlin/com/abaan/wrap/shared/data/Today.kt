package com.abaan.wrap.shared.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The current local date, as a value and as a Flow that ticks over at midnight. */
class Today(
    private val clock: Clock,
    val timeZone: TimeZone,
) {
    fun date(): LocalDate = clock.todayIn(timeZone)

    fun observe(): Flow<LocalDate> = flow {
        while (true) {
            emit(date())
            delay(CHECK_INTERVAL_MS)
        }
    }.distinctUntilChanged()

    private companion object {
        const val CHECK_INTERVAL_MS = 60_000L
    }
}
