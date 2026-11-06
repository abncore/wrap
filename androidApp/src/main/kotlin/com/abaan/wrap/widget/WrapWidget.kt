package com.abaan.wrap.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.abaan.wrap.MainActivity
import com.abaan.wrap.R
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.TimeLogRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.Dashboard
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.formatTime
import com.abaan.wrap.ui.components.formatWeekday
import com.abaan.wrap.ui.components.money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen widget: what you're owed, today's (or the next) job with its call time, and a button
 * to clock in or out. The app redraws it whenever its data changes (see [WidgetUpdater]).
 */
class WrapWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val ledger: LedgerRepository by inject()
    private val timeLogs: TimeLogRepository by inject()
    private val today: Today by inject()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        runAsync { WrapWidget.render(context, ledger.observeDashboard().first(), today) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CLOCK) return
        runAsync {
            val dashboard = ledger.observeDashboard().first()
            val todayJob = dashboard.todayJob
            when {
                dashboard.openLog != null -> timeLogs.clockOut()
                todayJob != null -> timeLogs.clockIn(todayJob.job.id)
            }
            WrapWidget.render(context, ledger.observeDashboard().first(), today)
        }
    }

    /** Broadcast receivers must finish quickly; [goAsync] lets the database work finish first. */
    private fun runAsync(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CLOCK = "com.abaan.wrap.widget.CLOCK"
    }
}

object WrapWidget {

    fun hasWidgets(context: Context): Boolean = ids(context).isNotEmpty()

    fun render(context: Context, dashboard: Dashboard, today: Today) {
        val ids = ids(context)
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_wrap)
        views.setTextViewText(R.id.widget_owed, money(dashboard.owedTotal, dashboard.currency))

        val openLog = dashboard.openLog
        val clocked = openLog?.let { log -> dashboard.todayJob?.takeIf { it.job.id == log.jobId } }
        val todayJob = dashboard.todayJob
        val next = dashboard.nextJob
        val (line, button) = when {
            openLog != null -> context.getString(
                R.string.widget_on_clock,
                clocked?.job?.title.orEmpty(),
                openLog.clockIn.formatTime(today.timeZone),
            ) to context.getString(R.string.widget_clock_out)
            todayJob != null -> context.getString(
                R.string.widget_today,
                listOfNotNull(todayJob.job.title, todayJob.job.callTime?.let { context.getString(R.string.home_call_time, it.formatShort()) })
                    .joinToString(" · "),
            ) to context.getString(R.string.widget_clock_in)
            next != null -> context.getString(
                R.string.widget_next,
                listOfNotNull(next.job.title, next.job.startDate.formatWeekday(), next.job.callTime?.formatShort()).joinToString(" · "),
            ) to null
            else -> context.getString(R.string.widget_no_jobs) to null
        }
        views.setTextViewText(R.id.widget_job, line)
        if (button != null) {
            views.setViewVisibility(R.id.widget_clock, View.VISIBLE)
            views.setTextViewText(R.id.widget_clock, button)
            val clock = Intent(context, WrapWidgetProvider::class.java).setAction(WrapWidgetProvider.ACTION_CLOCK)
            views.setOnClickPendingIntent(
                R.id.widget_clock,
                PendingIntent.getBroadcast(context, 0, clock, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            views.setViewVisibility(R.id.widget_clock, View.GONE)
        }

        // Tapping the rest opens the job shown, or the app.
        val jobId = (clocked ?: todayJob ?: next)?.job?.id
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            jobId?.let { putExtra(ReminderNotifications.EXTRA_JOB_ID, it) }
        }
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(context, REQUEST_OPEN, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }

    private fun ids(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, WrapWidgetProvider::class.java))

    private const val REQUEST_OPEN = 3_000_000
}

/** Redraws the widget whenever anything on it changes, while the app process is alive. */
class WidgetUpdater(
    private val context: Context,
    private val ledger: LedgerRepository,
    private val today: Today,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            ledger.observeDashboard().collect { dashboard ->
                if (WrapWidget.hasWidgets(context)) WrapWidget.render(context, dashboard, today)
            }
        }
    }
}
