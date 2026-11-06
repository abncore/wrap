package com.abaan.wrap.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.abaan.wrap.R

@Composable
fun HomeScreen(onAddJob: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.Home,
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body),
        actionLabel = stringResource(R.string.home_empty_action),
        onAction = onAddJob,
        modifier = modifier,
    )
}
