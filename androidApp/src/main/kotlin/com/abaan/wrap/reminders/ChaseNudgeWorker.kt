package com.abaan.wrap.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abaan.wrap.shared.chase.ChaseRules
import com.abaan.wrap.shared.data.ChaseRepository
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/** Runs each morning and reminds the freelancer to chase overdue payments nobody has chased lately. */
class ChaseNudgeWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val ledger: LedgerRepository by inject()
    private val settings: SettingsRepository by inject()
    private val chases: ChaseRepository by inject()
    private val today: Today by inject()

    override suspend fun doWork(): Result {
        val appSettings = settings.getSettings()
        if (!appSettings.chaseRemindersEnabled) return Result.success()
        chases.jobsToNudge(ledger.observeJobSummaries().first()).forEach { summary ->
            ReminderNotifications.showNudge(
                applicationContext,
                summary,
                daysOverdue = ChaseRules.daysOverdue(summary, today.date()),
                currency = appSettings.currency,
            )
            chases.recordNudge(summary.job.id)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "chase-nudge"
        private val RUN_AT = LocalTime(10, 0)

        /** Schedules the daily check, first run at the next 10 AM. Safe to call on every launch. */
        fun schedule(context: Context, today: Today, clock: Clock) {
            val now = clock.now()
            var next = LocalDateTime(today.date(), RUN_AT).toInstant(today.timeZone)
            if (next <= now) next = LocalDateTime(today.date().plus(DatePeriod(days = 1)), RUN_AT).toInstant(today.timeZone)
            val request = PeriodicWorkRequestBuilder<ChaseNudgeWorker>(1, TimeUnit.DAYS)
                .setInitialDelay((next - now).inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
