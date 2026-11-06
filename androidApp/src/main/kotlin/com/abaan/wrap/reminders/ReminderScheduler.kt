package com.abaan.wrap.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.abaan.wrap.shared.chase.CallReminderRules
import com.abaan.wrap.shared.chase.ReminderRules
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Keeps WorkManager jobs in step with the database and settings: one per upcoming job at 8 PM the
 * night before, and one per job day shortly before call time.
 */
class ReminderScheduler(
    private val context: Context,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val today: Today,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(ledger.observeJobSummaries(), settings.observeSettings()) { jobs, appSettings ->
                jobs to appSettings
            }.collectLatest { (jobs, appSettings) ->
                sync(jobs, appSettings.remindersEnabled)
                syncCallReminders(jobs, appSettings)
            }
        }
    }

    private fun sync(jobs: List<JobSummary>, enabled: Boolean) {
        if (!enabled) {
            workManager.cancelAllWorkByTag(TAG)
            return
        }
        val now = clock.now()
        jobs.forEach { summary ->
            val name = workName(summary.job.id)
            if (ReminderRules.shouldRemind(summary, now, today.timeZone)) {
                val delay = ReminderRules.reminderAt(summary.job.startDate, today.timeZone) - now
                val request = OneTimeWorkRequestBuilder<JobReminderWorker>()
                    .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(JobReminderWorker.KEY_JOB_ID to summary.job.id))
                    .addTag(TAG)
                    .build()
                workManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
            } else {
                workManager.cancelUniqueWork(name)
            }
        }
    }

    private fun syncCallReminders(jobs: List<JobSummary>, appSettings: AppSettings) {
        // Cheaper to rebuild than to diff: there are only ever a handful of upcoming call times.
        workManager.cancelAllWorkByTag(CALL_TAG)
        val now = clock.now()
        jobs.forEach { summary ->
            CallReminderRules.upcoming(summary, appSettings.callReminderMinutes, now, today.timeZone).forEach { reminder ->
                val epochDay = reminder.date.toEpochDays()
                val request = OneTimeWorkRequestBuilder<CallReminderWorker>()
                    .setInitialDelay((reminder.at - now).inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(CallReminderWorker.KEY_JOB_ID to summary.job.id, CallReminderWorker.KEY_DATE to epochDay.toLong()),
                    )
                    .addTag(CALL_TAG)
                    .build()
                workManager.enqueueUniqueWork("call-reminder-${summary.job.id}-$epochDay", ExistingWorkPolicy.REPLACE, request)
            }
        }
    }

    private fun workName(jobId: Long) = "job-reminder-$jobId"

    private companion object {
        const val TAG = "job-reminder"
        const val CALL_TAG = "call-reminder"
    }
}
