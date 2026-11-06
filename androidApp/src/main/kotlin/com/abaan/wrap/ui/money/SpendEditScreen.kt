package com.abaan.wrap.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.R
import com.abaan.wrap.shared.model.SpendCategory
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.DateInput
import com.abaan.wrap.ui.components.DropdownInput
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.SectionTitle
import com.abaan.wrap.ui.components.TextInput
import com.abaan.wrap.ui.components.formatMedium
import com.abaan.wrap.ui.components.labelRes
import com.abaan.wrap.ui.theme.color
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendEditScreen(
    spendId: Long,
    onClose: () -> Unit,
    viewModel: SpendEditViewModel = koinViewModel(key = "spend-$spendId") { parametersOf(spendId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.spend_new else R.string.spend_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.loading) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MoneyInput(
                value = state.amount,
                onValueChange = viewModel::setAmount,
                label = stringResource(R.string.spend_field_amount),
                currency = state.currency,
                error = stringResource(R.string.error_amount_positive).takeIf { SpendFormError.AMOUNT_INVALID in state.errors },
            )
            DateInput(
                date = state.date,
                onDateChange = viewModel::setDate,
                label = stringResource(R.string.spend_field_date),
            )

            SectionTitle(stringResource(R.string.spend_field_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpendCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(stringResource(category.labelRes())) },
                        leadingIcon = {
                            Box(Modifier.size(10.dp).background(category.color(), CircleShape))
                        },
                    )
                }
            }

            TextInput(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = stringResource(R.string.spend_field_note),
                placeholder = stringResource(R.string.spend_field_note_hint),
                modifier = Modifier.padding(top = 8.dp),
            )

            val jobOptions = listOf<Long?>(null) + state.jobs.map { it.job.id }
            val jobsById = state.jobs.associateBy { it.job.id }
            DropdownInput(
                label = stringResource(R.string.spend_bill_to),
                options = jobOptions,
                selected = state.jobId,
                optionLabel = { id ->
                    jobsById[id]?.let { stringResource(R.string.job_option, it.job.title, it.job.startDate.formatMedium()) }
                        ?: stringResource(R.string.spend_bill_none)
                },
                onSelect = viewModel::setJob,
                placeholder = stringResource(R.string.spend_bill_none),
                supportingText = stringResource(R.string.spend_bill_desc),
                modifier = Modifier.padding(top = 8.dp),
            )

            SectionTitle(stringResource(R.string.receipt_title))
            ReceiptSection(
                receipt = state.receipt,
                importing = state.importingReceipt,
                error = stringResource(R.string.receipt_unreadable).takeIf { SpendFormError.RECEIPT_UNREADABLE in state.errors },
                cameraUri = viewModel::cameraUri,
                onPicked = viewModel::attachReceipt,
                onRemove = viewModel::removeReceipt,
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.spend_repeat)) },
                supportingContent = { Text(stringResource(R.string.spend_repeat_desc, state.date.day)) },
                trailingContent = {
                    Switch(checked = state.repeatMonthly, onCheckedChange = viewModel::setRepeatMonthly)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.spend_delete_title),
            body = null,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
