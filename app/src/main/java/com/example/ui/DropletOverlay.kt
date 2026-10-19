package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.FlyingDroplet

/**
 * Renders an animated luminous droplet soaring in an arc from the pouring
 * tube to the target tube during a transfer move.
 * Kyun: User requirement B.4 — Droplet pouring tube ke mouth se shuru hota hai
 * aur 30dp lift offset ko accurately factor karta hai.
 */
@Composable
fun DropletOverlay(
    activeDroplet: FlyingDroplet?,
    tubePositions: Map<Int, Offset>,
    modifier: Modifier = Modifier
) {
    if (activeDroplet == null) return

    val startPos = tubePositions[activeDroplet.fromTubeIndex]
    val endPos = tubePositions[activeDroplet.toTubeIndex]

    if (startPos == null || endPos == null) return

    val density = LocalDensity.current
    // Kyun: User requirement B.4 — Pouring tube 30dp lift hoti hai, isliye start Y me -30dp add kiya
    val liftOffset = with(density) { 30.dp.toPx() }
    val startY = startPos.y - liftOffset
    val endY = endPos.y

    val progress = remember(activeDroplet.id) { Animatable(0f) }

    LaunchedEffect(activeDroplet.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )
    }

    val p = progress.value
    // Parabolic arc trajectory from mouth of pouring tube to target tube
    val currentX = startPos.x + (endPos.x - startPos.x) * p
    val arcHeight = with(density) { 32.dp.toPx() }
    val currentY = startY + (endY - startY) * p - (arcHeight * 4f * p * (1f - p))
    val radius = with(density) { 5.dp.toPx() } * (1.1f - 0.45f * p)
    val alpha = (1f - 0.3f * p).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(currentX, currentY)
        val dropletColor = activeDroplet.color.color.copy(alpha = alpha)

        // Outer glow
        drawCircle(
            color = dropletColor.copy(alpha = 0.35f * alpha),
            radius = radius * 2.2f,
            center = center
        )

        // Core droplet
        drawCircle(
            color = dropletColor,
            radius = radius,
            center = center
        )

        // Specular dot
        drawCircle(
            color = Color.White.copy(alpha = 0.8f * alpha),
            radius = radius * 0.35f,
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f)
        )
    }
}
