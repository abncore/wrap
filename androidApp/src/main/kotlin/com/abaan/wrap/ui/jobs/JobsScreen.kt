package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.R
import com.abaan.wrap.ui.components.formatMonthYear
import com.abaan.wrap.ui.screens.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    onOpenJob: (Long) -> Unit,
    viewModel: JobsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter = state.filter

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.jobs_title)) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = filter.tab.ordinal) {
                JobsTab.entries.forEach { tab ->
                    val label = stringResource(
                        when (tab) {
                            JobsTab.UPCOMING -> R.string.jobs_tab_upcoming
                            JobsTab.UNPAID -> R.string.jobs_tab_unpaid
                            JobsTab.PAID -> R.string.jobs_tab_paid
                        },
                    )
                    val count = state.counts[tab] ?: 0
                    Tab(
                        selected = filter.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(if (count > 0) stringResource(R.string.jobs_tab_count, label, count) else label) },
                    )
                }
            }

            if (state.hasAnyJobs) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    FilterMenu(
                        label = state.clients.firstOrNull { it.id == filter.clientId }?.name
                            ?: stringResource(R.string.jobs_filter_client),
                        selected = filter.clientId != null,
                        allLabel = stringResource(R.string.jobs_filter_all_clients),
                        options = state.clients.map { it.id to it.name },
                        onSelect = viewModel::filterClient,
                    )
                    FilterMenu(
                        label = filter.month?.formatMonthYear() ?: stringResource(R.string.jobs_filter_month),
                        selected = filter.month != null,
                        allLabel = stringResource(R.string.jobs_filter_all_months),
                        options = state.months.map { it to it.formatMonthYear() },
                        onSelect = viewModel::filterMonth,
                    )
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize())
                state.jobs.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.jobs_title),
                    body = stringResource(
                        when {
                            filter.clientId != null || filter.month != null -> R.string.jobs_empty_filtered
                            filter.tab == JobsTab.UPCOMING -> R.string.jobs_empty_upcoming
                            filter.tab == JobsTab.UNPAID -> R.string.jobs_empty_unpaid
                            else -> R.string.jobs_empty_paid
                        },
                    ),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.jobs, key = { it.job.id }) { summary ->
                        JobCard(summary, state.currency, onClick = { onOpenJob(summary.job.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterMenu(
    label: String,
    selected: Boolean,
    allLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
