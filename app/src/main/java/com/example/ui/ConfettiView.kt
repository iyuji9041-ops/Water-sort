package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.WaterColor
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val rotationSpeed: Float,
    val wobbleFrequency: Float,
    val delayFraction: Float
)

/**
 * Fullscreen celebratory confetti animation triggered upon solving a level.
 */
@Composable
fun ConfettiView(
    isWin: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isWin) return

    val particles = remember {
        val colors = WaterColor.ALL_COLORS.map { it.color }
        val rnd = Random(System.currentTimeMillis())
        List(85) {
            ConfettiParticle(
                startXFraction = rnd.nextFloat(),
                speed = rnd.nextFloat() * 0.4f + 0.8f,
                width = rnd.nextFloat() * 8f + 8f,
                height = rnd.nextFloat() * 12f + 14f,
                color = colors[rnd.nextInt(colors.size)],
                rotationSpeed = (rnd.nextFloat() * 720f - 360f),
                wobbleFrequency = rnd.nextFloat() * 8f + 4f,
                delayFraction = rnd.nextFloat() * 0.35f
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isWin) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
    }

    val currentProgress = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { p ->
            val adjustedProgress = ((currentProgress - p.delayFraction) / (1f - p.delayFraction))
                .coerceIn(0f, 1f)

            if (adjustedProgress > 0f) {
                val y = adjustedProgress * p.speed * (canvasHeight + 100f) - 50f
                val wobble = kotlin.math.sin(adjustedProgress * p.wobbleFrequency * Math.PI.toFloat()) * 25f
                val x = (p.startXFraction * canvasWidth) + wobble
                val rotation = adjustedProgress * p.rotationSpeed
                val alpha = (1f - adjustedProgress * 0.7f).coerceIn(0f, 1f)

                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - p.width / 2, y - p.height / 2),
                        size = Size(p.width, p.height)
                    )
                }
            }
        }
    }
}
