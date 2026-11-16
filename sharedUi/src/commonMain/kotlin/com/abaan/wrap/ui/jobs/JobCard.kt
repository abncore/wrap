package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.StatusChip
import com.abaan.wrap.ui.components.formatDateRange
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.formatMonthShort
import com.abaan.wrap.ui.components.money

@Composable
fun JobCard(
    summary: JobSummary,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val job = summary.job
    val overdue = summary.isOverdue
    val errorColor = MaterialTheme.colorScheme.error
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (overdue) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            DateBadge(day = job.startDate.day, month = job.startDate.formatMonthShort(), overdue = overdue)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(summary.client?.name, job.role.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = when {
                        overdue -> stringResource(Res.string.jobs_overdue_since, job.dueDate?.formatMedium().orEmpty())
                        summary.isUnpaid && job.dueDate != null -> stringResource(Res.string.jobs_due, job.dueDate!!.formatMedium())
                        else -> listOfNotNull(
                            formatDateRange(job.startDate, job.endDate),
                            job.location.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val amount = if (summary.isUnpaid) summary.money.owed else summary.money.total
                Text(
                    text = money(amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (overdue) errorColor else MaterialTheme.colorScheme.onSurface,
                )
                StatusChip(summary.status)
            }
        }
    }
}

@Composable
private fun DateBadge(day: Int, month: String, overdue: Boolean) {
    Surface(
        color = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(min = 52.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(text = month, style = MaterialTheme.typography.labelSmall)
            Text(text = day.toString(), style = MaterialTheme.typography.titleLarge)
        }
    }
}
