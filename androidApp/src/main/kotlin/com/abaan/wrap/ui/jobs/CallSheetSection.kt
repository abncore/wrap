package com.abaan.wrap.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abaan.wrap.R
import com.abaan.wrap.shared.chase.PhoneNumbers
import com.abaan.wrap.shared.model.CrewContact
import com.abaan.wrap.shared.model.Job
import com.abaan.wrap.ui.components.ConfirmDialog
import com.abaan.wrap.ui.components.TextInput

/** Unit base, parking and crew contacts from the job's call sheet. */
@Composable
fun CallSheetSection(
    job: Job,
    crew: List<CrewContact>,
    onAddCrew: (name: String, role: String, phone: String) -> Unit,
    onDeleteCrew: (Long) -> Unit,
    onEditJob: () -> Unit,
) {
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    DetailSection(
        title = stringResource(R.string.call_sheet_title),
        action = {
            TextButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.crew_add))
            }
        },
    ) {
        if (job.unitBase.isBlank() && job.parking.isBlank() && crew.isEmpty()) {
            Text(
                text = stringResource(R.string.call_sheet_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onEditJob) { Text(stringResource(R.string.call_sheet_add_places)) }
        }
        listOf(
            R.string.call_sheet_unit_base to job.unitBase,
            R.string.call_sheet_parking to job.parking,
        ).filter { (_, place) -> place.isNotBlank() }.forEach { (label, place) ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                overlineContent = { Text(stringResource(label)) },
                headlineContent = {
                    Text(
                        if (isLink(place)) stringResource(R.string.call_sheet_map_link) else place,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    IconButton(onClick = { openMaps(context, place) }) {
                        Icon(painterResource(R.drawable.ic_directions), contentDescription = stringResource(R.string.action_directions))
                    }
                },
                colors = transparent,
            )
        }
        crew.forEach { contact ->
            ListItem(
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                headlineContent = { Text(contact.name) },
                supportingContent = contact.role.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                trailingContent = {
                    Row {
                        if (contact.phone.isNotBlank()) {
                            IconButton(onClick = { dial(context, contact.phone) }) {
                                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.crew_call, contact.name))
                            }
                            if (PhoneNumbers.toWhatsAppDigits(contact.phone) != null) {
                                IconButton(onClick = { openWhatsApp(context, contact.phone) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.crew_whatsapp, contact.name),
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { deleting = contact.id }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.crew_delete, contact.name))
                        }
                    }
                },
                colors = transparent,
            )
        }
    }

    if (adding) {
        CrewDialog(
            onDismiss = { adding = false },
            onSave = { name, role, phone ->
                onAddCrew(name, role, phone)
                adding = false
            },
        )
    }
    deleting?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.crew_delete_title),
            body = null,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleting = null
                onDeleteCrew(id)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CrewDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crew_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = stringResource(R.string.crew_name),
                    capitalization = KeyboardCapitalization.Words,
                    error = stringResource(R.string.error_required).takeIf { showError },
                )
                TextInput(
                    value = role,
                    onValueChange = { role = it },
                    label = stringResource(R.string.crew_role),
                    placeholder = stringResource(R.string.crew_role_hint),
                )
                TextInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.crew_phone),
                    keyboardType = KeyboardType.Phone,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isBlank()) showError = true else onSave(name, role, phone) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
