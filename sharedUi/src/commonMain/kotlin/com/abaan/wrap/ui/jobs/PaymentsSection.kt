package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.Payment
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.theme.LocalStatusColors

@Composable
fun PaymentsSection(
    payments: List<Payment>,
    currency: String,
    onEdit: (Long) -> Unit,
) {
    DetailSection(title = stringResource(Res.string.job_payments)) {
        if (payments.isEmpty()) {
            Text(
                text = stringResource(Res.string.job_no_payments),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        payments.forEach { payment ->
            val method = stringResource(payment.method.labelRes())
            ListItem(
                headlineContent = { Text(payment.dateReceived.formatMedium()) },
                supportingContent = {
                    Text(if (payment.note.isBlank()) method else stringResource(Res.string.separator_dot, method, payment.note))
                },
                trailingContent = {
                    Text(
                        text = money(payment.amount, currency),
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalStatusColors.current.success,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onEdit(payment.id) },
            )
        }
    }
}
