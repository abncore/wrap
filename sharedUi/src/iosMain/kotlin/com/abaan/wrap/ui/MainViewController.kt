package com.abaan.wrap.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.abaan.wrap.shared.data.RecurringSpendRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.di.initKoin
import com.abaan.wrap.ui.di.uiModule
import com.abaan.wrap.ui.settings.NightShoot
import com.abaan.wrap.ui.theme.WrapTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Called once from the iOS app's start-up, before any screen is shown. */
fun startWrap() {
    initKoin { modules(uiModule) }
    // Monthly costs are added as they come due: on launch and whenever the date ticks over.
    val koin = KoinPlatform.getKoin()
    val recurring = koin.get<RecurringSpendRepository>()
    appScope.launch {
        koin.get<Today>().observe().collect { recurring.addDue(it) }
    }
}

/** The whole app as a UIKit screen, shown by the SwiftUI app. */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val nightShoot = koinInject<NightShoot>()
    val night by nightShoot.enabled.collectAsState()
    WrapTheme(nightShoot = night) {
        WrapApp()
    }
}
