package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.R
import com.abaan.wrap.files.SharedFiles
import com.abaan.wrap.ui.settings.vatPercent
import com.abaan.wrap.shared.chase.ChaseRules
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.ui.components.ClockButton
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.StatusChip
import com.abaan.wrap.ui.components.formatDateRange
import com.abaan.wrap.ui.components.formatDecimal
import com.abaan.wrap.ui.components.formatDuration
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.screens.EmptyState
import com.abaan.wrap.ui.theme.LocalStatusColors
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    jobId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: JobDetailViewModel = koinViewModel(key = "job-$jobId") { parametersOf(jobId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    val invoice by viewModel.invoice.collectAsStateWithLifecycle()
    LaunchedEffect(invoice) {
        invoice?.let {
            SharedFiles.share(
                context = context,
                file = it.file,
                mimeType = "application/pdf",
                subject = context.getString(R.string.invoice_subject, it.number, it.jobTitle),
                chooserTitle = context.getString(R.string.invoice_share_title),
            )
            viewModel.invoiceShared()
        }
    }
    val needsProfile by viewModel.needsProfile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.job_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.summary != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val summary = state.summary
        when {
            state.loading || state.deleted -> Box(Modifier.fillMaxSize().padding(padding))
            summary == null -> EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(R.string.job_detail_title),
                body = stringResource(R.string.job_not_found),
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                JobHeader(summary)
                if (summary.canClock) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ClockButton(
                            openLog = summary.openLog,
                            timeZone = state.timeZone,
                            onClockIn = viewModel::clockIn,
                            onClockOut = viewModel::clockOut,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                val job = summary.job
                if (!summary.workDone || state.crew.isNotEmpty() || job.unitBase.isNotBlank() || job.parking.isNotBlank()) {
                    CallSheetSection(
                        job = job,
                        crew = state.crew,
                        onAddCrew = viewModel::addCrew,
                        onDeleteCrew = viewModel::deleteCrew,
                        onEditJob = onEdit,
                    )
                }
                MoneyCard(summary, state.currency)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(R.drawable.ic_payments), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.job_add_payment))
                    }
                    if (ChaseRules.canChase(summary)) {
                        OutlinedButton(
                            onClick = {
                                launchChase(context, summary, state.currency)
                                viewModel.recordChase()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.job_chase))
                        }
                    }
                }
                OutlinedButton(onClick = viewModel::makeInvoice, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_receipt), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        summary.job.invoiceNumber?.let { stringResource(R.string.job_invoice_again, it) }
                            ?: stringResource(R.string.job_invoice),
                    )
                }
                if (ChaseRules.canChase(summary)) {
                    val last = state.chases.firstOrNull()
                    Text(
                        text = if (last == null) {
                            stringResource(R.string.chase_never)
                        } else {
                            pluralStringResource(
                                R.plurals.chase_history,
                                state.chases.size,
                                state.chases.size,
                                last.at.toLocalDateTime(state.timeZone).date.formatMedium(),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
                if (!summary.workDoneAutomatically) {
                    OutlinedButton(
                        onClick = { viewModel.setWorkDone(!summary.workDone) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (summary.workDone) R.string.job_mark_not_done else R.string.job_mark_done))
                    }
                }
                TimeLogsSection(
                    logs = summary.timeLogs,
                    defaultDate = if (summary.isOnToday) state.today else summary.job.startDate,
                    defaultStart = summary.job.callTime,
                    timeZone = state.timeZone,
                    onAdd = viewModel::addTimeLog,
                    onDelete = viewModel::deleteTimeLog,
                )
                PaymentsSection(summary.payments, state.currency, onEdit = onEditPayment)
                if (summary.job.notes.isNotBlank()) {
                    DetailSection(stringResource(R.string.job_notes)) {
                        Text(summary.job.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (needsProfile) {
        AlertDialog(
            onDismissRequest = viewModel::profilePromptShown,
            title = { Text(stringResource(R.string.job_invoice_profile_title)) },
            text = { Text(stringResource(R.string.job_invoice_profile_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.profilePromptShown()
                    onOpenProfile()
                }) { Text(stringResource(R.string.job_invoice_profile_action)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::profilePromptShown) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.job_delete_title),
            body = stringResource(R.string.job_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun JobHeader(summary: JobSummary) {
    val job = summary.job
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            StatusChip(summary.status)
        }
        val who = listOfNotNull(summary.client?.name, job.role.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (who.isNotEmpty()) InfoLine(rememberVectorPainter(Icons.Filled.Person), who)
        val call = job.callTime?.let { stringResource(R.string.home_call_time, it.formatShort()) }
        InfoLine(
            rememberVectorPainter(Icons.Filled.DateRange),
            listOfNotNull(formatDateRange(job.startDate, job.endDate), call).joinToString(" · "),
        )
        if (job.location.isNotBlank()) {
            val context = LocalContext.current
            val description = stringResource(R.string.job_location_directions, job.location)
            InfoLine(
                icon = painterResource(R.drawable.ic_directions),
                text = if (isLink(job.location)) stringResource(R.string.call_sheet_map_link) else job.location,
                modifier = Modifier
                    .clickable { openMaps(context, job.location) }
                    .semantics { contentDescription = description },
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InfoLine(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun MoneyCard(summary: JobSummary, currency: String) {
    val job = summary.job
    val money = summary.money
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.job_money), style = MaterialTheme.typography.titleMedium)
            val baseLabel = when (job.rateType) {
                RateType.DAY -> stringResource(R.string.job_base_days, money.days, money(job.rate, currency))
                RateType.HALF_DAY -> stringResource(R.string.job_base_half_days, money.days)
                RateType.HOUR -> if (money.workedMinutes > 0) {
                    stringResource(R.string.job_base_hours, formatDuration(money.workedMinutes))
                } else {
                    stringResource(R.string.job_base_hours_estimated, formatDuration((money.days * job.dayLengthHours * 60).toLong()))
                }
                RateType.FIXED -> stringResource(R.string.job_base_fixed)
            }
            MoneyRow(baseLabel, money(money.base, currency))
            if (money.overtime > 0) {
                MoneyRow(
                    stringResource(R.string.job_overtime, formatDuration(money.overtimeMinutes), formatDecimal(job.otMultiplier)),
                    money(money.overtime, currency),
                )
            }
            if (money.kitRental > 0) MoneyRow(stringResource(R.string.job_kit_rental), money(money.kitRental, currency))
            if (money.expenses > 0) MoneyRow(stringResource(R.string.job_expenses), money(money.expenses, currency))
            if (money.billedSpends > 0) {
                MoneyRow(
                    stringResource(R.string.job_billed_spends, summary.billedSpends.size),
                    money(money.billedSpends, currency),
                )
            }
            HorizontalDivider()
            if (money.vat > 0) {
                MoneyRow(stringResource(R.string.job_subtotal), money(money.subtotal, currency))
                MoneyRow(stringResource(R.string.job_vat, vatPercent(job.vatRate)), money(money.vat, currency))
            }
            MoneyRow(stringResource(R.string.job_total), money(money.total, currency), bold = true)
            if (money.paid > 0) {
                MoneyRow(stringResource(R.string.job_paid), money(money.paid, currency), color = LocalStatusColors.current.success)
            }
            MoneyRow(
                stringResource(R.string.job_owed),
                money(money.owed, currency),
                bold = true,
                color = if (summary.isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
            if (money.credit > 0) MoneyRow(stringResource(R.string.job_credit), money(money.credit, currency))
            MoneyRow(
                stringResource(R.string.job_due_date),
                job.dueDate?.formatMedium() ?: stringResource(R.string.job_no_due_date),
                color = if (summary.isOverdue) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        }
    }
}

@Composable
private fun MoneyRow(label: String, value: String, bold: Boolean = false, color: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@Composable
fun DetailSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        content()
    }
}

/** The clock icon used for time-related rows and buttons. */
@Composable
fun clockPainter(): Painter = painterResource(R.drawable.ic_schedule)
