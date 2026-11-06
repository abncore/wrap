package com.abaan.wrap.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.abaan.wrap.shared.data.ClientRepository
import com.abaan.wrap.shared.data.JobRepository
import com.abaan.wrap.shared.data.PaymentRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.TimeLogRepository
import com.abaan.wrap.shared.db.WrapDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.TimeZone
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

/** Provides the platform [SqlDriver]. */
expect val platformModule: Module

val sharedModule = module {
    single { WrapDatabase(get<SqlDriver>()) }
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<Clock> { Clock.System }
    single { TimeZone.currentSystemDefault() }

    single { SettingsRepository(get(), get()) }
    single { ClientRepository(get(), get()) }
    single { JobRepository(get(), get()) }
    single { TimeLogRepository(get(), get(), get()) }
    single { PaymentRepository(get(), get()) }
}

/** Starts Koin with the shared modules plus whatever the platform app declares. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(platformModule, sharedModule)
}
