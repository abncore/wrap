package com.abaan.wrap.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abaan.wrap.shared.model.JobStatus
import com.abaan.wrap.ui.theme.LocalStatusColors

@Composable
fun StatusChip(status: JobStatus, modifier: Modifier = Modifier) {
    val (container, content) = statusColors(status)
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun statusColors(status: JobStatus): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val extra = LocalStatusColors.current
    return when (status) {
        JobStatus.UPCOMING -> colors.secondaryContainer to colors.onSecondaryContainer
        JobStatus.DONE_UNPAID -> extra.warningContainer to extra.onWarningContainer
        JobStatus.PART_PAID -> colors.secondaryContainer to extra.warning
        JobStatus.PAID -> extra.successContainer to extra.onSuccessContainer
        JobStatus.OVERDUE -> colors.errorContainer to colors.onErrorContainer
    }
}
