package com.abaan.wrap.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.Today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Posts the "tomorrow" reminder for one job, if it is still relevant when the work runs. */
class JobReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val ledger: LedgerRepository by inject()
    private val settings: SettingsRepository by inject()
    private val today: Today by inject()

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, 0)
        if (!settings.getSettings().remindersEnabled) return Result.success()
        val summary = ledger.observeJobSummary(jobId).first() ?: return Result.success()
        val tomorrow = today.date().plus(DatePeriod(days = 1))
        if (summary.workDone || summary.job.startDate != tomorrow) return Result.success()
        ReminderNotifications.show(applicationContext, summary)
        return Result.success()
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
    }
}
