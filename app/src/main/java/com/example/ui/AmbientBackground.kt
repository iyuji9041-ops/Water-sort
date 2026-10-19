package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SpaceDeep
import com.example.ui.theme.SpaceElevated
import com.example.ui.theme.SpaceMid
import kotlin.math.max

/**
 * Atmospheric dark background featuring a responsive radial gradient and
 * floating ambient blurred glow orbs.
 * Kyun: User requirement D.18 — Radius aur offsets dp / screen-fraction me dynamically calculate hote hain.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val density = LocalDensity.current

    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1_float"
    )

    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2_float"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxDim = max(width, height)

            // Main ambient radial gradient (25% 8% origin matching HTML/JS)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(SpaceElevated, SpaceMid, SpaceDeep),
                    center = Offset(width * 0.25f, height * 0.08f),
                    radius = maxDim * 1.2f
                )
            )

            val orb1Radius = width * 0.44f
            val orb1DeltaX = with(density) { 24.dp.toPx() } * floatOffset1
            val orb1DeltaY = with(density) { 20.dp.toPx() } * floatOffset1

            // Top-left luminous blue orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x383B6BFF), Color(0x103B6BFF), Color.Transparent),
                    center = Offset(width * 0.15f + orb1DeltaX, height * 0.10f + orb1DeltaY),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(width * 0.15f + orb1DeltaX, height * 0.10f + orb1DeltaY)
            )

            val orb2Radius = width * 0.40f
            val orb2DeltaX = with(density) { 20.dp.toPx() } * floatOffset2
            val orb2DeltaY = with(density) { 24.dp.toPx() } * floatOffset2

            // Bottom-right luminous purple orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x34B06BFF), Color(0x0E8B5CF6), Color.Transparent),
                    center = Offset(width * 0.85f + orb2DeltaX, height * 0.90f + orb2DeltaY),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(width * 0.85f + orb2DeltaX, height * 0.90f + orb2DeltaY)
            )
        }

        content()
    }
}
