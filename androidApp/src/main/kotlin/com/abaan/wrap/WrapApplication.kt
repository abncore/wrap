package com.abaan.wrap

import android.app.Application
import com.abaan.wrap.di.appModule
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.reminders.ReminderScheduler
import com.abaan.wrap.shared.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class WrapApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@WrapApplication)
            modules(appModule)
        }
        ReminderNotifications.createChannel(this)
        get<ReminderScheduler>().start(appScope)
    }
}
