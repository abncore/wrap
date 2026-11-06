package com.abaan.wrap.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abaan.wrap.R
import com.abaan.wrap.ui.navigation.TopLevelDestination

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = TopLevelDestination.SETTINGS.icon,
        titleRes = R.string.settings_empty_title,
        bodyRes = R.string.settings_empty_body,
        modifier = modifier,
    )
}
