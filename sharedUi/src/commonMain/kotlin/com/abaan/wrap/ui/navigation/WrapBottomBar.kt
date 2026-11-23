package com.abaan.wrap.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abaan.wrap.ui.platform.LocalWrapStyle
import com.abaan.wrap.ui.platform.WrapStyle
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
import org.jetbrains.compose.resources.stringResource
import com.abaan.wrap.ui.resources.*

/**
 * Bottom navigation: Home and Jobs, the + button, then the remaining tabs.
 */
@Composable
fun WrapBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
) {
    if (LocalWrapStyle.current == WrapStyle.IOS) {
        IosTabBar(current, onSelect, onAddClick)
        return
    }
    val destinations = TopLevelDestination.entries
    val half = TABS_BEFORE_ADD

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
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.nav_add))
            }
        }

        destinations.drop(half).forEach { destination ->
            TabItem(destination, selected = destination == current, onSelect = onSelect)
        }
    }
}

/** How many tabs sit before the + button. */
private const val TABS_BEFORE_ADD = 2

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
        icon = { Icon(destination.icon(), contentDescription = null) },
        label = { Text(label) },
    )
}

/** The iOS tab bar: a hairline on top, tinted icon and small label for the open tab, no pill. */
@Composable
private fun IosTabBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val destinations = TopLevelDestination.entries
    Column(Modifier.fillMaxWidth().background(colors.surfaceContainerLow)) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(50.dp),
        ) {
            destinations.take(TABS_BEFORE_ADD).forEach { IosTab(it, selected = it == current, onSelect = onSelect) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(role = Role.Button, onClick = onAddClick),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.nav_add),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            destinations.drop(TABS_BEFORE_ADD).forEach { IosTab(it, selected = it == current, onSelect = onSelect) }
        }
    }
}

@Composable
private fun RowScope.IosTab(
    destination: TopLevelDestination,
    selected: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(destination) },
            ),
    ) {
        Icon(destination.icon(), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(stringResource(destination.labelRes), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tint, maxLines = 1)
    }
}
