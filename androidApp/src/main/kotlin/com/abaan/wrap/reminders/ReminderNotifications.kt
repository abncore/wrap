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

object ReminderNotifications {
    const val CHANNEL_ID = "job_reminders"
    const val EXTRA_JOB_ID = "com.abaan.wrap.extra.JOB_ID"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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

        val openJob = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_JOB_ID, job.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            job.id.toInt(),
            openJob,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(context.getString(R.string.reminder_title, job.title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(job.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and posting; nothing to show.
        }
    }
}
