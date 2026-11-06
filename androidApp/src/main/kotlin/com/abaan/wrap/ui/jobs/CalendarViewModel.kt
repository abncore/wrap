package com.abaan.wrap.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.money.CalendarDay
import com.abaan.wrap.shared.money.CalendarRules
import com.abaan.wrap.shared.money.Insights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.time.temporal.WeekFields
import java.util.Locale

data class CalendarUiState(
    val loading: Boolean = true,
    val month: LocalDate,
    val today: LocalDate,
    val selected: LocalDate,
    val days: List<CalendarDay> = emptyList(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val selectedJobs: List<JobSummary> = emptyList(),
    /** Days this month with two or more bookings. */
    val clashCount: Int = 0,
    /** Days this month with nothing booked. */
    val freeCount: Int = 0,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
)

class CalendarViewModel(
    ledger: LedgerRepository,
    settings: SettingsRepository,
    today: Today,
) : ViewModel() {
    private val firstDayOfWeek = DayOfWeek.entries[WeekFields.of(Locale.getDefault()).firstDayOfWeek.ordinal]
    private val month = MutableStateFlow(Insights.firstOfMonth(today.date()))
    private val selected = MutableStateFlow(today.date())

    val state: StateFlow<CalendarUiState> = combine(
        ledger.observeJobSummaries(),
        settings.observeSettings(),
        month,
        selected,
        today.observe(),
    ) { jobs, appSettings, shownMonth, selectedDay, date ->
        val days = CalendarRules.monthGrid(shownMonth, firstDayOfWeek, jobs)
        val inMonth = days.filter { it.inMonth }
        CalendarUiState(
            loading = false,
            month = shownMonth,
            today = date,
            selected = selectedDay,
            days = days,
            firstDayOfWeek = firstDayOfWeek,
            selectedJobs = CalendarRules.jobsOn(selectedDay, jobs),
            clashCount = inMonth.count { it.clash },
            freeCount = inMonth.count { it.jobs.isEmpty() },
            currency = appSettings.currency,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month = month.value, today = today.date(), selected = today.date()),
    )

    fun showMonth(offset: Int) {
        month.value = month.value.plus(DatePeriod(months = offset))
    }

    fun select(date: LocalDate) {
        selected.value = date
        if (Insights.firstOfMonth(date) != month.value) month.value = Insights.firstOfMonth(date)
    }
}
