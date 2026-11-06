package com.abaan.wrap.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.abaan.wrap.R

/**
 * Bottom navigation with the four tabs split two-and-two around a central + button.
 */
@Composable
fun WrapBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
) {
    val destinations = TopLevelDestination.entries
    val half = destinations.size / 2

    NavigationBar {
        destinations.take(half).forEach { destination ->
            TabItem(destination, selected = destination == current, onSelect = onSelect)
        }

        // NavigationBar only sets a minimum height, so the slot must not fill the
        // available height or the bar stretches to the whole screen.
        Box(
            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center,
        ) {
            SmallFloatingActionButton(
                onClick = onAddClick,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.nav_add))
            }
        }

        destinations.drop(half).forEach { destination ->
            TabItem(destination, selected = destination == current, onSelect = onSelect)
        }
    }
}

@Composable
private fun RowScope.TabItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val label = stringResource(destination.labelRes)
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(destination) },
        icon = { Icon(destination.icon, contentDescription = null) },
        label = { Text(label) },
    )
}
