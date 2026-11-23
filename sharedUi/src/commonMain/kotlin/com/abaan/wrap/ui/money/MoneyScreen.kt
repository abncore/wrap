package com.abaan.wrap.ui.money

import com.abaan.wrap.ui.components.WrapTopBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.CategoryTotal
import com.abaan.wrap.shared.model.InsightPeriod
import com.abaan.wrap.shared.model.MoneyOverview
import com.abaan.wrap.shared.model.MonthTotals
import com.abaan.wrap.shared.model.Spend
import com.abaan.wrap.shared.money.Insights
import com.abaan.wrap.ui.components.formatMonthName
import com.abaan.wrap.ui.components.formatMonthShort
import com.abaan.wrap.ui.components.formatMonthYear
import com.abaan.wrap.ui.components.formatWeekday
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.PaidOwedCard
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.theme.LocalStatusColors
import com.abaan.wrap.ui.theme.color
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onAddSpend: () -> Unit,
    onOpenSpend: (Long) -> Unit,
    viewModel: MoneyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overview = state.overview

    Scaffold(
        topBar = {
            WrapTopBar(
                title = { Text(stringResource(Res.string.money_title)) },
                actions = {
                    IconButton(onClick = onAddSpend) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.money_add))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading || overview == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(key = "lifetime") { LifetimeCard(overview) }
            item(key = "budget") {
                BudgetCard(
                    budget = overview.budget,
                    monthName = overview.thisMonth.month.formatMonthName(),
                    currency = overview.currency,
                    onSetBudget = viewModel::setBudget,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "chart") { ChartCard(overview.months, overview.currency) }
            item(key = "categories") { CategoryCard(overview, onSelectPeriod = viewModel::setPeriod) }
            if (state.recurring.isNotEmpty()) {
                item(key = "recurring") {
                    RecurringCard(
                        recurring = state.recurring,
                        currency = overview.currency,
                        onStop = viewModel::stopRecurring,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            spendLog(overview, onAddSpend, onOpenSpend)
        }
    }
}

@Composable
private fun LifetimeCard(overview: MoneyOverview) {
    PaidOwedCard(
        paidLabel = stringResource(Res.string.money_earned_lifetime),
        paid = overview.earnedLifetime,
        owedLabel = stringResource(Res.string.home_owed_to_you),
        owed = overview.owedTotal,
        currency = overview.currency,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LifetimeFigure(
                label = stringResource(Res.string.money_spent_lifetime),
                value = money(overview.spentLifetime, overview.currency),
                modifier = Modifier.weight(1f),
            )
            LifetimeFigure(
                label = stringResource(Res.string.money_net_lifetime),
                value = money(overview.netLifetime, overview.currency),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LifetimeFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ChartCard(months: List<MonthTotals>, currency: String) {
    var selected by rememberSaveable(months.size) { mutableIntStateOf(months.lastIndex) }
    val earnedColor = LocalStatusColors.current.success
    val spentColor = MaterialTheme.colorScheme.secondary
    val max = months.maxOf { maxOf(it.earned, it.spent) }.coerceAtLeast(1)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.money_chart_title, Insights.CHART_MONTHS),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Legend(earnedColor, stringResource(Res.string.money_legend_earned))
                Spacer(Modifier.width(12.dp))
                Legend(spentColor, stringResource(Res.string.money_legend_spent))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().height(168.dp),
            ) {
                months.forEachIndexed { index, month ->
                    val description = stringResource(
                        Res.string.money_chart_month_desc,
                        month.month.formatMonthYear(),
                        money(month.earned, currency),
                        money(month.spent, currency),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                            )
                            .selectable(selected = index == selected, role = Role.Tab, onClick = { selected = index })
                            .semantics { contentDescription = description }
                            .padding(top = 8.dp, bottom = 6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Bar(month.earned.toFloat() / max, earnedColor)
                            Bar(month.spent.toFloat() / max, spentColor)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = month.month.formatMonthShort(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val month = months[selected.coerceIn(months.indices)]
            HorizontalDivider()
            Text(month.month.formatMonthYear(), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthFigure(stringResource(Res.string.money_legend_earned), money(month.earned, currency), Modifier.weight(1f))
                MonthFigure(stringResource(Res.string.money_legend_spent), money(month.spent, currency), Modifier.weight(1f))
                MonthFigure(
                    label = stringResource(Res.string.money_net),
                    value = money(month.net, currency),
                    modifier = Modifier.weight(1f),
                    color = if (month.net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "bar")
    Box(
        Modifier
            .width(12.dp)
            .heightIn(min = 2.dp)
            .fillMaxHeight(animated)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(color),
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MonthFigure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryCard(overview: MoneyOverview, onSelectPeriod: (InsightPeriod) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.money_categories),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (overview.periodSpent > 0) {
                    Text(money(overview.periodSpent, overview.currency), style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = overview.period == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(stringResource(period.labelRes())) },
                    )
                }
            }
            if (overview.categories.isEmpty()) {
                Text(
                    text = stringResource(Res.string.money_categories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                overview.categories.forEach { CategoryRow(it, overview.currency) }
            }
        }
    }
}

@Composable
private fun CategoryRow(total: CategoryTotal, currency: String) {
    val share by animateFloatAsState(targetValue = total.share.toFloat(), label = "share")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(total.category.color(), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(total.category.labelRes()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.money_category_share, (total.share * 100).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(money(total.amount, currency), style = MaterialTheme.typography.titleSmall)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(share)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(total.category.color()),
            )
        }
    }
}

private fun LazyListScope.spendLog(
    overview: MoneyOverview,
    onAddSpend: () -> Unit,
    onOpenSpend: (Long) -> Unit,
) {
    item(key = "log-title") {
        Text(
            text = stringResource(Res.string.money_log),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    if (overview.spends.isEmpty()) {
        item(key = "log-empty") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Text(stringResource(Res.string.money_log_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.money_log_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onAddSpend, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(Res.string.money_add))
                }
            }
        }
        return
    }
    overview.spends.groupBy { Insights.firstOfMonth(it.date) }.forEach { (month, spends) ->
        item(key = "month-$month") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            ) {
                Text(
                    text = month.formatMonthYear(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.money_month_total, money(spends.sumOf { it.amount }, overview.currency)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(spends, key = { "spend-${it.id}" }) { spend ->
            SpendRow(spend, overview.currency, onClick = { onOpenSpend(spend.id) })
        }
    }
}

@Composable
private fun SpendRow(spend: Spend, currency: String, onClick: () -> Unit) {
    val category = stringResource(spend.category.labelRes())
    ListItem(
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).background(spend.category.color(), CircleShape),
            ) {
                Text(
                    text = category.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
        },
        headlineContent = {
            Text(
                text = spend.note.ifBlank { category },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            val details = if (spend.note.isBlank()) {
                spend.date.formatWeekday()
            } else {
                stringResource(Res.string.separator_dot, category, spend.date.formatWeekday())
            }
            Text(
                if (spend.jobId == null) details else stringResource(Res.string.separator_dot, details, stringResource(Res.string.spend_billed)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (spend.receipt.isNotBlank()) {
                    Icon(
                        painterResource(Res.drawable.ic_receipt),
                        contentDescription = stringResource(Res.string.receipt_attached),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (spend.recurringId != null) {
                    Icon(
                        painterResource(Res.drawable.ic_repeat),
                        contentDescription = stringResource(Res.string.spend_repeating),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(money(-spend.amount, currency), style = MaterialTheme.typography.titleSmall)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
