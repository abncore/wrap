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
import com.abaan.wrap.ui.navigation.ClientEditRoute
import com.abaan.wrap.ui.navigation.ClientsRoute
import com.abaan.wrap.ui.navigation.JobDetailRoute
import com.abaan.wrap.ui.navigation.JobEditRoute
import com.abaan.wrap.ui.navigation.HomeRoute
import com.abaan.wrap.ui.navigation.JobsRoute
import com.abaan.wrap.ui.navigation.PaymentEditRoute
import com.abaan.wrap.ui.navigation.SettingsRoute
import com.abaan.wrap.ui.navigation.TopLevelDestination
import com.abaan.wrap.ui.navigation.WrapBottomBar
import com.abaan.wrap.ui.payments.PaymentEditScreen
import com.abaan.wrap.ui.home.HomeScreen
import com.abaan.wrap.ui.jobs.JobDetailScreen
import com.abaan.wrap.ui.jobs.JobEditScreen
import com.abaan.wrap.ui.jobs.JobsScreen
import com.abaan.wrap.ui.jobs.JobsTab
import com.abaan.wrap.ui.settings.SettingsScreen

private const val REQUESTED_JOBS_TAB = "requested_jobs_tab"

@Composable
fun WrapApp(
    openJobId: Long? = null,
    onJobOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canNotify(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(openJobId) {
        if (openJobId != null) {
            navController.navigate(JobDetailRoute(openJobId))
            onJobOpened()
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
                )
            }
            composable<JobsRoute> { entry ->
                val requestedTab by entry.savedStateHandle.getStateFlow<String?>(REQUESTED_JOBS_TAB, null).collectAsState()
                JobsScreen(
                    onOpenJob = { navController.navigate(JobDetailRoute(it)) },
                    requestedTab = requestedTab?.let(JobsTab::valueOf),
                    onRequestedTabShown = { entry.savedStateHandle[REQUESTED_JOBS_TAB] = null },
                )
            }
            composable<ClientsRoute> {
                ClientsScreen(
                    onAddClient = { navController.navigate(ClientEditRoute()) },
                    onOpenClient = { navController.navigate(ClientEditRoute(it)) },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
            composable<JobDetailRoute> { entry ->
                val route = entry.toRoute<JobDetailRoute>()
                JobDetailScreen(
                    jobId = route.jobId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(JobEditRoute(jobId = route.jobId)) },
                    onAddPayment = { navController.navigate(PaymentEditRoute(jobId = route.jobId)) },
                    onEditPayment = { navController.navigate(PaymentEditRoute(jobId = route.jobId, paymentId = it)) },
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
                ClientEditScreen(clientId = route.clientId, onClose = { navController.popBackStack() })
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
    }
}
