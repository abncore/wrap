package com.abaan.wrap.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.ClockButton
import com.abaan.wrap.ui.components.formatFullWeekday
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.components.rememberNow
import com.abaan.wrap.ui.jobs.JobCard
import com.abaan.wrap.ui.screens.EmptyState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onAddJob: () -> Unit,
    onOpenJob: (Long) -> Unit,
    onOpenUnpaid: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dashboard = state.dashboard
    val today = state.today

    if (state.loading || dashboard == null || today == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Greeting(today, state.timeZone, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
            }
        }

        if (!dashboard.hasAnyJobs) {
            EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(R.string.home_empty_title),
                body = stringResource(R.string.home_empty_body),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onAddJob,
                modifier = Modifier.height(420.dp),
            )
            return@Column
        }

        OwedCard(dashboard)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.home_received_month),
                value = money(dashboard.receivedThisMonth, dashboard.currency),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.home_overdue),
                value = if (dashboard.overdueCount > 0) {
                    money(dashboard.overdueTotal, dashboard.currency)
                } else {
                    pluralStringResource(R.plurals.home_overdue_count, 0, 0)
                },
                detail = if (dashboard.overdueCount > 0) {
                    pluralStringResource(R.plurals.home_overdue_count, dashboard.overdueCount, dashboard.overdueCount)
                } else {
                    null
                },
                alert = dashboard.overdueCount > 0,
                onClick = onOpenUnpaid,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.home_earned_lifetime),
                value = money(dashboard.earnedLifetime, dashboard.currency),
                onClick = onOpenMoney,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.home_spent_month),
                value = money(dashboard.spentThisMonth, dashboard.currency),
                onClick = onOpenMoney,
                modifier = Modifier.weight(1f),
            )
        }

        val todayJob = dashboard.todayJob
        val openLog = dashboard.openLog
        if (todayJob != null || openLog != null) {
            val clockJob = openLog?.let { log ->
                if (todayJob?.job?.id == log.jobId) todayJob else null
            } ?: todayJob
            TodayCard(
                summary = clockJob,
                dashboard = dashboard,
                timeZone = state.timeZone,
                onOpenJob = onOpenJob,
                onClockIn = { clockJob?.let { viewModel.clockIn(it.job.id) } },
                onClockOut = viewModel::clockOut,
            )
        }

        val next = dashboard.nextJob
        if (next != null && next.job.id != todayJob?.job?.id) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.home_next_job), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = daysUntilLabel(today, next.job.startDate),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                JobCard(next, dashboard.currency, onClick = { onOpenJob(next.job.id) })
            }
        } else if (next == null && todayJob == null) {
            Text(
                text = stringResource(R.string.home_no_next_job),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Greeting(today: LocalDate, timeZone: TimeZone, modifier: Modifier = Modifier) {
    val hour = rememberNow(60_000).toLocalDateTime(timeZone).hour
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    Column(modifier) {
        Text(greeting, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = today.formatFullWeekday(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OwedCard(dashboard: Dashboard) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_owed_to_you), style = MaterialTheme.typography.titleSmall)
            Text(money(dashboard.owedTotal, dashboard.currency), style = MaterialTheme.typography.displaySmall)
            Text(
                text = if (dashboard.unpaidCount > 0) {
                    pluralStringResource(R.plurals.home_owed_jobs, dashboard.unpaidCount, dashboard.unpaidCount)
                } else {
                    stringResource(R.string.home_all_paid)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    alert: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (alert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
    )
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alert) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, colors = colors, modifier = modifier) { content() }
    } else {
        Card(colors = colors, modifier = modifier) { content() }
    }
}

@Composable
private fun TodayCard(
    summary: JobSummary?,
    dashboard: Dashboard,
    timeZone: TimeZone,
    onOpenJob: (Long) -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_schedule), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.home_today), style = MaterialTheme.typography.labelLarge)
            }
            if (summary != null) {
                val job = summary.job
                Card(
                    onClick = { onOpenJob(job.id) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Text(job.title, style = MaterialTheme.typography.titleLarge)
                    val who = listOfNotNull(summary.client?.name, job.role.takeIf { it.isNotBlank() }).joinToString(" · ")
                    if (who.isNotEmpty()) Text(who, style = MaterialTheme.typography.bodyMedium)
                    val details = listOfNotNull(
                        job.callTime?.let { stringResource(R.string.home_call_time, it.formatShort()) },
                        job.location.takeIf { it.isNotBlank() },
                    )
                    if (details.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            ClockButton(
                openLog = dashboard.openLog,
                timeZone = timeZone,
                onClockIn = onClockIn,
                onClockOut = onClockOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun daysUntilLabel(today: LocalDate, date: LocalDate): String {
    val days = today.daysUntil(date)
    return when {
        days <= 0 -> stringResource(R.string.home_in_days_zero)
        days == 1 -> stringResource(R.string.home_in_days_one)
        else -> pluralStringResource(R.plurals.home_in_days, days, days)
    }
}
