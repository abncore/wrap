package com.abaan.wrap.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.shared.money.InvoiceRules
import com.abaan.wrap.ui.components.DecimalInput
import com.abaan.wrap.ui.components.SectionTitle
import com.abaan.wrap.ui.components.TextInput
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel

/** The freelancer's own details for invoices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onClose: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)
    val profile = state.profile

    LaunchedEffect(state.done) { if (state.done) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
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
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.profile_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionTitle(stringResource(Res.string.profile_section_you))
            TextInput(
                value = profile.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = stringResource(Res.string.profile_name),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = profile.businessName,
                onValueChange = { v -> viewModel.update { it.copy(businessName = v) } },
                label = stringResource(Res.string.profile_business),
                supportingText = stringResource(Res.string.profile_business_hint),
                capitalization = KeyboardCapitalization.Words,
            )
            TextInput(
                value = profile.address,
                onValueChange = { v -> viewModel.update { it.copy(address = v) } },
                label = stringResource(Res.string.profile_address),
                singleLine = false,
                minLines = 2,
            )
            TextInput(
                value = profile.phone,
                onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                label = stringResource(Res.string.profile_phone),
                keyboardType = KeyboardType.Phone,
            )
            TextInput(
                value = profile.email,
                onValueChange = { v -> viewModel.update { it.copy(email = v) } },
                label = stringResource(Res.string.profile_email),
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
            )

            SectionTitle(stringResource(Res.string.profile_section_payment))
            TextInput(
                value = profile.bankDetails,
                onValueChange = { v -> viewModel.update { it.copy(bankDetails = v) } },
                label = stringResource(Res.string.profile_bank),
                placeholder = stringResource(Res.string.profile_bank_hint),
                singleLine = false,
                minLines = 3,
            )

            SectionTitle(stringResource(Res.string.profile_section_vat))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.profile_vat)) },
                supportingContent = { Text(stringResource(Res.string.profile_vat_desc, vatPercent(InvoiceRules.UAE_VAT_RATE))) },
                trailingContent = {
                    Switch(
                        checked = profile.vatRegistered,
                        onCheckedChange = { checked -> viewModel.update { it.copy(vatRegistered = checked) } },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { viewModel.update { it.copy(vatRegistered = !it.vatRegistered) } },
            )
            if (profile.vatRegistered || profile.trn.isNotBlank()) {
                TextInput(
                    value = profile.trn,
                    onValueChange = { v -> viewModel.update { it.copy(trn = v) } },
                    label = stringResource(Res.string.profile_trn),
                    keyboardType = KeyboardType.Number,
                )
            }

            SectionTitle(stringResource(Res.string.profile_section_numbering))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(
                    value = profile.invoicePrefix,
                    onValueChange = { v -> viewModel.update { it.copy(invoicePrefix = v) } },
                    label = stringResource(Res.string.profile_prefix),
                    capitalization = KeyboardCapitalization.Characters,
                    modifier = Modifier.weight(1f),
                )
                DecimalInput(
                    value = state.nextNumber,
                    onValueChange = viewModel::setNextNumber,
                    label = stringResource(Res.string.profile_next_number),
                    integerOnly = true,
                    error = stringResource(Res.string.error_invalid_number).takeIf { state.nextNumberInvalid },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(
                    Res.string.profile_next_preview,
                    InvoiceRules.formatNumber(profile.invoicePrefix.trim(), state.nextNumber.toLongOrNull() ?: 1),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 0.05 -> "5". */
fun vatPercent(rate: Double): String = (rate * 100).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
