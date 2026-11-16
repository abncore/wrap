package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.RateType
import com.abaan.wrap.ui.components.DateInput
import com.abaan.wrap.ui.components.DecimalInput
import com.abaan.wrap.ui.components.DropdownInput
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.SectionTitle
import com.abaan.wrap.ui.settings.vatPercent
import com.abaan.wrap.shared.money.InvoiceRules
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.Color
import com.abaan.wrap.ui.components.TextInput
import com.abaan.wrap.ui.components.TimeInput
import com.abaan.wrap.ui.components.fieldLabelRes
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobEditScreen(
    jobId: Long,
    finished: Boolean,
    onClose: () -> Unit,
    onSaved: (jobId: Long, isNew: Boolean) -> Unit,
    viewModel: JobEditViewModel = koinViewModel(key = "job-edit-$jobId-$finished") { parametersOf(jobId, finished) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    var showNewClient by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.savedJobId) { state.savedJobId?.let { onSaved(it, state.isNew) } }

    val title = when {
        !state.isNew -> Res.string.job_edit
        state.finished -> Res.string.job_new_finished
        else -> Res.string.job_new_upcoming
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.loading) {
                        Text(stringResource(Res.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val form = state.form
        val errors = state.errors
        val currency = state.settings.currency
        val required = stringResource(Res.string.error_required)
        val invalidAmount = stringResource(Res.string.error_invalid_amount)
        val invalidNumber = stringResource(Res.string.error_invalid_number)

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionTitle(stringResource(Res.string.job_section_client))
            DropdownInput(
                label = stringResource(Res.string.job_field_client),
                options = state.clients,
                selected = state.clients.firstOrNull { it.id == form.clientId },
                optionLabel = { it.name },
                onSelect = viewModel::selectClient,
                placeholder = if (state.clients.isEmpty()) stringResource(Res.string.job_no_clients) else "",
                error = required.takeIf { JobFormError.CLIENT_REQUIRED in errors },
                extraItem = stringResource(Res.string.job_field_new_client) to { showNewClient = true },
            )

            SectionTitle(stringResource(Res.string.job_section_details))
            TextInput(
                value = form.title,
                onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                label = stringResource(Res.string.job_field_title),
                placeholder = stringResource(Res.string.job_field_title_hint),
                error = required.takeIf { JobFormError.TITLE_REQUIRED in errors },
            )
            TextInput(
                value = form.role,
                onValueChange = { v -> viewModel.update { it.copy(role = v) } },
                label = stringResource(Res.string.job_field_role),
                placeholder = stringResource(Res.string.job_field_role_hint),
            )
            TextInput(
                value = form.location,
                onValueChange = { v -> viewModel.update { it.copy(location = v) } },
                label = stringResource(Res.string.job_field_location),
                placeholder = stringResource(Res.string.job_field_place_hint),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = form.unitBase,
                onValueChange = { v -> viewModel.update { it.copy(unitBase = v) } },
                label = stringResource(Res.string.job_field_unit_base),
                placeholder = stringResource(Res.string.job_field_place_hint),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = form.parking,
                onValueChange = { v -> viewModel.update { it.copy(parking = v) } },
                label = stringResource(Res.string.job_field_parking),
                placeholder = stringResource(Res.string.job_field_place_hint),
            )

            SectionTitle(stringResource(Res.string.job_section_when))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateInput(
                    date = form.startDate,
                    onDateChange = { d ->
                        viewModel.update { it.copy(startDate = d, endDate = if (it.endDate < d) d else it.endDate) }
                    },
                    label = stringResource(Res.string.job_field_start),
                    modifier = Modifier.weight(1f),
                )
                DateInput(
                    date = form.endDate,
                    onDateChange = { d -> viewModel.update { it.copy(endDate = d) } },
                    label = stringResource(Res.string.job_field_end),
                    error = stringResource(Res.string.error_end_before_start).takeIf { JobFormError.END_BEFORE_START in errors },
                    modifier = Modifier.weight(1f),
                )
            }
            TimeInput(
                time = form.callTime,
                onTimeChange = { t -> viewModel.update { it.copy(callTime = t) } },
                label = stringResource(Res.string.job_field_call_time),
            )

            SectionTitle(stringResource(Res.string.job_section_rate))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RateType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = form.rateType == type,
                        onClick = { viewModel.update { it.copy(rateType = type) } },
                        shape = SegmentedButtonDefaults.itemShape(index, RateType.entries.size),
                        label = { Text(stringResource(type.labelRes()), maxLines = 1) },
                    )
                }
            }
            MoneyInput(
                value = form.rate,
                onValueChange = { v -> viewModel.update { it.copy(rate = v, rateEdited = true) } },
                label = stringResource(form.rateType.fieldLabelRes()),
                currency = currency,
                error = invalidAmount.takeIf { JobFormError.RATE_INVALID in errors },
            )
            if (form.rateType != RateType.FIXED) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DecimalInput(
                        value = form.dayLengthHours,
                        onValueChange = { v -> viewModel.update { it.copy(dayLengthHours = v) } },
                        label = stringResource(Res.string.job_field_day_length),
                        error = invalidNumber.takeIf { JobFormError.DAY_LENGTH_INVALID in errors },
                        modifier = Modifier.weight(1f),
                    )
                    if (form.rateType == RateType.DAY) {
                        DecimalInput(
                            value = form.otMultiplier,
                            onValueChange = { v -> viewModel.update { it.copy(otMultiplier = v) } },
                            label = stringResource(Res.string.job_field_ot_multiplier),
                            error = invalidNumber.takeIf { JobFormError.OT_INVALID in errors },
                            suffix = "×",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (state.isNew && state.finished) {
                SectionTitle(stringResource(Res.string.job_section_hours))
                DecimalInput(
                    value = form.hoursPerDay,
                    onValueChange = { v -> viewModel.update { it.copy(hoursPerDay = v) } },
                    label = stringResource(Res.string.job_field_hours_per_day),
                    supportingText = stringResource(Res.string.job_field_hours_per_day_hint),
                    error = invalidNumber.takeIf { JobFormError.HOURS_INVALID in errors },
                )
            }

            SectionTitle(stringResource(Res.string.job_section_extras))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyInput(
                    value = form.kitRental,
                    onValueChange = { v -> viewModel.update { it.copy(kitRental = v) } },
                    label = stringResource(Res.string.job_field_kit_rental),
                    currency = currency,
                    error = invalidAmount.takeIf { JobFormError.KIT_INVALID in errors },
                    modifier = Modifier.weight(1f),
                )
                MoneyInput(
                    value = form.expenses,
                    onValueChange = { v -> viewModel.update { it.copy(expenses = v) } },
                    label = stringResource(Res.string.job_field_expenses),
                    currency = currency,
                    error = invalidAmount.takeIf { JobFormError.EXPENSES_INVALID in errors },
                    modifier = Modifier.weight(1f),
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(Res.string.job_field_vat, vatPercent(InvoiceRules.UAE_VAT_RATE))) },
                supportingContent = { Text(stringResource(Res.string.job_field_vat_desc)) },
                trailingContent = {
                    Switch(checked = form.addVat, onCheckedChange = { v -> viewModel.update { it.copy(addVat = v) } })
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            SectionTitle(stringResource(Res.string.job_section_payment))
            DateInput(
                date = state.dueDate,
                onDateChange = { d -> viewModel.update { it.copy(dueDateOverride = d) } },
                label = stringResource(Res.string.job_field_due_date),
                displayText = if (form.dueDateOverride == null) {
                    stringResource(Res.string.job_field_due_auto, state.autoDueDate.formatMedium(), state.paymentTermsDays)
                } else {
                    state.dueDate.formatMedium()
                },
            )
            if (form.dueDateOverride != null) {
                TextButton(onClick = { viewModel.update { it.copy(dueDateOverride = null) } }) {
                    Text(stringResource(Res.string.job_reset_due))
                }
            }
            TextInput(
                value = form.notes,
                onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                label = stringResource(Res.string.job_field_notes),
                singleLine = false,
                minLines = 3,
            )

            state.estimate?.let { estimate ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.job_estimate),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = money(estimate.total, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(
                                Res.string.job_estimate_detail,
                                money(estimate.base, currency),
                                money(estimate.overtime, currency),
                                money(estimate.kitRental + estimate.expenses, currency),
                            ) + if (estimate.vat > 0) stringResource(Res.string.job_estimate_vat, money(estimate.vat, currency)) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNewClient) {
        NewClientDialog(
            onDismiss = { showNewClient = false },
            onCreate = { name, phone ->
                viewModel.createClient(name, phone)
                showNewClient = false
            },
        )
    }
}

@Composable
private fun NewClientDialog(onDismiss: () -> Unit, onCreate: (name: String, phone: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.job_new_client_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.client_field_name),
                    capitalization = KeyboardCapitalization.Words,
                )
                TextInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(Res.string.client_field_phone),
                    keyboardType = KeyboardType.Phone,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, phone) }, enabled = name.isNotBlank()) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
