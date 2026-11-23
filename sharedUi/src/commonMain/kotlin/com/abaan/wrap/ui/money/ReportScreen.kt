package com.abaan.wrap.ui.money

import com.abaan.wrap.ui.components.BackButton
import com.abaan.wrap.ui.components.WrapTopBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.abaan.wrap.ui.platform.rememberPhoneActions
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.money.YearReport
import com.abaan.wrap.ui.components.formatMonthShort
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.theme.color
import org.koin.compose.viewmodel.koinViewModel

/** A year at a glance for tax time, with a CSV export for an accountant. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val view by viewModel.state.collectAsStateWithLifecycle()
    val csv by viewModel.csv.collectAsStateWithLifecycle()
    val actions = rememberPhoneActions()

    LaunchedEffect(csv) {
        csv?.let {
            actions.shareFile(
                path = it.path,
                mimeType = "text/csv",
                subject = it.name.substringBeforeLast('.'),
                title = getString(Res.string.report_export),
            )
            viewModel.csvShared()
        }
    }

    Scaffold(
        topBar = {
            WrapTopBar(
                title = { Text(stringResource(Res.string.report_title)) },
                navigationIcon = {
                    BackButton(onClick = onBack)
                },
                actions = {
                    IconButton(onClick = viewModel::exportCsv, enabled = view != null) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.report_export))
                    }
                },
            )
        },
    ) { padding ->
        val current = view
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val report = current.report
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                current.years.forEach { year ->
                    FilterChip(
                        selected = year == report.year,
                        onClick = { viewModel.selectYear(year) },
                        label = { Text(year.toString()) },
                    )
                }
            }
            SummaryCard(report)
            ClientsCard(report)
            CategoriesCard(report)
            MonthsCard(report)
            Button(onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.report_export))
            }
            Text(
                text = stringResource(Res.string.report_export_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun SummaryCard(report: YearReport) {
    val currency = report.currency
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.report_received, report.year), style = MaterialTheme.typography.titleSmall)
            Text(money(report.received, currency), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Figure(stringResource(Res.string.report_spent), money(report.spent, currency), Modifier.weight(1f))
                Figure(stringResource(Res.string.report_net), money(report.net, currency), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Figure(stringResource(Res.string.report_invoiced), money(report.invoiced, currency), Modifier.weight(1f))
                Figure(stringResource(Res.string.report_owed), money(report.owed, currency), Modifier.weight(1f))
            }
            if (report.vatCharged > 0 || report.billedSpent > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Figure(stringResource(Res.string.report_vat), money(report.vatCharged, currency), Modifier.weight(1f))
                    Figure(stringResource(Res.string.report_billed), money(report.billedSpent, currency), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
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
private fun ClientsCard(report: YearReport) {
    SectionCard(stringResource(Res.string.report_by_client)) {
        if (report.byClient.isEmpty()) {
            Empty(stringResource(Res.string.report_no_income))
        }
        report.byClient.forEach { client ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        client.clientName.ifBlank { stringResource(Res.string.report_unknown_client) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        pluralStringResource(Res.plurals.report_jobs, client.jobCount, client.jobCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(money(client.received, report.currency), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CategoriesCard(report: YearReport) {
    SectionCard(stringResource(Res.string.report_by_category)) {
        if (report.byCategory.isEmpty()) {
            Empty(stringResource(Res.string.report_no_spending))
        }
        report.byCategory.forEach { total ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(total.category.color(), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(total.category.labelRes()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(money(total.amount, report.currency), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun MonthsCard(report: YearReport) {
    SectionCard(stringResource(Res.string.report_by_month)) {
        Row {
            Spacer(Modifier.width(48.dp))
            Header(stringResource(Res.string.money_legend_earned), Modifier.weight(1f))
            Header(stringResource(Res.string.money_legend_spent), Modifier.weight(1f))
            Header(stringResource(Res.string.money_net), Modifier.weight(1f))
        }
        HorizontalDivider()
        report.months.forEach { month ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(month.month.formatMonthShort(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
                Cell(money(month.earned, report.currency), Modifier.weight(1f))
                Cell(money(month.spent, report.currency), Modifier.weight(1f))
                Cell(
                    money(month.net, report.currency),
                    Modifier.weight(1f),
                    if (month.net < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

@Composable
private fun Cell(text: String, modifier: Modifier, color: Color = Color.Unspecified) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

@Composable
private fun Empty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
