package com.abaan.wrap.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abaan.wrap.ui.platform.LocalWrapStyle
import com.abaan.wrap.ui.platform.WrapStyle
import com.abaan.wrap.ui.resources.*
import com.abaan.wrap.ui.theme.LocalStatusColors
import org.jetbrains.compose.resources.stringResource

// Building blocks that look like Material on Android and like Apple's own controls on iOS.
// Screens use these instead of the Material versions so each phone gets the look its owner
// expects, from the same screen code.

@Composable
private fun isIos() = LocalWrapStyle.current == WrapStyle.IOS

@Composable
private fun isDark() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * The screen's top bar. On iOS, screens without a back button get a large title under the
 * actions; the others get a centred title, like a navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapTopBar(
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (!isIos()) {
        TopAppBar(title = title, navigationIcon = navigationIcon ?: {}, actions = actions)
        return
    }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.primary) {
            Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp)) {
                if (navigationIcon != null) {
                    Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                        navigationIcon()
                    }
                    Box(Modifier.align(Alignment.Center).padding(horizontal = 96.dp)) {
                        CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                            ProvideTextStyle(type.titleMedium) { title() }
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
        if (navigationIcon == null) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)) {
                CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                    ProvideTextStyle(type.headlineLarge) { title() }
                }
            }
        }
    }
}

/** Back: an arrow on Android, "‹ Back" in the tint colour on iOS. */
@Composable
fun BackButton(onClick: () -> Unit) {
    val label = stringResource(Res.string.action_back)
    if (!isIos()) {
        IconButton(onClick = onClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = label)
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(32.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.offset(x = (-4).dp))
    }
}

/** An on/off switch: Material's on Android, the green iOS switch on iOS. */
@Composable
fun WrapSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!isIos()) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val on = LocalStatusColors.current.success
    val off = if (isDark()) Color(0xFF39393D) else Color(0xFFE9E9EA)
    val track by animateColorAsState(if (checked) on else off)
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp)
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }
    Box(
        modifier
            .size(width = 51.dp, height = 31.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(track)
            .then(toggle)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = knob)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** Pick one of a few options: Material segmented buttons, or an iOS segmented control. */
@Composable
fun ChoiceRow(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isIos()) {
        IosSegmented(labels, selected, onSelect, modifier)
        return
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

/** A screen's tabs: a Material tab row, or an iOS segmented control under the title. */
@Composable
fun WrapTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    if (isIos()) {
        IosSegmented(labels, selected, onSelect, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        return
    }
    PrimaryTabRow(selectedTabIndex = selected) {
        labels.forEachIndexed { index, label ->
            Tab(selected = index == selected, onClick = { onSelect(index) }, text = { Text(label) })
        }
    }
}

@Composable
private fun IosSegmented(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isDark()
    val trackColor = if (dark) Color(0xFF1C1C1F) else Color(0xFFE9E9EB)
    val thumbColor = if (dark) Color(0xFF5A5A5E) else Color.White
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(trackColor)
            .padding(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val background by animateColorAsState(if (isSelected) thumbColor else Color.Transparent)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(7.dp)) else Modifier)
                    .clip(RoundedCornerShape(7.dp))
                    .background(background)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    ),
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * A text field: outlined on Android; on iOS a filled, rounded field with the label inside and
 * no underline, like the fields in Apple's own forms.
 */
@Composable
fun WrapTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
) {
    if (!isIos()) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = modifier, readOnly = readOnly,
            label = label, placeholder = placeholder, trailingIcon = trailingIcon, prefix = prefix,
            suffix = suffix, supportingText = supportingText, isError = isError,
            keyboardOptions = keyboardOptions, singleLine = singleLine, minLines = minLines,
            interactionSource = interactionSource,
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    TextField(
        value = value, onValueChange = onValueChange, modifier = modifier, readOnly = readOnly,
        label = label, placeholder = placeholder, trailingIcon = trailingIcon, prefix = prefix,
        suffix = suffix, supportingText = supportingText, isError = isError,
        keyboardOptions = keyboardOptions, singleLine = singleLine, minLines = minLines,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainer,
            unfocusedContainerColor = colors.surfaceContainer,
            disabledContainerColor = colors.surfaceContainer,
            errorContainerColor = colors.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}
