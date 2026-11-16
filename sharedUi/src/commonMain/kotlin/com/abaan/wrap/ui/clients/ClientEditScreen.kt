package com.abaan.wrap.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.DecimalInput
import com.abaan.wrap.ui.components.MoneyInput
import com.abaan.wrap.ui.components.TextInput
import com.abaan.wrap.ui.components.money
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    clientId: Long,
    onClose: () -> Unit,
    viewModel: ClientEditViewModel = koinViewModel(key = "client-$clientId") { parametersOf(clientId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) Res.string.client_new else Res.string.client_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    }
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
        val required = stringResource(Res.string.error_required)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextInput(
                value = form.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = stringResource(Res.string.client_field_name),
                error = required.takeIf { ClientFormError.NAME_REQUIRED in state.errors },
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = form.company,
                onValueChange = { v -> viewModel.update { it.copy(company = v) } },
                label = stringResource(Res.string.client_field_company),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = form.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = stringResource(Res.string.client_field_phone),
                keyboardType = KeyboardType.Phone,
            )
            TextInput(
                value = form.email,
                onValueChange = { v -> viewModel.update { it.copy(email = v) } },
                label = stringResource(Res.string.client_field_email),
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
            )
            DecimalInput(
                value = form.paymentTerms,
                onValueChange = { v -> viewModel.update { it.copy(paymentTerms = v) } },
                label = stringResource(Res.string.client_field_terms),
                supportingText = stringResource(Res.string.client_field_terms_hint, state.settings.paymentTermsDays),
                error = stringResource(Res.string.error_invalid_number).takeIf { ClientFormError.TERMS_INVALID in state.errors },
                integerOnly = true,
            )
            MoneyInput(
                value = form.defaultRate,
                onValueChange = { v -> viewModel.update { it.copy(defaultRate = v) } },
                label = stringResource(Res.string.client_field_rate),
                currency = state.settings.currency,
                supportingText = state.settings.defaultRate.takeIf { it > 0 }?.let {
                    stringResource(Res.string.client_field_rate_hint, money(it, state.settings.currency))
                },
                error = stringResource(Res.string.error_invalid_amount).takeIf { ClientFormError.RATE_INVALID in state.errors },
            )
            TextInput(
                value = form.notes,
                onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                label = stringResource(Res.string.client_field_notes),
                singleLine = false,
                minLines = 3,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.client_delete_title),
            body = stringResource(Res.string.client_delete_body),
            confirmLabel = stringResource(Res.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (state.deleteBlocked) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlocked,
            text = { Text(stringResource(Res.string.client_delete_blocked)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlocked) { Text(stringResource(Res.string.action_ok)) }
            },
        )
    }
}
