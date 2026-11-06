package com.abaan.wrap.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.NEW_ID
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object JobsRoute

@Serializable
data object MoneyRoute

@Serializable
data object ClientsRoute

@Serializable
data object SettingsRoute

@Serializable
data class JobDetailRoute(val jobId: Long)

/** Add ([jobId] = [NEW_ID]) or edit a job; [finished] opens the "log finished job" form. */
@Serializable
data class JobEditRoute(val jobId: Long = NEW_ID, val finished: Boolean = false)

/** Record a payment, optionally for a fixed [jobId], or edit [paymentId]. */
@Serializable
data class PaymentEditRoute(val jobId: Long = NEW_ID, val paymentId: Long = NEW_ID)

@Serializable
data class ClientEditRoute(val clientId: Long = NEW_ID)

/** Add ([spendId] = [NEW_ID]) or edit a spending entry. */
@Serializable
data class SpendEditRoute(val spendId: Long = NEW_ID)

/** The four top-level tabs shown in the bottom bar. Settings opens from Home's top bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: @Composable () -> ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, { Icons.Filled.Home }),
    JOBS(JobsRoute, R.string.nav_jobs, { Icons.AutoMirrored.Filled.List }),
    MONEY(MoneyRoute, R.string.nav_money, { ImageVector.vectorResource(R.drawable.ic_insights) }),
    CLIENTS(ClientsRoute, R.string.nav_clients, { Icons.Filled.Person }),
}

/** Actions offered by the central + button's bottom sheet. */
enum class AddAction {
    UPCOMING_JOB,
    FINISHED_JOB,
    PAYMENT,
    SPEND,
}
