package com.abaan.wrap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.TimeLog
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** The current time, refreshed every [intervalMillis] while on screen. */
@Composable
fun rememberNow(intervalMillis: Long = 30_000): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(intervalMillis) {
        while (true) {
            now = Clock.System.now()
            delay(intervalMillis)
        }
    }
    return now
}

/**
 * Large clock in / clock out button. When [openLog] is set the freelancer is on the clock,
 * so it shows the start time and elapsed time and offers Clock out.
 */
@Composable
fun ClockButton(
    openLog: TimeLog?,
    timeZone: TimeZone,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (openLog != null) {
            val now = rememberNow()
            val elapsed = (now - openLog.clockIn).inWholeMinutes.coerceAtLeast(0)
            Text(
                text = stringResource(Res.string.home_on_clock_since, openLog.clockIn.formatTime(timeZone)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.home_elapsed, formatDuration(elapsed)),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClockOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_stop), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.home_clock_out), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onClockIn,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.home_clock_in), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
