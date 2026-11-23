package com.abaan.wrap.ui.jobs

import com.abaan.wrap.ui.components.BackButton
import com.abaan.wrap.ui.components.WrapTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.shared.money.CalendarDay
import com.abaan.wrap.ui.components.formatFullWeekday
import com.abaan.wrap.ui.components.formatMonthYear
import com.abaan.wrap.ui.theme.LocalStatusColors
import kotlinx.datetime.DayOfWeek
import com.abaan.wrap.ui.components.narrowName
import org.koin.compose.viewmodel.koinViewModel

/** Month view of bookings: spot clashes (two jobs on a day) and free days at a glance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenJob: (Long) -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WrapTopBar(
                title = { Text(stringResource(Res.string.calendar_title)) },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.showMonth(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.calendar_previous))
                }
                Text(
                    state.month.formatMonthYear(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { viewModel.showMonth(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.calendar_next))
                }
            }
            Text(
                text = listOf(
                    pluralStringResource(Res.plurals.calendar_free_days, state.freeCount, state.freeCount),
                    if (state.clashCount > 0) pluralStringResource(Res.plurals.calendar_clashes, state.clashCount, state.clashCount) else null,
                ).filterNotNull().joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.clashCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            // Weekday header, starting on the locale's first day of the week.
            Row {
                (0 until 7).forEach { offset ->
                    val day = DayOfWeek.entries[(state.firstDayOfWeek.ordinal + offset) % 7]
                    Text(
                        day.narrowName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            state.days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            isToday = day.date == state.today,
                            isSelected = day.date == state.selected,
                            onClick = { viewModel.select(day.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Legend()

            Text(
                state.selected.formatFullWeekday(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
            if (state.selectedJobs.isEmpty()) {
                Text(
                    stringResource(Res.string.calendar_day_free),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
                )
            }
            state.selectedJobs.forEach { summary ->
                JobCard(summary = summary, currency = state.currency, onClick = { onOpenJob(summary.job.id) })
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val statusColors = LocalStatusColors.current
    val description = stringResource(
        if (day.clash) Res.string.calendar_day_desc_clash else Res.string.calendar_day_desc,
        day.date.formatFullWeekday(),
        day.jobs.size,
    )
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.clash -> MaterialTheme.colorScheme.errorContainer
        day.jobs.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !day.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        day.clash -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(
            day.date.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            day.jobs.take(3).forEach { summary ->
                val dot = when (summary.status) {
                    JobStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                    JobStatus.PAID -> statusColors.success
                    JobStatus.OVERDUE -> MaterialTheme.colorScheme.error
                    else -> statusColors.warning
                }
                Box(Modifier.size(5.dp).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else dot, CircleShape))
            }
        }
    }
}

@Composable
private fun Legend() {
    val statusColors = LocalStatusColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(Res.string.status_upcoming))
        LegendItem(statusColors.warning, stringResource(Res.string.calendar_legend_unpaid))
        LegendItem(statusColors.success, stringResource(Res.string.status_paid))
        LegendItem(MaterialTheme.colorScheme.errorContainer, stringResource(Res.string.calendar_legend_clash))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
    }
}
