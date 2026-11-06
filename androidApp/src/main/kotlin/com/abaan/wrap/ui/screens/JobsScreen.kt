package com.abaan.wrap.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abaan.wrap.R
import com.abaan.wrap.ui.navigation.TopLevelDestination

@Composable
fun JobsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = TopLevelDestination.JOBS.icon,
        titleRes = R.string.jobs_empty_title,
        bodyRes = R.string.jobs_empty_body,
        modifier = modifier,
    )
}
