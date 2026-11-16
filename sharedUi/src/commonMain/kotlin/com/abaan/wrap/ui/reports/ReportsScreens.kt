package com.abaan.wrap.ui.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.money.AgeBucket
import com.abaan.wrap.shared.money.ClientPerformance
import com.abaan.wrap.shared.money.WorkReport
import com.abaan.wrap.ui.components.formatDuration
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.formatMonthShort
import com.abaan.wrap.ui.components.formatMonthYear
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.jobs.JobCard
import com.abaan.wrap.ui.theme.LocalStatusColors
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

/** The Reports tab: a list of the reports. */
@Composable
fun ReportsScreen(
    onOpenYear: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenAging: () -> Unit,
    onOpenWork: () -> Unit,
) {
    ReportScaffold(stringResource(Res.string.reports_title), onBack = null) {
        ReportLink(painterResource(Res.drawable.ic_insights), Res.string.reports_year_title, Res.string.reports_year_desc, onOpenYear)
        ReportLink(rememberVectorPainter(Icons.Filled.Person), Res.string.reports_clients_title, Res.string.reports_clients_desc, onOpenClients)
        ReportLink(painterResource(Res.drawable.ic_payments), Res.string.reports_aging_title, Res.string.reports_aging_desc, onOpenAging)
        ReportLink(painterResource(Res.drawable.ic_schedule), Res.string.reports_work_title, Res.string.reports_work_desc, onOpenWork)
    }
}

@Composable
private fun ReportLink(icon: Painter, title: StringResource, description: StringResource, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(stringResource(title), fontWeight = FontWeight.Medium) },
            supportingContent = { Text(stringResource(description)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

/** Every client with finished work, biggest earners first. */
@Composable
fun ClientPerformanceScreen(
    onBack: () -> Unit,
    onOpenClient: (Long) -> Unit,
    viewModel: ClientPerformanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportScaffold(stringResource(Res.string.reports_clients_title), onBack) {
        PeriodChips(state.period, viewModel::setPeriod)
        val clients = state.data ?: return@ReportScaffold
        if (clients.isEmpty()) Empty(stringResource(Res.string.reports_empty))
        clients.forEach { performance -> ClientRow(performance, state.currency) { onOpenClient(performance.client.id) } }
    }
}

@Composable
private fun ClientRow(performance: ClientPerformance, currency: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    performance.client.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(money(performance.received, currency), style = MaterialTheme.typography.titleMedium)
            }
            ShareBar(performance.shareOfIncome.toFloat(), MaterialTheme.colorScheme.primary)
            Text(
                listOfNotNull(
                    stringResource(Res.string.report_share, (performance.shareOfIncome * 100).roundToInt()),
                    pluralStringResource(Res.plurals.report_jobs, performance.jobs, performance.jobs),
                    performance.averageDaysToPay?.let { daysToPay(it) },
                    performance.owed.takeIf { it > 0 }?.let { stringResource(Res.string.client_report_owes, money(it, currency)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (performance.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One client in detail: money, how they pay, rates and every job. */
@Composable
fun ClientReportScreen(
    clientId: Long,
    onBack: () -> Unit,
    onOpenJob: (Long) -> Unit,
    viewModel: ClientReportViewModel = koinViewModel(key = "client-report-$clientId") { parametersOf(clientId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val performance = state.data
    ReportScaffold(performance?.client?.name ?: stringResource(Res.string.reports_clients_title), onBack) {
        PeriodChips(state.period, viewModel::setPeriod)
        if (performance == null || performance.jobs == 0) {
            Empty(stringResource(Res.string.reports_empty))
            return@ReportScaffold
        }
        val currency = state.currency
        HeroCard(stringResource(Res.string.client_report_received), money(performance.received, currency)) {
            Tile(stringResource(Res.string.client_report_billed), money(performance.billed, currency), Modifier.weight(1f))
            Tile(stringResource(Res.string.client_report_owed), money(performance.owed, currency), Modifier.weight(1f))
        }
        if (performance.overdue > 0) {
            Text(
                stringResource(Res.string.client_report_overdue, money(performance.overdue, currency)),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        SectionCard(stringResource(Res.string.client_report_paying)) {
            Figure(
                stringResource(Res.string.client_report_pays_label),
                performance.averageDaysToPay?.let { daysToPay(it) } ?: stringResource(Res.string.client_report_not_paid_yet),
            )
            Figure(
                stringResource(Res.string.client_report_on_time),
                performance.onTimeShare?.let { stringResource(Res.string.report_percent, (it * 100).roundToInt()) }
                    ?: stringResource(Res.string.client_report_not_paid_yet),
                valueColor = performance.onTimeShare?.let { if (it >= 0.75) LocalStatusColors.current.success else MaterialTheme.colorScheme.error }
                    ?: Color.Unspecified,
            )
            Figure(stringResource(Res.string.report_share_label), stringResource(Res.string.report_percent, (performance.shareOfIncome * 100).roundToInt()))
        }
        SectionCard(stringResource(Res.string.client_report_work)) {
            Figure(stringResource(Res.string.work_jobs), performance.jobs.toString())
            Figure(stringResource(Res.string.work_days), performance.daysWorked.toString())
            performance.averageDayRate?.let { Figure(stringResource(Res.string.work_average_day_rate), money(it, currency)) }
            performance.earningsPerDay?.let { Figure(stringResource(Res.string.work_per_day), money(it, currency)) }
            if (performance.firstJob != null && performance.lastJob != null) {
                Text(
                    stringResource(Res.string.client_report_since, performance.firstJob!!.formatMedium(), performance.lastJob!!.formatMedium()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(stringResource(Res.string.client_report_jobs), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        performance.jobSummaries.forEach { summary ->
            JobCard(summary = summary, currency = currency, onClick = { onOpenJob(summary.job.id) })
        }
    }
}

/** Who owes you, and how late they are. */
@Composable
fun AgingScreen(
    onBack: () -> Unit,
    onOpenJob: (Long) -> Unit,
    onOpenClient: (Long) -> Unit,
    viewModel: AgingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportScaffold(stringResource(Res.string.reports_aging_title), onBack) {
        val report = state.data ?: return@ReportScaffold
        val currency = state.currency
        if (report.total == 0L) {
            Empty(stringResource(Res.string.aging_empty))
            return@ReportScaffold
        }
        HeroCard(stringResource(Res.string.aging_total), money(report.total, currency)) {}
        SectionCard(stringResource(Res.string.aging_by_age)) {
            val max = report.byBucket.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            AgeBucket.entries.forEach { bucket ->
                val amount = report.byBucket[bucket] ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(stringResource(bucket.labelRes()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(money(amount, currency), style = MaterialTheme.typography.titleSmall)
                    }
                    ShareBar(amount.toFloat() / max, bucket.color())
                }
            }
        }
        SectionCard(stringResource(Res.string.aging_by_client)) {
            report.clients.forEach { owed ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = owed.client != null) { owed.client?.let { onOpenClient(it.id) } }
                        .padding(vertical = 4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(owed.clientName.ifBlank { stringResource(Res.string.report_unknown_client) }, fontWeight = FontWeight.Medium)
                        Text(
                            if (owed.overdue > 0) {
                                pluralStringResource(Res.plurals.aging_client_overdue, owed.oldestDays, money(owed.overdue, currency), owed.oldestDays)
                            } else {
                                stringResource(Res.string.aging_not_due)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (owed.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(money(owed.total, currency), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Text(stringResource(Res.string.client_report_jobs), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        report.jobs.forEach { owed -> JobCard(summary = owed.summary, currency = currency, onClick = { onOpenJob(owed.summary.job.id) }) }
    }
}

/** Days worked, overtime, rates and roles. */
@Composable
fun WorkReportScreen(
    onBack: () -> Unit,
    viewModel: WorkReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportScaffold(stringResource(Res.string.reports_work_title), onBack) {
        PeriodChips(state.period, viewModel::setPeriod)
        val work = state.data ?: return@ReportScaffold
        val currency = state.currency
        if (work.jobs == 0) Empty(stringResource(Res.string.reports_empty)) else WorkFigures(work, currency)
        DaysChart(work)
        if (work.byRole.isNotEmpty()) {
            SectionCard(stringResource(Res.string.work_by_role)) {
                work.byRole.forEach { role ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(role.role.ifBlank { stringResource(Res.string.work_no_role) }, fontWeight = FontWeight.Medium)
                            Text(
                                listOf(
                                    pluralStringResource(Res.plurals.report_jobs, role.jobs, role.jobs),
                                    pluralStringResource(Res.plurals.work_days_count, role.days, role.days),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(money(role.billed, currency), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkFigures(work: WorkReport, currency: String) {
    HeroCard(stringResource(Res.string.work_billed), money(work.billed, currency)) {
        Tile(stringResource(Res.string.work_jobs), work.jobs.toString(), Modifier.weight(1f))
        Tile(stringResource(Res.string.work_days), work.daysWorked.toString(), Modifier.weight(1f))
    }
    SectionCard(stringResource(Res.string.work_rates)) {
        work.averageDayRate?.let { Figure(stringResource(Res.string.work_average_day_rate), money(it, currency)) }
        work.earningsPerDay?.let { Figure(stringResource(Res.string.work_per_day), money(it, currency)) }
        work.averagePerJob?.let { Figure(stringResource(Res.string.work_per_job), money(it, currency)) }
        if (work.minutesLogged > 0) Figure(stringResource(Res.string.work_hours_logged), formatDuration(work.minutesLogged))
        if (work.overtimeMinutes > 0) {
            Figure(
                stringResource(Res.string.work_overtime),
                stringResource(Res.string.work_overtime_value, formatDuration(work.overtimeMinutes), money(work.overtimePay, currency)),
            )
        }
        work.busiestMonth?.let {
            Figure(stringResource(Res.string.work_busiest), pluralStringResource(Res.plurals.work_busiest_value, it.days, it.month.formatMonthYear(), it.days))
        }
    }
}

@Composable
private fun DaysChart(work: WorkReport) {
    val max = work.months.maxOf { it.days }.coerceAtLeast(1)
    SectionCard(stringResource(Res.string.work_chart_title)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().height(140.dp),
        ) {
            work.months.forEach { month ->
                val description = pluralStringResource(Res.plurals.work_busiest_value, month.days, month.month.formatMonthYear(), month.days)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description },
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        val fraction by animateFloatAsState(month.days.toFloat() / max, label = "days")
                        Box(
                            Modifier
                                .width(14.dp)
                                .heightIn(min = 2.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(month.month.formatMonthShort().take(1), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// Building blocks shared by the report screens.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportScaffold(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    // Sub-reports have a back arrow; the Reports tab itself doesn't.
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun PeriodChips(selected: InsightPeriod, onSelect: (InsightPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InsightPeriod.entries.forEach { period ->
            FilterChip(selected = period == selected, onClick = { onSelect(period) }, label = { Text(stringResource(period.labelRes())) })
        }
    }
}

@Composable
private fun HeroCard(label: String, value: String, tiles: @Composable RowScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp), content = tiles)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Figure(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

@Composable
private fun ShareBar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "share")
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
private fun Empty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun daysToPay(days: Int): String =
    if (days == 0) stringResource(Res.string.client_report_pays_same_day) else pluralStringResource(Res.plurals.client_report_pays_in, days, days)

private fun AgeBucket.labelRes(): StringResource = when (this) {
    AgeBucket.NOT_DUE -> Res.string.aging_bucket_not_due
    AgeBucket.DAYS_1_30 -> Res.string.aging_bucket_30
    AgeBucket.DAYS_31_60 -> Res.string.aging_bucket_60
    AgeBucket.DAYS_61_90 -> Res.string.aging_bucket_90
    AgeBucket.OVER_90 -> Res.string.aging_bucket_over_90
}

@Composable
private fun AgeBucket.color(): Color = when (this) {
    AgeBucket.NOT_DUE -> MaterialTheme.colorScheme.primary
    AgeBucket.DAYS_1_30 -> LocalStatusColors.current.warning
    else -> MaterialTheme.colorScheme.error
}
