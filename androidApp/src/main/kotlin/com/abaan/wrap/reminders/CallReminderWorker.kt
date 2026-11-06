package com.abaan.wrap.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Posts the "call time soon" reminder for one job day, if it is still relevant when the work runs. */
class CallReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val ledger: LedgerRepository by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, 0)
        val date = LocalDate.fromEpochDays(inputData.getLong(KEY_DATE, 0).toInt())
        val minutesBefore = settings.getSettings().callReminderMinutes
        if (minutesBefore <= 0) return Result.success()
        val summary = ledger.observeJobSummary(jobId).first() ?: return Result.success()
        val stillDue = !summary.workDone && summary.openLog == null && summary.job.callTime != null &&
            date in summary.job.startDate..summary.job.endDate
        if (stillDue) ReminderNotifications.showCall(applicationContext, summary)
        return Result.success()
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_DATE = "date"
    }
}
