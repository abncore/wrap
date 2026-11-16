package com.abaan.wrap.ui.di

import android.content.Context
import androidx.core.content.edit
import com.abaan.wrap.files.SharedFiles
import com.abaan.wrap.ui.platform.ExportFiles
import com.abaan.wrap.ui.platform.KeyValueFiles
import com.abaan.wrap.ui.platform.KeyValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The Android services the screens need that don't depend on the rest of the app. The app module
 * adds the bigger ones (receipts, invoices, backups, calendar, lock, alerts).
 */
actual val uiPlatformModule: Module = module {
    single<KeyValueFiles> {
        val context = androidContext()
        KeyValueFiles { name -> SharedPreferencesValues(context.getSharedPreferences(name, Context.MODE_PRIVATE)) }
    }
    single<ExportFiles> {
        val context = androidContext()
        ExportFiles { name, text ->
            withContext(Dispatchers.IO) {
                SharedFiles.file(context, name).apply { writeText(text) }.path
            }
        }
    }
}

private class SharedPreferencesValues(private val prefs: android.content.SharedPreferences) : KeyValues {
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
}
