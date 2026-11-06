package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.TimeLog
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.DateInput
import com.abaan.wrap.ui.components.TimeInput
import com.abaan.wrap.ui.components.formatDuration
import com.abaan.wrap.ui.components.formatTime
import com.abaan.wrap.ui.components.formatWeekday
import com.abaan.wrap.ui.components.localDateSaver
import com.abaan.wrap.ui.components.localTimeSaver
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimeLogsSection(
    logs: List<TimeLog>,
    defaultDate: LocalDate,
    defaultStart: LocalTime?,
    timeZone: TimeZone,
    onAdd: (LocalDate, LocalTime, LocalTime) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    DetailSection(
        title = stringResource(R.string.job_time_logs),
        action = {
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.job_add_time))
            }
        },
    ) {
        if (logs.isEmpty()) {
            Text(
                text = stringResource(R.string.job_no_time_logs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        logs.forEach { log -> TimeLogRow(log, timeZone, onDelete = { pendingDelete = log.id }) }
    }

    if (showAdd) {
        AddTimeDialog(
            initialDate = defaultDate,
            initialStart = defaultStart ?: LocalTime(8, 0),
            onDismiss = { showAdd = false },
            onAdd = { date, from, to ->
                onAdd(date, from, to)
                showAdd = false
            },
        )
    }
    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.job_delete_log_title),
            body = null,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDelete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TimeLogRow(log: TimeLog, timeZone: TimeZone, onDelete: () -> Unit) {
    val startDate = log.clockIn.toLocalDateTime(timeZone).date
    val clockOut = log.clockOut
    val range = when {
        clockOut == null -> stringResource(R.string.job_on_clock)
        clockOut.toLocalDateTime(timeZone).date != startDate ->
            stringResource(R.string.job_time_log_overnight, log.clockIn.formatTime(timeZone), clockOut.formatTime(timeZone))
        else -> stringResource(R.string.job_time_log_range, log.clockIn.formatTime(timeZone), clockOut.formatTime(timeZone))
    }
    ListItem(
        headlineContent = { Text(startDate.formatWeekday()) },
        supportingContent = { Text(range) },
        trailingContent = {
            Row {
                if (clockOut != null) {
                    Text(
                        text = formatDuration((clockOut - log.clockIn).inWholeMinutes),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun AddTimeDialog(
    initialDate: LocalDate,
    initialStart: LocalTime,
    onDismiss: () -> Unit,
    onAdd: (LocalDate, LocalTime, LocalTime) -> Unit,
) {
    var date by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(initialDate) }
    var from by rememberSaveable(stateSaver = localTimeSaver) { mutableStateOf(initialStart) }
    var to by rememberSaveable(stateSaver = localTimeSaver) {
        mutableStateOf(LocalTime((initialStart.hour + 10) % 24, initialStart.minute))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.job_add_time_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateInput(date = date, onDateChange = { date = it }, label = stringResource(R.string.job_add_time_date))
                TimeInput(
                    time = from,
                    onTimeChange = { it?.let { t -> from = t } },
                    label = stringResource(R.string.job_add_time_start),
                    clearable = false,
                )
                TimeInput(
                    time = to,
                    onTimeChange = { it?.let { t -> to = t } },
                    label = stringResource(R.string.job_add_time_end),
                    clearable = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(date, from, to) }, enabled = from != to) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
