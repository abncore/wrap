package com.abaan.wrap.ui.payments

import com.abaan.wrap.ui.components.ChoiceRow
import com.abaan.wrap.ui.components.BackButton
import com.abaan.wrap.ui.components.WrapTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.model.PaymentMethod
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.DateInput
import com.abaan.wrap.ui.components.DropdownInput
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.SectionTitle
import com.abaan.wrap.ui.components.TextInput
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.components.money
import com.abaan.wrap.ui.screens.EmptyState
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEditScreen(
    jobId: Long,
    paymentId: Long,
    onClose: () -> Unit,
    viewModel: PaymentEditViewModel = koinViewModel(key = "payment-$jobId-$paymentId") { parametersOf(jobId, paymentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onClose() }

    Scaffold(
        topBar = {
            WrapTopBar(
                title = { Text(stringResource(if (state.isNew) Res.string.payment_new else Res.string.payment_edit)) },
                navigationIcon = {
                    BackButton(onClick = onClose)
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.loading && state.jobs.isNotEmpty()) {
                        Text(stringResource(Res.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding))
            state.jobs.isEmpty() -> EmptyState(
                icon = vectorResource(Res.drawable.ic_payments),
                title = stringResource(Res.string.payment_new),
                body = stringResource(Res.string.payment_no_jobs),
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                SectionTitle(stringResource(Res.string.payment_field_job))
                if (state.jobLocked) {
                    val summary = state.jobs.firstOrNull { it.job.id == state.jobId }
                    Text(
                        text = summary?.let { s -> listOfNotNull(s.job.title, s.client?.name).joinToString(" · ") }.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    DropdownInput(
                        label = stringResource(Res.string.payment_field_job),
                        options = state.jobs,
                        selected = state.jobs.firstOrNull { it.job.id == state.jobId },
                        optionLabel = { s -> listOfNotNull(s.job.title, s.client?.name).joinToString(" · ") },
                        onSelect = { viewModel.selectJob(it.job.id) },
                        placeholder = stringResource(Res.string.payment_select_job),
                        error = stringResource(Res.string.error_required).takeIf { PaymentFormError.JOB_REQUIRED in state.errors },
                    )
                }

                SectionTitle(stringResource(Res.string.payment_field_amount))
                MoneyInput(
                    value = state.amount,
                    onValueChange = viewModel::setAmount,
                    label = stringResource(Res.string.payment_field_amount),
                    currency = state.currency,
                    supportingText = state.owed?.let { stringResource(Res.string.payment_owed_hint, money(it, state.currency)) },
                    error = stringResource(Res.string.error_amount_positive).takeIf { PaymentFormError.AMOUNT_INVALID in state.errors },
                )
                if (state.owed != null && state.owed!! > 0) {
                    AssistChip(
                        onClick = viewModel::fillOwed,
                        label = { Text(stringResource(Res.string.payment_full_amount)) },
                    )
                }
                DateInput(
                    date = state.date,
                    onDateChange = viewModel::setDate,
                    label = stringResource(Res.string.payment_field_date),
                )

                SectionTitle(stringResource(Res.string.payment_field_method))
                ChoiceRow(
                    labels = PaymentMethod.entries.map { stringResource(it.labelRes()) },
                    selected = state.method.ordinal,
                    onSelect = { viewModel.setMethod(PaymentMethod.entries[it]) },
                )
                TextInput(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = stringResource(Res.string.payment_field_note),
                    placeholder = stringResource(Res.string.payment_field_note_hint),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.job_delete_payment_title),
            body = null,
            confirmLabel = stringResource(Res.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
