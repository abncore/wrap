package com.abaan.wrap.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abaan.wrap.R
import com.abaan.wrap.ui.navigation.TopLevelDestination

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = TopLevelDestination.HOME.icon,
        titleRes = R.string.home_empty_title,
        bodyRes = R.string.home_empty_body,
        modifier = modifier,
    )
}
