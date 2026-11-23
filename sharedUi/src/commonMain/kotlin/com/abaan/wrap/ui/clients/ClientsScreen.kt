package com.abaan.wrap.ui.clients

import com.abaan.wrap.ui.components.WrapTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.model.ClientSummary
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.screens.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onAddClient: () -> Unit,
    onOpenClient: (Long) -> Unit,
    viewModel: ClientsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            WrapTopBar(
                title = { Text(stringResource(Res.string.clients_title)) },
                actions = {
                    IconButton(onClick = onAddClient) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.clients_add))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding))
            state.clients.isEmpty() -> EmptyState(
                icon = Icons.Filled.Person,
                title = stringResource(Res.string.clients_empty_title),
                body = stringResource(Res.string.clients_empty_body),
                actionLabel = stringResource(Res.string.clients_add),
                onAction = onAddClient,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(state.clients, key = { it.client.id }) { summary ->
                    ClientRow(summary, state.currency, onClick = { onOpenClient(summary.client.id) })
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun ClientRow(summary: ClientSummary, currency: String, onClick: () -> Unit) {
    val client = summary.client
    ListItem(
        leadingContent = { Initials(client.name) },
        headlineContent = { Text(client.name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            val jobs = pluralStringResource(Res.plurals.clients_job_count, summary.jobCount, summary.jobCount)
            Text(if (client.company.isBlank()) jobs else stringResource(Res.string.separator_dot, client.company, jobs))
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (summary.owed > 0) {
                    Text(
                        text = money(summary.owed, currency),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (summary.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    if (summary.overdue) {
                        Text(
                            text = stringResource(Res.string.status_overdue),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.clients_settled),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun Initials(name: String, modifier: Modifier = Modifier) {
    val initials = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
