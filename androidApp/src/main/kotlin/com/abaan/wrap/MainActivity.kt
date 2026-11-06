package com.abaan.wrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.abaan.wrap.reminders.BudgetNotifications
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.ui.WrapApp
import com.abaan.wrap.ui.theme.WrapTheme

class MainActivity : ComponentActivity() {
    /** Job to open, set when the app is launched from a reminder notification. */
    private val pendingJobId = mutableStateOf<Long?>(null)

    /** True when launched from a budget alert, which opens the Money tab. */
    private val pendingOpenMoney = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            pendingJobId.value = intent.jobId()
            pendingOpenMoney.value = intent.opensMoney()
        }
        setContent {
            WrapTheme {
                WrapApp(
                    openJobId = pendingJobId.value,
                    onJobOpened = { pendingJobId.value = null },
                    openMoney = pendingOpenMoney.value,
                    onMoneyOpened = { pendingOpenMoney.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.jobId()?.let { pendingJobId.value = it }
        if (intent.opensMoney()) pendingOpenMoney.value = true
    }

    private fun Intent.opensMoney(): Boolean = getBooleanExtra(BudgetNotifications.EXTRA_OPEN_MONEY, false)

    private fun Intent.jobId(): Long? =
        getLongExtra(ReminderNotifications.EXTRA_JOB_ID, 0L).takeIf { it != 0L }
}
