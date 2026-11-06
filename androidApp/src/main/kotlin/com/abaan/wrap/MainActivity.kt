package com.abaan.wrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.ui.WrapApp
import com.abaan.wrap.ui.theme.WrapTheme

class MainActivity : ComponentActivity() {
    /** Job to open, set when the app is launched from a reminder notification. */
    private val pendingJobId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingJobId.value = intent.jobId()
        setContent {
            WrapTheme {
                WrapApp(
                    openJobId = pendingJobId.value,
                    onJobOpened = { pendingJobId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.jobId()?.let { pendingJobId.value = it }
    }

    private fun Intent.jobId(): Long? =
        getLongExtra(ReminderNotifications.EXTRA_JOB_ID, 0L).takeIf { it != 0L }
}
