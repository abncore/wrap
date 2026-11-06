package com.abaan.wrap.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abaan.wrap.R
import com.abaan.wrap.ui.navigation.TopLevelDestination

@Composable
fun ClientsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = TopLevelDestination.CLIENTS.icon,
        titleRes = R.string.clients_empty_title,
        bodyRes = R.string.clients_empty_body,
        modifier = modifier,
    )
}
