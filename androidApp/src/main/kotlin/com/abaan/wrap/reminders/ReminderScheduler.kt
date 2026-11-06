package com.abaan.wrap.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.abaan.wrap.shared.chase.ReminderRules
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.JobSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Keeps one WorkManager job per upcoming job, due at 8 PM the night before, in step with
 * the database and the reminders setting.
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
                jobs to appSettings.remindersEnabled
            }.collectLatest { (jobs, enabled) -> sync(jobs, enabled) }
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

    private fun workName(jobId: Long) = "job-reminder-$jobId"

    private companion object {
        const val TAG = "job-reminder"
    }
}
