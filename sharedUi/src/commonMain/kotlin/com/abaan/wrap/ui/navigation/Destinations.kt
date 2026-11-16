package com.abaan.wrap.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
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
data object ProfileRoute

@Serializable
data object ReportRoute

@Serializable
data object CalendarRoute

@Serializable
data object ReportsRoute

@Serializable
data object ClientPerformanceRoute

@Serializable
data class ClientReportRoute(val clientId: Long)

@Serializable
data object AgingRoute

@Serializable
data object WorkReportRoute

@Serializable
data object TutorialRoute

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

/** The top-level tabs shown in the bottom bar. Settings opens from Home's top bar. */
enum class TopLevelDestination(
    val route: Any,
    val labelRes: StringResource,
    val icon: @Composable () -> ImageVector,
) {
    HOME(HomeRoute, Res.string.nav_home, { Icons.Filled.Home }),
    JOBS(JobsRoute, Res.string.nav_jobs, { Icons.AutoMirrored.Filled.List }),
    MONEY(MoneyRoute, Res.string.nav_money, { vectorResource(Res.drawable.ic_insights) }),
    REPORTS(ReportsRoute, Res.string.nav_reports, { vectorResource(Res.drawable.ic_report) }),
    CLIENTS(ClientsRoute, Res.string.nav_clients, { Icons.Filled.Person }),
}

/** Actions offered by the central + button's bottom sheet. */
enum class AddAction {
    UPCOMING_JOB,
    FINISHED_JOB,
    PAYMENT,
    SPEND,
}
