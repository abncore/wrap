package com.abaan.wrap.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abaan.wrap.MainActivity
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.JobSummary
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.jobs.isLink
import com.abaan.wrap.ui.jobs.mapsIntent

object ReminderNotifications {
    const val CHANNEL_ID = "job_reminders"
    const val CALL_CHANNEL_ID = "call_reminders"
    /** Call reminders use their own notification ids so they don't replace the night-before one. */
    private const val CALL_ID_OFFSET = 1_000_000
    const val CHASE_CHANNEL_ID = "chase_reminders"
    private const val CHASE_ID_OFFSET = 2_000_000
    const val EXTRA_JOB_ID = "com.abaan.wrap.extra.JOB_ID"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_desc) }
        val callChannel = NotificationChannel(
            CALL_CHANNEL_ID,
            context.getString(R.string.call_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.call_reminder_channel_desc) }
        val chaseChannel = NotificationChannel(
            CHASE_CHANNEL_ID,
            context.getString(R.string.chase_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.chase_channel_desc) }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(channel, callChannel, chaseChannel))
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, summary: JobSummary) {
        if (!canNotify(context)) return
        val job = summary.job
        val body = listOfNotNull(
            job.callTime?.let { context.getString(R.string.home_call_time, it.formatShort()) },
            job.location.takeIf { it.isNotBlank() },
            summary.client?.name,
        ).joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(context.getString(R.string.reminder_title, job.title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openJobIntent(context, job.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        post(context, job.id.toInt(), notification)
    }

    /** "Call at 6:30: Desert car commercial", with directions when the job has a location. */
    fun showCall(context: Context, summary: JobSummary) {
        if (!canNotify(context)) return
        val job = summary.job
        val callTime = job.callTime ?: return
        val body = listOfNotNull(
            job.location.takeIf { it.isNotBlank() && !isLink(it) },
            job.unitBase.takeIf { it.isNotBlank() && !isLink(it) }?.let { context.getString(R.string.call_sheet_unit_base) + ": " + it },
            summary.client?.name,
        ).joinToString(" · ")
        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(context.getString(R.string.call_reminder_title, callTime.formatShort(), job.title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openJobIntent(context, job.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        val place = job.location.ifBlank { job.unitBase }
        if (place.isNotBlank()) {
            val directions = PendingIntent.getActivity(
                context,
                CALL_ID_OFFSET + job.id.toInt(),
                mapsIntent(place),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_directions, context.getString(R.string.action_directions), directions)
        }
        post(context, CALL_ID_OFFSET + job.id.toInt(), builder.build())
    }

    /** "Time to chase Falcon Productions: AED 1,500 for Desert shoot is 12 days overdue." Opens the job. */
    fun showNudge(context: Context, summary: JobSummary, daysOverdue: Int, currency: String) {
        if (!canNotify(context)) return
        val job = summary.job
        val who = summary.client?.name ?: job.title
        val body = context.resources.getQuantityString(
            R.plurals.chase_nudge_body,
            daysOverdue,
            money(summary.money.owed, currency),
            job.title,
            daysOverdue,
        )
        val notification = NotificationCompat.Builder(context, CHASE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_payments)
            .setContentTitle(context.getString(R.string.chase_nudge_title, who))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openJobIntent(context, job.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        post(context, CHASE_ID_OFFSET + job.id.toInt(), notification)
    }

    private fun openJobIntent(context: Context, jobId: Long): PendingIntent {
        val openJob = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_JOB_ID, jobId)
        }
        return PendingIntent.getActivity(
            context,
            jobId.toInt(),
            openJob,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and posting; nothing to show.
        }
    }
}
