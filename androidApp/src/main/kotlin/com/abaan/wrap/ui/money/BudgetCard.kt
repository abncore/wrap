package com.abaan.wrap.ui.money

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.BudgetStatus
import com.abaan.wrap.shared.money.MoneyFormat
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.theme.LocalStatusColors
import kotlin.math.roundToInt

/** This month's budget with a progress bar, or a prompt to set one. */
@Composable
fun BudgetCard(
    budget: BudgetStatus?,
    monthName: String,
    currency: String,
    onSetBudget: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val statusColors = LocalStatusColors.current
    val (container, content) = when (budget?.level) {
        BudgetLevel.OVER -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        BudgetLevel.NEAR -> statusColors.warningContainer to statusColors.onWarningContainer
        else -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (budget == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.budget_set_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.budget_set_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = { editing = true }) { Text(stringResource(R.string.budget_set_action)) }
            }
        } else {
            Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (budget.level != BudgetLevel.UNDER) {
                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(
                        text = stringResource(R.string.budget_title, monthName),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.budget_edit))
                    }
                }
                Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(money(budget.spent, currency), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = stringResource(R.string.budget_of, money(budget.budget, currency)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(start = 6.dp, bottom = 3.dp),
                        )
                        Text(
                            text = stringResource(R.string.money_category_share, (budget.share * 100).roundToInt()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    BudgetBar(
                        share = budget.share.toFloat(),
                        color = when (budget.level) {
                            BudgetLevel.OVER -> MaterialTheme.colorScheme.error
                            BudgetLevel.NEAR -> statusColors.warning
                            BudgetLevel.UNDER -> MaterialTheme.colorScheme.primary
                        },
                    )
                    Text(
                        text = if (budget.level == BudgetLevel.OVER) {
                            stringResource(R.string.budget_over_by, money(-budget.remaining, currency))
                        } else {
                            pluralStringResource(
                                R.plurals.budget_left,
                                budget.daysLeft,
                                money(budget.remaining, currency),
                                money(budget.dailyAllowance, currency),
                                budget.daysLeft,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (editing) {
        BudgetDialog(
            current = budget?.budget ?: 0,
            currency = currency,
            onDismiss = { editing = false },
            onSave = {
                onSetBudget(it)
                editing = false
            },
        )
    }
}

@Composable
private fun BudgetBar(share: Float, color: Color) {
    val animated by animateFloatAsState(targetValue = share.coerceIn(0f, 1f), label = "budget")
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Set, change or remove (saves 0) the monthly budget. */
@Composable
fun BudgetDialog(
    current: Long,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(if (current > 0) MoneyFormat.toInput(current) else "") }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.budget_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyInput(
                    value = value,
                    onValueChange = {
                        value = it
                        showError = false
                    },
                    label = stringResource(R.string.budget_dialog_field),
                    currency = currency,
                    error = stringResource(R.string.error_amount_positive).takeIf { showError },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = MoneyFormat.parse(value)
                if (parsed == null || parsed <= 0) showError = true else onSave(parsed)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (current > 0) {
                    TextButton(onClick = { onSave(0) }) { Text(stringResource(R.string.budget_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
