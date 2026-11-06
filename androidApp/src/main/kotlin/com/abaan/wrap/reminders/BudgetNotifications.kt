package com.abaan.wrap.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.abaan.wrap.MainActivity
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.BudgetLevel
import com.abaan.wrap.shared.model.BudgetStatus
import com.abaan.wrap.ui.components.money
import kotlin.math.roundToInt

/** Posts "nearly at budget" and "over budget" notifications; tapping one opens the Money tab. */
class BudgetNotifications(private val context: Context) {

    fun show(level: BudgetLevel, status: BudgetStatus, currency: String) {
        if (!ReminderNotifications.canNotify(context)) return
        val (title, body) = when (level) {
            BudgetLevel.UNDER -> return
            BudgetLevel.NEAR -> context.getString(R.string.budget_alert_near_title, (status.share * 100).roundToInt()) to
                context.resources.getQuantityString(
                    R.plurals.budget_alert_near_body,
                    status.daysLeft,
                    money(status.remaining, currency),
                    money(status.dailyAllowance, currency),
                    status.daysLeft,
                )
            BudgetLevel.OVER -> context.getString(R.string.budget_alert_over_title) to
                context.getString(
                    R.string.budget_alert_over_body,
                    money(status.spent, currency),
                    money(status.budget, currency),
                    money(-status.remaining, currency),
                )
        }

        val openMoney = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MONEY, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openMoney,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_insights)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            // One budget notification at a time: "over" replaces "nearly there".
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and posting; nothing to show.
        }
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val EXTRA_OPEN_MONEY = "com.abaan.wrap.extra.OPEN_MONEY"
        private const val NOTIFICATION_ID = -1

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.budget_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.budget_channel_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
