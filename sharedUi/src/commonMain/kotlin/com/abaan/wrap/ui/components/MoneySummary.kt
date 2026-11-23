package com.abaan.wrap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abaan.wrap.shared.money.MoneyFormat
import com.abaan.wrap.ui.theme.LocalStatusColors

/** Wrap's logo mark: an open ring with a green dot closing the gap at the top. */
@Composable
fun WrapMark(
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.onSurface,
    dotColor: Color = LocalStatusColors.current.success,
) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.17f
        val radius = (size.minDimension - stroke) / 2f
        drawArc(
            color = ringColor,
            startAngle = -58.5f,
            sweepAngle = 297f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(dotColor, radius = stroke * 0.62f, center = Offset(center.x, center.y - radius))
    }
}

/** A small coloured dot before a label, the way Wrap marks paid (green), owed (amber) and late (red). */
@Composable
fun DotLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A thin bar split into two rounded parts, sized by the two amounts. Empty when both are zero. */
@Composable
fun SplitBar(
    first: Long,
    second: Long,
    firstColor: Color,
    secondColor: Color,
    modifier: Modifier = Modifier,
) {
    val total = first + second
    val track = MaterialTheme.colorScheme.outlineVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth().height(8.dp),
    ) {
        if (total <= 0L) {
            Box(Modifier.fillMaxWidth().fillMaxHeight().clip(CircleShape).background(track))
        } else {
            val share = first.toFloat() / total
            if (first > 0L) {
                Box(Modifier.weight(share).fillMaxHeight().clip(CircleShape).background(firstColor))
            }
            if (second > 0L) {
                Box(Modifier.weight(1f - share).fillMaxHeight().clip(CircleShape).background(secondColor))
            }
        }
    }
}

/** The card Wrap is built around: what you've been paid next to what you're still owed. */
@Composable
fun PaidOwedCard(
    paidLabel: String,
    paid: Long,
    owedLabel: String,
    owed: Long,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val status = LocalStatusColors.current
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val content: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AmountColumn(paidLabel, paid, currency, status.success, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                AmountColumn(owedLabel, owed, currency, status.warning, status.warning, Modifier.weight(1f))
            }
            SplitBar(paid, owed, status.success, status.warning)
            footer()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, colors = colors, border = border, modifier = modifier.fillMaxWidth(), content = content)
    } else {
        Card(colors = colors, border = border, modifier = modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun AmountColumn(
    label: String,
    amount: Long,
    currency: String,
    dot: Color,
    amountColor: Color,
    modifier: Modifier = Modifier,
) {
    val figure = MoneyFormat.formatAmount(amount)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DotLabel(label, dot)
        Spacer(Modifier.height(2.dp))
        Text(
            text = figure,
            style = if (figure.length > 10) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            color = amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(currency, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
