package com.abaan.wrap.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.RecurringSpend
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.formatWeekday
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.theme.color

/** Monthly costs that add themselves, with a way to stop each one. */
@Composable
fun RecurringCard(
    recurring: List<RecurringSpend>,
    currency: String,
    onStop: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stopping by rememberSaveable { mutableStateOf<Long?>(null) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(Res.drawable.ic_repeat),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.recurring_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.recurring_total, money(recurring.sumOf { it.amount }, currency)),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            recurring.forEach { item ->
                val category = stringResource(item.category.labelRes())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(item.category.color(), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.note.ifBlank { category },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(Res.string.recurring_next, item.nextDate.formatWeekday()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(money(item.amount, currency), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { stopping = item.id }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.recurring_stop))
                    }
                }
            }
        }
    }

    stopping?.let { id ->
        ConfirmDialog(
            title = stringResource(Res.string.recurring_stop_title),
            body = stringResource(Res.string.recurring_stop_body),
            confirmLabel = stringResource(Res.string.recurring_stop),
            onConfirm = {
                stopping = null
                onStop(id)
            },
            onDismiss = { stopping = null },
        )
    }
}
