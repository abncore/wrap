package com.abaan.wrap

import android.app.Application
import com.abaan.wrap.di.appModule
import com.abaan.wrap.reminders.BudgetNotifications
import com.abaan.wrap.reminders.CalendarSync
import com.abaan.wrap.widget.WidgetUpdater
import com.abaan.wrap.reminders.ChaseNudgeWorker
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.reminders.ReminderScheduler
import com.abaan.wrap.shared.data.RecurringSpendRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.di.initKoin
import com.abaan.wrap.ui.di.uiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class WrapApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@WrapApplication)
            modules(uiModule, appModule)
        }
        ReminderNotifications.createChannel(this)
        BudgetNotifications.createChannel(this)
        get<ReminderScheduler>().start(appScope)
        get<CalendarSync>().start(appScope)
        get<WidgetUpdater>().start(appScope)
        addMonthlyCosts()
        ChaseNudgeWorker.schedule(this, get(), get())
    }

    /** Adds monthly costs as they come due: on launch and whenever the date ticks over. */
    private fun addMonthlyCosts() {
        val recurring = get<RecurringSpendRepository>()
        appScope.launch {
            get<Today>().observe().collect { recurring.addDue(it) }
        }
    }
}
