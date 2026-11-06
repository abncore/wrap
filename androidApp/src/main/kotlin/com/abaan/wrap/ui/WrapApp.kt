package com.abaan.wrap.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.abaan.wrap.ui.clients.ClientEditScreen
import com.abaan.wrap.ui.clients.ClientsScreen
import com.abaan.wrap.ui.navigation.AddAction
import com.abaan.wrap.ui.navigation.AddSheet
import com.abaan.wrap.ui.navigation.ClientEditRoute
import com.abaan.wrap.ui.navigation.ClientsRoute
import com.abaan.wrap.ui.navigation.HomeRoute
import com.abaan.wrap.ui.navigation.JobsRoute
import com.abaan.wrap.ui.navigation.SettingsRoute
import com.abaan.wrap.ui.navigation.TopLevelDestination
import com.abaan.wrap.ui.navigation.WrapBottomBar
import com.abaan.wrap.ui.screens.HomeScreen
import com.abaan.wrap.ui.screens.JobsScreen
import com.abaan.wrap.ui.settings.SettingsScreen

@Composable
fun WrapApp() {
    val navController = rememberNavController()
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
                HomeScreen(onAddJob = { showAddSheet = true })
            }
            composable<JobsRoute> { JobsScreen() }
            composable<ClientsRoute> {
                ClientsScreen(
                    onAddClient = { navController.navigate(ClientEditRoute()) },
                    onOpenClient = { navController.navigate(ClientEditRoute(it)) },
                )
            }
            composable<SettingsRoute> { SettingsScreen() }
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
        AddAction.UPCOMING_JOB,
        AddAction.FINISHED_JOB,
        AddAction.PAYMENT,
        -> navigateToTab(TopLevelDestination.JOBS)
    }
}
