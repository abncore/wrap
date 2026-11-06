package com.abaan.wrap

import android.app.Application
import com.abaan.wrap.di.appModule
import com.abaan.wrap.shared.di.initKoin
import org.koin.android.ext.koin.androidContext

class WrapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@WrapApplication)
            modules(appModule)
        }
    }
}
