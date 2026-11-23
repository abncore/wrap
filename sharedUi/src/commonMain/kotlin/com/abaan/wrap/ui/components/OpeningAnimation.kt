package com.abaan.wrap.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.abaan.wrap.ui.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Plays when the app opens, on top of the first screen: the ring draws itself, the green dot
 * pops into the gap, the name slides in, then everything fades away to the app underneath.
 */
@Composable
fun OpeningAnimation(onFinished: () -> Unit) {
    val ring = remember { Animatable(0f) }
    val dot = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // The first frames are busy building the screen underneath; start once they're drawn so
        // the ring doesn't skip its first half.
        repeat(3) { withFrameNanos { } }
        delay(80)
        launch { ring.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing)) }
        delay(520)
        launch { dot.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
        delay(160)
        name.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        delay(320)
        fade.animateTo(0f, tween(durationMillis = 300))
        onFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                val grow = 1f + (1f - fade.value) * 0.08f
                scaleX = grow
                scaleY = grow
            },
        ) {
            WrapMark(Modifier.size(52.dp), ring = ring.value, dot = dot.value)
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(Res.string.app_name).lowercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = name.value
                    translationX = (1f - name.value) * -24.dp.toPx()
                },
            )
        }
    }
}
