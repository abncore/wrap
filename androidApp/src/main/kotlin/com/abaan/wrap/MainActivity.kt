package com.abaan.wrap

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.abaan.wrap.reminders.BudgetNotifications
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.ui.WrapApp
import com.abaan.wrap.ui.theme.WrapTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// FragmentActivity (a ComponentActivity) because the system unlock prompt needs one.
class MainActivity : FragmentActivity() {
    private val settings: SettingsRepository by inject()

    /** Job to open, set when the app is launched from a reminder notification. */
    private val pendingJobId = mutableStateOf<Long?>(null)

    /** True when launched from a budget alert, which opens the Money tab. */
    private val pendingOpenMoney = mutableStateOf(false)

    /** Null until settings have loaded, so nothing private shows before we know whether to lock. */
    private var lockEnabled: Boolean? = null
    private val locked = mutableStateOf(false)
    private val ready = mutableStateOf(false)
    private var leftAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            pendingJobId.value = intent.jobId()
            pendingOpenMoney.value = intent.opensMoney()
        }
        // Rotating the screen doesn't lock an app that was already unlocked.
        val wasUnlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) == true
        lifecycleScope.launch {
            settings.observeSettings().collect { appSettings ->
                val first = lockEnabled == null
                lockEnabled = appSettings.appLockEnabled
                when {
                    first -> {
                        locked.value = appSettings.appLockEnabled && !wasUnlocked
                        ready.value = true
                    }
                    // Turning the lock on in Settings doesn't lock you out there and then.
                    !appSettings.appLockEnabled -> locked.value = false
                }
            }
        }
        setContent {
            WrapTheme {
                if (!ready.value) {
                    Surface(Modifier.fillMaxSize()) {}
                    return@WrapTheme
                }
                Box(Modifier.fillMaxSize()) {
                    WrapApp(
                        openJobId = pendingJobId.value.takeUnless { locked.value },
                        onJobOpened = { pendingJobId.value = null },
                        openMoney = pendingOpenMoney.value && !locked.value,
                        onMoneyOpened = { pendingOpenMoney.value = false },
                    )
                    if (locked.value) {
                        LockScreen(onUnlock = ::unlock)
                        LaunchedEffect(Unit) { unlock() }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled == true && leftAt > 0 && SystemClock.elapsedRealtime() - leftAt > AppLock.GRACE_PERIOD_MS) {
            locked.value = true
        }
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, !locked.value)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.jobId()?.let { pendingJobId.value = it }
        if (intent.opensMoney()) pendingOpenMoney.value = true
    }

    private fun unlock() {
        AppLock.prompt(this) { locked.value = false }
    }

    private fun Intent.opensMoney(): Boolean = getBooleanExtra(BudgetNotifications.EXTRA_OPEN_MONEY, false)

    private fun Intent.jobId(): Long? =
        getLongExtra(ReminderNotifications.EXTRA_JOB_ID, 0L).takeIf { it != 0L }

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
    }
}
