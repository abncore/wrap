package com.abaan.wrap.ui.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.ClockButton
import com.abaan.wrap.ui.components.DotLabel
import com.abaan.wrap.ui.components.PaidOwedCard
import com.abaan.wrap.ui.components.WrapMark
import com.abaan.wrap.ui.components.formatFullWeekday
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.components.rememberNow
import com.abaan.wrap.ui.jobs.JobCard
import com.abaan.wrap.ui.screens.EmptyState
import com.abaan.wrap.ui.theme.LocalStatusColors
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            WrapMark(Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.app_name).lowercase(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.nav_settings))
            }
        }
        Greeting(today, state.timeZone)

        if (!dashboard.hasAnyJobs) {
            EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(Res.string.home_empty_title),
                body = stringResource(Res.string.home_empty_body),
                actionLabel = stringResource(Res.string.home_empty_action),
                onAction = onAddJob,
                modifier = Modifier.height(420.dp),
            )
            return@Column
        }

        PaidOwedCard(
            paidLabel = stringResource(Res.string.home_earned_lifetime),
            paid = dashboard.earnedLifetime,
            owedLabel = stringResource(Res.string.home_owed_to_you),
            owed = dashboard.owedTotal,
            currency = dashboard.currency,
            onClick = onOpenUnpaid,
        ) {
            Text(
                text = if (dashboard.unpaidCount > 0) {
                    pluralStringResource(Res.plurals.home_owed_jobs, dashboard.unpaidCount, dashboard.unpaidCount)
                } else {
                    stringResource(Res.string.home_all_paid)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(Res.string.home_received_month),
                value = money(dashboard.receivedThisMonth, dashboard.currency),
                dot = LocalStatusColors.current.success,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(Res.string.home_overdue),
                value = if (dashboard.overdueCount > 0) {
                    money(dashboard.overdueTotal, dashboard.currency)
                } else {
                    pluralStringResource(Res.plurals.home_overdue_count, 0, 0)
                },
                detail = if (dashboard.overdueCount > 0) {
                    pluralStringResource(Res.plurals.home_overdue_count, dashboard.overdueCount, dashboard.overdueCount)
                } else {
                    null
                },
                alert = dashboard.overdueCount > 0,
                onClick = onOpenUnpaid,
                modifier = Modifier.weight(1f),
            )
        }
        val budget = dashboard.budget
        StatCard(
            label = stringResource(Res.string.home_spent_month),
            value = money(dashboard.spentThisMonth, dashboard.currency),
            detail = budget?.let {
                if (it.level == BudgetLevel.OVER) {
                    stringResource(Res.string.budget_over_by, money(-it.remaining, dashboard.currency))
                } else {
                    stringResource(Res.string.home_budget_left, money(it.remaining, dashboard.currency))
                }
            },
            alert = budget?.level == BudgetLevel.OVER,
            warning = budget?.level == BudgetLevel.NEAR,
            onClick = onOpenMoney,
            modifier = Modifier.fillMaxWidth(),
        )

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
                    Text(stringResource(Res.string.home_next_job), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                text = stringResource(Res.string.home_no_next_job),
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
            in 5..11 -> Res.string.home_greeting_morning
            in 12..16 -> Res.string.home_greeting_afternoon
            else -> Res.string.home_greeting_evening
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
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    dot: Color? = null,
    alert: Boolean = false,
    warning: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val statusColors = LocalStatusColors.current
    val accent = when {
        alert -> MaterialTheme.colorScheme.error
        warning -> statusColors.warning
        else -> dot ?: MaterialTheme.colorScheme.outline
    }
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DotLabel(label, accent)
            // Shrinks to fit so large amounts in a half-width tile stay whole.
            val amountStyle = MaterialTheme.typography.titleLarge
            BasicText(
                text = value,
                style = amountStyle.copy(color = if (alert || warning) accent else MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = amountStyle.fontSize),
            )
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, colors = colors, border = border, modifier = modifier) { content() }
    } else {
        Card(colors = colors, border = border, modifier = modifier) { content() }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(Res.drawable.ic_schedule),
                    contentDescription = null,
                    tint = LocalStatusColors.current.success,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.home_today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        job.callTime?.let { stringResource(Res.string.home_call_time, it.formatShort()) },
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
        days <= 0 -> stringResource(Res.string.home_in_days_zero)
        days == 1 -> stringResource(Res.string.home_in_days_one)
        else -> pluralStringResource(Res.plurals.home_in_days, days, days)
    }
}
