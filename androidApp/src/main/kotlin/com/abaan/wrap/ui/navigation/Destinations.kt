package com.abaan.wrap.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.abaan.wrap.R
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object JobsRoute

@Serializable
data object ClientsRoute

@Serializable
data object SettingsRoute

/** The four top-level tabs shown in the bottom bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Filled.Home),
    JOBS(JobsRoute, R.string.nav_jobs, Icons.AutoMirrored.Filled.List),
    CLIENTS(ClientsRoute, R.string.nav_clients, Icons.Filled.Person),
    SETTINGS(SettingsRoute, R.string.nav_settings, Icons.Filled.Settings),
}

/** Actions offered by the central + button's bottom sheet. */
enum class AddAction {
    UPCOMING_JOB,
    FINISHED_JOB,
    PAYMENT,
}
