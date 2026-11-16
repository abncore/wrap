package com.abaan.wrap.ui.settings


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import com.abaan.wrap.ui.components.formatDateTime
import com.abaan.wrap.ui.platform.AppInfo
import com.abaan.wrap.ui.platform.BackupFiles
import com.abaan.wrap.ui.platform.PhoneCalendar
import com.abaan.wrap.ui.platform.PlatformFeatures
import com.abaan.wrap.ui.platform.ScreenLock
import com.abaan.wrap.ui.platform.rememberCreateDocument
import com.abaan.wrap.ui.platform.rememberOpenDocument
import com.abaan.wrap.ui.platform.rememberPermissionsRequest
import com.abaan.wrap.ui.platform.rememberPhoneActions
import org.jetbrains.compose.resources.getString

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

import org.koin.compose.koinInject

import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.money.MoneyFormat
import com.abaan.wrap.ui.components.DecimalInput
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.formatDecimal
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.money.BudgetDialog
import org.koin.compose.viewmodel.koinViewModel

private enum class SettingDialog { RATE, DAY_LENGTH, OT_MULTIPLIER, TERMS, CURRENCY, BUDGET, CALL_REMINDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTutorial: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val profile = state.profile
    val actions = rememberPhoneActions()
    val features = koinInject<PlatformFeatures>()
    val calendarSync = koinInject<PhoneCalendar>()
    val calendarDenied = stringResource(Res.string.settings_calendar_denied)
    val calendarPermission = rememberPermissionsRequest { granted ->
        if (granted) {
            calendarSync.onPermissionChanged()
            viewModel.update { it.copy(calendarSyncEnabled = true) }
        } else {
            actions.showMessage(calendarDenied)
        }
    }
    val backupViewModel: BackupViewModel = koinViewModel()
    val backup by backupViewModel.state.collectAsStateWithLifecycle()
    val backupMimeType = koinInject<BackupFiles>().mimeType
    val saveBackup = rememberCreateDocument(backupMimeType, backupViewModel::save)
    val openBackup = rememberOpenDocument(backupViewModel::open)
    LaunchedEffect(backup.message) {
        val message = backup.message ?: return@LaunchedEffect
        val text = when (message) {
            BackupMessage.SAVED -> Res.string.backup_saved
            BackupMessage.SAVE_FAILED -> Res.string.backup_save_failed
            BackupMessage.NOT_A_BACKUP -> Res.string.backup_not_a_backup
            BackupMessage.RESTORED -> Res.string.backup_restored
            BackupMessage.RESTORE_FAILED -> Res.string.backup_restore_failed
        }
        actions.showMessage(getString(text))
        backupViewModel.messageShown()
    }
    val setCalendarSync: (Boolean) -> Unit = { on ->
        if (on && !calendarSync.hasPermission()) {
            calendarPermission(calendarSync.permissions)
        } else {
            viewModel.update { it.copy(calendarSyncEnabled = on) }
        }
    }
    var dialog by rememberSaveable { mutableStateOf<SettingDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingRow(
                title = stringResource(Res.string.settings_profile),
                value = profile.displayName.ifBlank { stringResource(Res.string.settings_profile_empty) },
                onClick = onOpenProfile,
            )
            if (features.backups) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsHeader(stringResource(Res.string.settings_section_backup))
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val backupFileName = stringResource(Res.string.backup_file_name, today.toString())
                SettingRow(
                    title = stringResource(Res.string.backup_now),
                    value = backup.lastBackupAt?.let { stringResource(Res.string.backup_last, formatDateTime(it)) }
                        ?: stringResource(Res.string.backup_never),
                    onClick = { if (!backup.working) saveBackup(backupFileName) },
                )
                SettingRow(
                    title = stringResource(Res.string.backup_restore),
                    value = stringResource(Res.string.backup_restore_desc),
                    onClick = {
                        if (!backup.working) openBackup(listOf(backupMimeType, "application/octet-stream", "application/x-zip-compressed"))
                    },
                )
                if (backup.working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsHeader(stringResource(Res.string.settings_section_defaults))
            SettingRow(
                title = stringResource(Res.string.settings_default_rate),
                value = if (settings.defaultRate > 0) money(settings.defaultRate, settings.currency) else stringResource(Res.string.settings_not_set),
                onClick = { dialog = SettingDialog.RATE },
            )
            SettingRow(
                title = stringResource(Res.string.settings_day_length),
                value = stringResource(Res.string.settings_day_length_value, formatDecimal(settings.dayLengthHours)),
                onClick = { dialog = SettingDialog.DAY_LENGTH },
            )
            SettingRow(
                title = stringResource(Res.string.settings_ot_multiplier),
                value = stringResource(Res.string.settings_ot_multiplier_value, formatDecimal(settings.otMultiplier)),
                onClick = { dialog = SettingDialog.OT_MULTIPLIER },
            )
            SettingRow(
                title = stringResource(Res.string.settings_payment_terms),
                value = pluralStringResource(Res.plurals.settings_payment_terms_value, settings.paymentTermsDays, settings.paymentTermsDays),
                onClick = { dialog = SettingDialog.TERMS },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsHeader(stringResource(Res.string.settings_section_spending))
            SettingRow(
                title = stringResource(Res.string.settings_budget),
                value = if (settings.monthlyBudget > 0) money(settings.monthlyBudget, settings.currency) else stringResource(Res.string.settings_not_set),
                onClick = { dialog = SettingDialog.BUDGET },
            )
            if (features.notifications) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_budget_alerts)) },
                    supportingContent = { Text(stringResource(Res.string.settings_budget_alerts_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.budgetAlertsEnabled,
                            onCheckedChange = { checked -> viewModel.update { it.copy(budgetAlertsEnabled = checked) } },
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.update { it.copy(budgetAlertsEnabled = !it.budgetAlertsEnabled) }
                    },
                )
            }

            if (features.appLock) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsHeader(stringResource(Res.string.settings_section_privacy))
                val screenLock = koinInject<ScreenLock>()
                val lockUnavailable = stringResource(Res.string.settings_app_lock_unavailable)
                val setAppLock: (Boolean) -> Unit = { on ->
                    if (on && !screenLock.isAvailable()) {
                        actions.showMessage(lockUnavailable)
                    } else {
                        viewModel.update { it.copy(appLockEnabled = on) }
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_app_lock)) },
                    supportingContent = { Text(stringResource(Res.string.settings_app_lock_desc)) },
                    trailingContent = { Switch(checked = settings.appLockEnabled, onCheckedChange = setAppLock) },
                    modifier = Modifier.clickable { setAppLock(!settings.appLockEnabled) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsHeader(stringResource(Res.string.settings_section_app))
            SettingRow(
                title = stringResource(Res.string.settings_currency),
                value = settings.currency,
                onClick = { dialog = SettingDialog.CURRENCY },
            )
            if (features.notifications) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_reminders)) },
                    supportingContent = { Text(stringResource(Res.string.settings_reminders_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.remindersEnabled,
                            onCheckedChange = { checked -> viewModel.update { it.copy(remindersEnabled = checked) } },
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.update { it.copy(remindersEnabled = !it.remindersEnabled) }
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_chase_reminders)) },
                    supportingContent = { Text(stringResource(Res.string.settings_chase_reminders_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.chaseRemindersEnabled,
                            onCheckedChange = { checked -> viewModel.update { it.copy(chaseRemindersEnabled = checked) } },
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.update { it.copy(chaseRemindersEnabled = !it.chaseRemindersEnabled) }
                    },
                )
            }
            if (features.calendarSync) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_calendar_sync)) },
                    supportingContent = { Text(stringResource(Res.string.settings_calendar_sync_desc)) },
                    trailingContent = {
                        Switch(checked = settings.calendarSyncEnabled, onCheckedChange = setCalendarSync)
                    },
                    modifier = Modifier.clickable { setCalendarSync(!settings.calendarSyncEnabled) },
                )
            }
            if (features.notifications) {
                SettingRow(
                    title = stringResource(Res.string.settings_call_reminder),
                    value = callReminderLabel(settings.callReminderMinutes),
                    onClick = { dialog = SettingDialog.CALL_REMINDER },
                )
            }
            SettingRow(
                title = stringResource(Res.string.settings_tutorial),
                value = stringResource(Res.string.settings_tutorial_desc),
                onClick = onOpenTutorial,
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_version)) },
                supportingContent = { Text(koinInject<AppInfo>().versionName) },
            )
            CreditsLine()
        }
    }

    backup.pending?.let { pending ->
        val summary = pending.summary
        AlertDialog(
            onDismissRequest = backupViewModel::cancelRestore,
            title = { Text(stringResource(Res.string.backup_confirm_title, formatDateTime(summary.createdAt))) },
            text = {
                Text(stringResource(Res.string.backup_confirm_body, summary.clients, summary.jobs, summary.payments, summary.spends))
            },
            confirmButton = {
                TextButton(onClick = backupViewModel::confirmRestore) {
                    Text(stringResource(Res.string.backup_confirm_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = backupViewModel::cancelRestore) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }

    when (dialog) {
        SettingDialog.RATE -> TextSettingDialog(
            title = stringResource(Res.string.settings_default_rate),
            initial = if (settings.defaultRate > 0) MoneyFormat.toInput(settings.defaultRate) else "",
            parse = MoneyFormat::parse,
            errorText = stringResource(Res.string.error_invalid_amount),
            onDismiss = { dialog = null },
            onSave = { value -> viewModel.update { it.copy(defaultRate = value) } },
            field = { value, onChange, error ->
                MoneyInput(value, onChange, stringResource(Res.string.settings_default_rate), settings.currency, error = error)
            },
        )
        SettingDialog.DAY_LENGTH -> TextSettingDialog(
            title = stringResource(Res.string.settings_day_length),
            initial = formatDecimal(settings.dayLengthHours),
            parse = { it.toDoubleOrNull()?.takeIf { hours -> hours > 0 && hours <= 24 } },
            errorText = stringResource(Res.string.error_invalid_number),
            onDismiss = { dialog = null },
            onSave = { value -> viewModel.update { it.copy(dayLengthHours = value) } },
            field = { value, onChange, error ->
                DecimalInput(value, onChange, stringResource(Res.string.job_field_day_length), error = error)
            },
        )
        SettingDialog.OT_MULTIPLIER -> TextSettingDialog(
            title = stringResource(Res.string.settings_ot_multiplier),
            initial = formatDecimal(settings.otMultiplier),
            parse = { it.toDoubleOrNull()?.takeIf { multiplier -> multiplier >= 0 && multiplier <= 10 } },
            errorText = stringResource(Res.string.error_invalid_number),
            onDismiss = { dialog = null },
            onSave = { value -> viewModel.update { it.copy(otMultiplier = value) } },
            field = { value, onChange, error ->
                DecimalInput(value, onChange, stringResource(Res.string.settings_ot_multiplier), error = error, suffix = "×")
            },
        )
        SettingDialog.TERMS -> TextSettingDialog(
            title = stringResource(Res.string.settings_payment_terms),
            initial = settings.paymentTermsDays.toString(),
            parse = { it.toIntOrNull()?.takeIf { days -> days in 0..365 } },
            errorText = stringResource(Res.string.error_invalid_number),
            onDismiss = { dialog = null },
            onSave = { value -> viewModel.update { it.copy(paymentTermsDays = value) } },
            field = { value, onChange, error ->
                DecimalInput(value, onChange, stringResource(Res.string.client_field_terms), error = error, integerOnly = true)
            },
        )
        SettingDialog.CURRENCY -> CurrencyDialog(
            selected = settings.currency,
            onDismiss = { dialog = null },
            onSelect = { currency ->
                viewModel.update { it.copy(currency = currency) }
                dialog = null
            },
        )
        SettingDialog.BUDGET -> BudgetDialog(
            current = settings.monthlyBudget,
            currency = settings.currency,
            onDismiss = { dialog = null },
            onSave = { value ->
                viewModel.update { it.copy(monthlyBudget = value) }
                dialog = null
            },
        )
        SettingDialog.CALL_REMINDER -> OptionsDialog(
            title = stringResource(Res.string.settings_call_reminder),
            options = AppSettings.CALL_REMINDER_OPTIONS,
            selected = settings.callReminderMinutes,
            label = { callReminderLabel(it) },
            onDismiss = { dialog = null },
            onSelect = { minutes ->
                viewModel.update { it.copy(callReminderMinutes = minutes) }
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun callReminderLabel(minutes: Int): String = when {
    minutes <= 0 -> stringResource(Res.string.settings_call_reminder_off)
    minutes % 60 == 0 || minutes % 30 == 0 && minutes > 60 -> {
        val hours = minutes / 60.0
        pluralStringResource(Res.plurals.settings_call_reminder_hours, if (hours == 1.0) 1 else 2, formatDecimal(hours))
    }
    else -> pluralStringResource(Res.plurals.settings_call_reminder_minutes, minutes, minutes)
}

@Composable
private fun <T> OptionsDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(label(option)) },
                        leadingContent = { RadioButton(selected = option == selected, onClick = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/**
 * The credit line, with its easter eggs: a tap opens the blooper reel, a long press rolls the end
 * credits, and ten quick taps switch the hidden Night shoot theme. The reel waits until the taps
 * stop, so a run of taps towards ten doesn't keep opening it.
 */
@Composable
private fun CreditsLine(viewModel: CreditsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nightShoot = koinInject<NightShoot>()
    val actions = rememberPhoneActions()
    val nightOn = stringResource(Res.string.egg_night_on)
    val nightOff = stringResource(Res.string.egg_night_off)
    val scope = rememberCoroutineScope()
    var taps by remember { mutableIntStateOf(0) }
    var pendingReel by remember { mutableStateOf<Job?>(null) }
    var showReel by rememberSaveable { mutableStateOf(false) }
    var showCredits by rememberSaveable { mutableStateOf(false) }

    Text(
        text = stringResource(Res.string.settings_credits),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    taps += 1
                    pendingReel?.cancel()
                    if (taps >= NightShoot.TAPS) {
                        taps = 0
                        val on = nightShoot.toggle()
                        actions.showMessage(if (on) nightOn else nightOff)
                    } else {
                        pendingReel = scope.launch {
                            delay(TAP_PAUSE_MS)
                            taps = 0
                            showReel = true
                        }
                    }
                },
                onLongClick = { showCredits = true },
            )
            .padding(top = 16.dp, bottom = 32.dp),
    )

    val reel = state.reel
    if (showReel && reel != null) BlooperReelDialog(reel, state.currency, onDismiss = { showReel = false })
    val credits = state.credits
    if (showCredits && credits != null) EndCreditsRoll(credits, state.currency, onDismiss = { showCredits = false })
}

/** How long after the last tap on the credits the blooper reel opens. */
private const val TAP_PAUSE_MS = 600L


@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun <T> TextSettingDialog(
    title: String,
    initial: String,
    parse: (String) -> T?,
    errorText: String,
    onDismiss: () -> Unit,
    onSave: (T) -> Unit,
    field: @Composable (value: String, onChange: (String) -> Unit, error: String?) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            field(value, { value = it; showError = false }, errorText.takeIf { showError })
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = parse(value)
                if (parsed == null) {
                    showError = true
                } else {
                    onSave(parsed)
                    onDismiss()
                }
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun CurrencyDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_currency)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_currency_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AppSettings.SUPPORTED_CURRENCIES.forEach { currency ->
                    ListItem(
                        headlineContent = { Text(currency) },
                        leadingContent = { RadioButton(selected = currency == selected, onClick = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = currency == selected, role = Role.RadioButton) { onSelect(currency) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
