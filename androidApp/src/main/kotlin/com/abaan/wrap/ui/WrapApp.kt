package com.abaan.wrap.ui

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
import com.abaan.wrap.ui.navigation.AddAction
import com.abaan.wrap.ui.navigation.AddSheet
import com.abaan.wrap.ui.navigation.ClientsRoute
import com.abaan.wrap.ui.navigation.HomeRoute
import com.abaan.wrap.ui.navigation.JobsRoute
import com.abaan.wrap.ui.navigation.SettingsRoute
import com.abaan.wrap.ui.navigation.TopLevelDestination
import com.abaan.wrap.ui.navigation.WrapBottomBar
import com.abaan.wrap.ui.screens.ClientsScreen
import com.abaan.wrap.ui.screens.HomeScreen
import com.abaan.wrap.ui.screens.JobsScreen
import com.abaan.wrap.ui.screens.SettingsScreen

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
        bottomBar = {
            WrapBottomBar(
                current = currentTab,
                onSelect = { navController.navigateToTab(it) },
                onAddClick = { showAddSheet = true },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> { HomeScreen() }
            composable<JobsRoute> { JobsScreen() }
            composable<ClientsRoute> { ClientsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
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

/**
 * Every add action lands on the Jobs tab until the dedicated add forms exist
 * (jobs in step 5, payments in step 7), which will replace these targets.
 */
private fun NavHostController.handleAddAction(action: AddAction) {
    when (action) {
        AddAction.UPCOMING_JOB,
        AddAction.FINISHED_JOB,
        AddAction.PAYMENT,
        -> navigateToTab(TopLevelDestination.JOBS)
    }
}
