package com.abaan.wrap.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import androidx.compose.ui.unit.dp
import com.abaan.wrap.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import kotlinx.coroutines.launch

private data class AddOption(
    val action: AddAction,
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: @Composable () -> ImageVector,
)

private val addOptions = listOf(
    AddOption(AddAction.UPCOMING_JOB, Res.string.add_upcoming_job, Res.string.add_upcoming_job_desc) { Icons.Filled.DateRange },
    AddOption(AddAction.FINISHED_JOB, Res.string.add_finished_job, Res.string.add_finished_job_desc) { Icons.Filled.CheckCircle },
    AddOption(AddAction.PAYMENT, Res.string.add_payment, Res.string.add_payment_desc) { vectorResource(Res.drawable.ic_payments) },
    AddOption(AddAction.SPEND, Res.string.add_spend, Res.string.add_spend_desc) { Icons.Filled.ShoppingCart },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    onAction: (AddAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(Res.string.add_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            addOptions.forEach { option ->
                ListItem(
                    headlineContent = { Text(stringResource(option.titleRes)) },
                    supportingContent = { Text(stringResource(option.descriptionRes)) },
                    leadingContent = { Icon(option.icon(), contentDescription = null) },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            onAction(option.action)
                        }
                    },
                )
            }
        }
    }
}
