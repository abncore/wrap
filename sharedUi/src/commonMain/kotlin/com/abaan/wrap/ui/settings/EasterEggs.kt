package com.abaan.wrap.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import com.abaan.wrap.shared.data.CrewRepository
import com.abaan.wrap.shared.data.LedgerRepository
import com.abaan.wrap.shared.data.PaymentRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.data.SpendRepository
import com.abaan.wrap.shared.data.Today
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.money.BlooperReel
import com.abaan.wrap.shared.money.EasterEggs
import com.abaan.wrap.shared.money.EndCredits
import com.abaan.wrap.ui.components.formatDuration
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.formatShort
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.platform.KeyValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The hidden "Night shoot" theme, unlocked by tapping the credits line ten times. Remembered on this phone. */
class NightShoot(private val prefs: KeyValues) {
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Turns it on or off; returns whether it is now on. */
    fun toggle(): Boolean {
        val on = !_enabled.value
        prefs.putBoolean(KEY, on)
        _enabled.value = on
        return on
    }

    companion object {
        /** Taps on the credits line that switch it. */
        const val TAPS = 10
        private const val KEY = "night_shoot"
    }
}

data class CreditsUiState(
    val reel: BlooperReel? = null,
    val credits: EndCredits? = null,
    val currency: String = AppSettings.DEFAULT_CURRENCY,
)

class CreditsViewModel(
    ledger: LedgerRepository,
    payments: PaymentRepository,
    spends: SpendRepository,
    crew: CrewRepository,
    settings: SettingsRepository,
    today: Today,
) : ViewModel() {
    val state: StateFlow<CreditsUiState> = combine(
        ledger.observeJobSummaries(),
        payments.observeAll(),
        spends.observeAll(),
        crew.observeAll(),
        settings.observeSettings(),
    ) { summaries, paymentList, spendList, crewList, appSettings ->
        CreditsUiState(
            reel = EasterEggs.blooperReel(summaries, paymentList, spendList, today.timeZone),
            credits = EasterEggs.endCredits(summaries, spendList, crewList),
            currency = appSettings.currency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditsUiState())
}

/** Tap the credits line: highlights from your own work. */
@Composable
fun BlooperReelDialog(reel: BlooperReel, currency: String, onDismiss: () -> Unit) {
    val lines = buildList {
        if (reel.minutesOnSet > 0) add(stringResource(Res.string.egg_reel_on_set, formatDuration(reel.minutesOnSet)))
        if (reel.daysOnSet > 0) add(pluralStringResource(Res.plurals.egg_reel_days_line, reel.daysOnSet, reel.daysOnSet))
        if (reel.longestShiftMinutes != null && reel.longestShiftDate != null) {
            add(
                stringResource(
                    Res.string.egg_reel_longest,
                    formatDuration(reel.longestShiftMinutes!!),
                    reel.longestShiftJob.orEmpty(),
                    reel.longestShiftDate!!.formatMedium(),
                ),
            )
        }
        if (reel.overtimeMinutes > 0) add(stringResource(Res.string.egg_reel_overtime, formatDuration(reel.overtimeMinutes)))
        reel.earliestCall?.let { add(stringResource(Res.string.egg_reel_earliest, it.formatShort(), reel.earliestCallJob.orEmpty())) }
        reel.loyalClient?.let { add(pluralStringResource(Res.plurals.egg_reel_loyal, reel.loyalClientJobs, it, reel.loyalClientJobs)) }
        if (reel.biggestPayment != null && reel.biggestPaymentDate != null) {
            add(stringResource(Res.string.egg_reel_payday, money(reel.biggestPayment!!, currency), reel.biggestPaymentDate!!.formatMedium()))
        }
        reel.topSpendCategory?.let {
            add(stringResource(Res.string.egg_reel_spend, stringResource(it.labelRes()), money(reel.topSpendAmount, currency)))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.egg_reel_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (lines.isEmpty()) Text(stringResource(Res.string.egg_reel_empty))
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.egg_reel_close)) } },
    )
}

/** Long-press the credits line: your work rolls past like the end of a film. Tap anywhere to stop. */
@Composable
fun EndCreditsRoll(credits: EndCredits, currency: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            val scroll = rememberScrollState()
            val screen = maxHeight
            LaunchedEffect(scroll.maxValue) {
                if (scroll.maxValue > 0) {
                    // Slow enough to read, like real end credits: about ten seconds a screen.
                    val duration = (scroll.maxValue / constraints.maxHeight.toFloat() * 10_000).toInt().coerceAtLeast(15_000)
                    scroll.animateScrollTo(scroll.maxValue, tween(durationMillis = duration, easing = LinearEasing))
                    onDismiss()
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(scroll, enabled = false).padding(horizontal = 32.dp),
            ) {
                Spacer(Modifier.height(screen))
                CreditTitle(stringResource(Res.string.app_name), big = true)
                Spacer(Modifier.height(48.dp))
                Credit(stringResource(Res.string.egg_credits_starring), listOf(stringResource(Res.string.egg_credits_you)))
                Credit(stringResource(Res.string.egg_credits_as), credits.roles)
                Credit(stringResource(Res.string.egg_credits_produced), credits.producers)
                Credit(stringResource(Res.string.egg_credits_crew), credits.crew)
                Credit(stringResource(Res.string.egg_credits_locations), credits.locations)
                if (credits.cateringSpent > 0) Credit(stringResource(Res.string.egg_credits_catering), listOf(money(credits.cateringSpent, currency)))
                if (credits.transportSpent > 0) Credit(stringResource(Res.string.egg_credits_transport), listOf(money(credits.transportSpent, currency)))
                if (credits.kitSpent > 0) Credit(stringResource(Res.string.egg_credits_kit), listOf(money(credits.kitSpent, currency)))
                Credit(
                    stringResource(Res.string.egg_credits_running_time),
                    listOf(
                        pluralStringResource(Res.plurals.report_jobs, credits.jobs, credits.jobs),
                        pluralStringResource(Res.plurals.egg_reel_days, credits.daysOnSet, credits.daysOnSet),
                    ),
                )
                Spacer(Modifier.height(64.dp))
                CreditTitle(stringResource(Res.string.egg_credits_wrap), big = true)
                Spacer(Modifier.height(24.dp))
                CreditLine(stringResource(Res.string.settings_credits))
                Spacer(Modifier.height(screen))
            }
        }
    }
}

@Composable
private fun Credit(role: String, names: List<String>) {
    if (names.isEmpty()) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 18.dp)) {
        Text(role.uppercase(), color = Color(0xFFBDBDBD), fontSize = 13.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        names.take(MAX_NAMES).forEach { CreditLine(it) }
    }
}

@Composable
private fun CreditLine(text: String) {
    Text(text, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun CreditTitle(text: String, big: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.White,
            fontSize = if (big) 40.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp,
        )
    }
}

private const val MAX_NAMES = 12
