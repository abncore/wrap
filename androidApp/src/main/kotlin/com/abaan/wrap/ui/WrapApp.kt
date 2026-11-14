package com.abaan.wrap.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.abaan.wrap.reminders.ReminderNotifications
import com.abaan.wrap.ui.clients.ClientEditScreen
import com.abaan.wrap.ui.clients.ClientsScreen
import com.abaan.wrap.ui.navigation.AddAction
import com.abaan.wrap.ui.navigation.AddSheet
import com.abaan.wrap.ui.navigation.AgingRoute
import com.abaan.wrap.ui.navigation.CalendarRoute
import com.abaan.wrap.ui.navigation.ClientPerformanceRoute
import com.abaan.wrap.ui.navigation.ClientReportRoute
import com.abaan.wrap.ui.navigation.ReportsRoute
import com.abaan.wrap.ui.navigation.TutorialRoute
import com.abaan.wrap.ui.navigation.WorkReportRoute
import com.abaan.wrap.ui.reports.AgingScreen
import com.abaan.wrap.ui.reports.ClientPerformanceScreen
import com.abaan.wrap.ui.reports.ClientReportScreen
import com.abaan.wrap.ui.reports.ReportsScreen
import com.abaan.wrap.ui.reports.WorkReportScreen
import com.abaan.wrap.ui.tutorial.Onboarding
import com.abaan.wrap.ui.tutorial.TutorialScreen
import org.koin.compose.koinInject
import com.abaan.wrap.ui.navigation.ClientEditRoute
import com.abaan.wrap.ui.navigation.ClientsRoute
import com.abaan.wrap.ui.navigation.JobDetailRoute
import com.abaan.wrap.ui.navigation.JobEditRoute
import com.abaan.wrap.ui.navigation.HomeRoute
import com.abaan.wrap.ui.navigation.JobsRoute
import com.abaan.wrap.ui.navigation.MoneyRoute
import com.abaan.wrap.ui.navigation.PaymentEditRoute
import com.abaan.wrap.ui.navigation.ProfileRoute
import com.abaan.wrap.ui.navigation.ReportRoute
import com.abaan.wrap.ui.navigation.SettingsRoute
import com.abaan.wrap.ui.navigation.SpendEditRoute
import com.abaan.wrap.ui.navigation.TopLevelDestination
import com.abaan.wrap.ui.navigation.WrapBottomBar
import com.abaan.wrap.ui.payments.PaymentEditScreen
import com.abaan.wrap.ui.home.HomeScreen
import com.abaan.wrap.ui.jobs.CalendarScreen
import com.abaan.wrap.ui.jobs.JobDetailScreen
import com.abaan.wrap.ui.jobs.JobEditScreen
import com.abaan.wrap.ui.jobs.JobsScreen
import com.abaan.wrap.ui.jobs.JobsTab
import com.abaan.wrap.ui.money.MoneyScreen
import com.abaan.wrap.ui.money.ReportScreen
import com.abaan.wrap.ui.money.SpendEditScreen
import com.abaan.wrap.ui.settings.ProfileScreen
import com.abaan.wrap.ui.settings.SettingsScreen

private const val REQUESTED_JOBS_TAB = "requested_jobs_tab"

@Composable
fun WrapApp(
    openJobId: Long? = null,
    onJobOpened: () -> Unit = {},
    openMoney: Boolean = false,
    onMoneyOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canNotify(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val onboarding = koinInject<Onboarding>()
    LaunchedEffect(Unit) {
        if (!onboarding.tutorialSeen) navController.navigate(TutorialRoute)
    }
    LaunchedEffect(openJobId) {
        if (openJobId != null) {
            navController.navigate(JobDetailRoute(openJobId))
            onJobOpened()
        }
    }
    LaunchedEffect(openMoney) {
        if (openMoney) {
            navController.navigateToTab(TopLevelDestination.MONEY)
            onMoneyOpened()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Each screen draws its own top bar and handles system insets itself.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                WrapBottomBar(
                    current = currentTab,
                    onSelect = { navController.navigateToTab(it) },
                    onAddClick = { showAddSheet = true },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onAddJob = { showAddSheet = true },
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                    onOpenUnpaid = {
                        navController.navigateToTab(TopLevelDestination.JOBS)
                        navController.getBackStackEntry<JobsRoute>().savedStateHandle[REQUESTED_JOBS_TAB] = JobsTab.UNPAID.name
                    },
                    onOpenMoney = { navController.navigateToTab(TopLevelDestination.MONEY) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<JobsRoute> { entry ->
                val requestedTab by entry.savedStateHandle.getStateFlow<String?>(REQUESTED_JOBS_TAB, null).collectAsState()
                JobsScreen(
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                    requestedTab = requestedTab?.let(JobsTab::valueOf),
                    onRequestedTabShown = { entry.savedStateHandle[REQUESTED_JOBS_TAB] = null },
                    onOpenCalendar = { navController.navigate(CalendarRoute) },
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                )
            }
            composable<ClientsRoute> {
                ClientsScreen(
                    onAddClient = { navController.navigate(ClientEditRoute()) },
                    onOpenClient = { navController.navigate(ClientEditRoute(it)) },
                )
            }
            composable<MoneyRoute> {
                MoneyScreen(
                    onAddSpend = { navController.navigate(SpendEditRoute()) },
                    onOpenSpend = { navController.navigate(SpendEditRoute(it)) },
                )
            }
            composable<ReportRoute> { ReportScreen(onBack = { navController.popBackStack() }) }
            composable<ReportsRoute> {
                ReportsScreen(
                    onOpenYear = { navController.navigate(ReportRoute) },
                    onOpenClients = { navController.navigate(ClientPerformanceRoute) },
                    onOpenAging = { navController.navigate(AgingRoute) },
                    onOpenWork = { navController.navigate(WorkReportRoute) },
                )
            }
            composable<ClientPerformanceRoute> {
                ClientPerformanceScreen(
                    onBack = { navController.popBackStack() },
                    onOpenClient = { navController.navigate(ClientReportRoute(it)) },
                )
            }
            composable<ClientReportRoute> { entry ->
                ClientReportScreen(
                    clientId = entry.toRoute<ClientReportRoute>().clientId,
                    onBack = { navController.popBackStack() },
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                )
            }
            composable<AgingRoute> {
                AgingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                    onOpenClient = { navController.navigate(ClientReportRoute(it)) },
                )
            }
            composable<WorkReportRoute> { WorkReportScreen(onBack = { navController.popBackStack() }) }
            composable<TutorialRoute> {
                TutorialScreen(onDone = {
                    onboarding.markTutorialSeen()
                    navController.popBackStack()
                })
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { navController.navigate(ProfileRoute) },
                    onOpenTutorial = { navController.navigate(TutorialRoute) },
                )
            }
            composable<ProfileRoute> { ProfileScreen(onClose = { navController.popBackStack() }) }
            composable<JobDetailRoute> { entry ->
                val route = entry.toRoute<JobDetailRoute>()
                JobDetailScreen(
                    jobId = route.jobId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(JobEditRoute(jobId = route.jobId)) },
                    onAddPayment = { navController.navigate(PaymentEditRoute(jobId = route.jobId)) },
                    onEditPayment = { navController.navigate(PaymentEditRoute(jobId = route.jobId, paymentId = it)) },
                    onOpenProfile = { navController.navigate(ProfileRoute) },
                )
            }
            composable<JobEditRoute> { entry ->
                val route = entry.toRoute<JobEditRoute>()
                JobEditScreen(
                    jobId = route.jobId,
                    finished = route.finished,
                    onClose = { navController.popBackStack() },
                    onSaved = { id, isNew ->
                        if (isNew) {
                            navController.navigate(JobDetailRoute(id)) {
                                popUpTo<JobEditRoute> { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable<PaymentEditRoute> { entry ->
                val route = entry.toRoute<PaymentEditRoute>()
                PaymentEditScreen(
                    jobId = route.jobId,
                    paymentId = route.paymentId,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<ClientEditRoute> { entry ->
                val route = entry.toRoute<ClientEditRoute>()
                ClientEditScreen(
                    clientId = route.clientId,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<SpendEditRoute> { entry ->
                val route = entry.toRoute<SpendEditRoute>()
                SpendEditScreen(spendId = route.spendId, onClose = { navController.popBackStack() })
            }
        }
    }

    if (showAddSheet) {
        AddSheet(
            onDismiss = { showAddSheet = false },
            onAction = { action -> navController.handleAddAction(action) },
        )
    }
}

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.handleAddAction(action: AddAction) {
    when (action) {
        AddAction.UPCOMING_JOB -> navigate(JobEditRoute())
        AddAction.FINISHED_JOB -> navigate(JobEditRoute(finished = true))
        AddAction.PAYMENT -> navigate(PaymentEditRoute())
        AddAction.SPEND -> navigate(SpendEditRoute())
    }
}
