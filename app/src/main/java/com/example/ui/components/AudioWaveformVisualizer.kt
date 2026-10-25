package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    amplitudes: List<Float>,
    progress: Float, // 0.0f .. 1.0f
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    // Subtle bounce animation when audio is actively playing
    val animFactor = remember { Animatable(1.0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animFactor.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            animFactor.snapTo(1.0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("waveform_visualizer")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val count = amplitudes.size.coerceAtLeast(1)
            val barSpacing = 3.dp.toPx()
            val totalSpacing = barSpacing * (count - 1)
            val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())
            val centerY = size.height / 2f
            val maxBarHeight = size.height * 0.85f

            for (i in 0 until count) {
                val x = i * (barWidth + barSpacing)
                val baseAmp = amplitudes[i].coerceIn(0.1f, 1.0f)
                // When playing, apply dynamic bounce
                val bounce = if (isPlaying) {
                    val phase = ((i % 5) * 0.15f)
                    (baseAmp * (animFactor.value + phase)).coerceIn(0.15f, 1.0f)
                } else {
                    baseAmp
                }

                val barHeight = (maxBarHeight * bounce).coerceAtLeast(4.dp.toPx())
                val top = centerY - barHeight / 2f
                val barFraction = i.toFloat() / count.toFloat()

                val barColor = if (barFraction <= progress) {
                    if (barFraction >= progress - 0.08f) accentColor else activeColor
                } else {
                    inactiveColor
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
