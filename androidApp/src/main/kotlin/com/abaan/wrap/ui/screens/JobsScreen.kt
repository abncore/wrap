package com.abaan.wrap.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.abaan.wrap.R

@Composable
fun JobsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.AutoMirrored.Filled.List,
        title = stringResource(R.string.jobs_title),
        body = stringResource(R.string.jobs_empty_upcoming),
        modifier = modifier,
    )
}
