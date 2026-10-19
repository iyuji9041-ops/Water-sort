package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.TUBE_CAPACITY
import com.example.model.Tube
import com.example.model.WaterSegment
import kotlinx.coroutines.delay

/**
 * Renders an individual test tube with ASMR glassmorphic physics.
 * Implementations per Section D (Rules 16 & 17).
 */
@Composable
fun TubeView(
    tube: Tube,
    index: Int,
    isSelected: Boolean,
    isPouring: Boolean,
    isShaking: Boolean,
    pourTargetDirection: Int, // -1 for left, +1 for right, 0 for none
    isWinCelebrating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tubeWidth: Dp = 56.dp,
    tubeHeight: Dp = 200.dp
) {
    // Selection and pouring lift offset (-30dp for pouring, -20dp for selection)
    val targetOffsetY = when {
        isPouring -> -30.dp
        isSelected -> -20.dp
        else -> 0.dp
    }
    val animatedOffsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tube_lift"
    )

    // Pour tilt angle
    val targetRotation = if (isPouring && pourTargetDirection != 0) {
        if (pourTargetDirection > 0) 22f else -22f
    } else 0f
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tube_tilt"
    )

    // Shake animation for invalid selection
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(-8f, tween(60))
            shakeOffset.animateTo(8f, tween(60))
            shakeOffset.animateTo(-6f, tween(60))
            shakeOffset.animateTo(5f, tween(60))
            shakeOffset.animateTo(0f, tween(60))
        }
    }

    // Win celebration bounce
    val winHop = remember { Animatable(0f) }
    LaunchedEffect(isWinCelebrating) {
        if (isWinCelebrating) {
            delay((index * 60).toLong())
            winHop.animateTo(-16f, tween(180, easing = FastOutSlowInEasing))
            winHop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Pulse glow animation for completed tubes
    val infiniteTransition = rememberInfiniteTransition(label = "tube_done_pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "done_pulse_alpha"
    )

    val tubeShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 26.dp, bottomEnd = 26.dp)

    val glowColor = if (tube.isComplete && tube.segments.isNotEmpty()) {
        tube.segments[0].color.glowColor.copy(alpha = pulseGlowAlpha)
    } else if (isSelected) {
        Color(0x703B6BFF)
    } else {
        Color.Transparent
    }

    val borderColor = when {
        isSelected -> Color(0x99FFFFFF)
        tube.isComplete -> Color(0x99FFFFFF)
        else -> Color(0x28FFFFFF)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .testTag("tube_$index")
            // Kyun: User requirement D.17 — Pouring tube par 20f zIndex aur selected par 10f
            .zIndex(if (isPouring) 20f else if (isSelected) 10f else 0f)
            .width(tubeWidth)
            .height(tubeHeight)
            .offset(x = shakeOffset.value.dp, y = animatedOffsetY + winHop.value.dp)
            .rotate(animatedRotation)
            .drawBehind {
                if (tube.isComplete || isSelected) {
                    drawRoundRect(
                        color = glowColor,
                        size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()),
                        topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glass tube container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(tubeShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x0CFFFFFF), Color(0x04FFFFFF))
                    )
                )
                .border(2.dp, borderColor, tubeShape)
        ) {
            // Liquid segments stacked from bottom to top
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 2.dp, start = 2.dp, end = 2.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
            ) {
                // Empty slots at the top
                val emptySlots = TUBE_CAPACITY - tube.segments.size
                if (emptySlots > 0) {
                    Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                }

                // Kyun: User requirement D.16 — Animated liquid segments with stagger delay
                tube.segments.forEachIndexed { segIdx, segment ->
                    LiquidSegment(
                        segment = segment,
                        isTopSegment = segIdx == tube.segments.lastIndex,
                        isBottomSegment = segIdx == 0,
                        staggerDelayMs = (segIdx * 45L + index * 25L),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Glass Shine vertical streak along the left side
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .width(6.dp)
                    .padding(top = 10.dp)
                    .offset(x = 7.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x60FFFFFF), Color(0x08FFFFFF))
                        )
                    )
            )

            // Top glass rim lip highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x35FFFFFF), Color.Transparent)
                        )
                    )
            )
        }
    }
}

/**
 * Kyun: User requirement D.16 — Fluid segment with spring/tween bottom-up fill animation (170ms)
 * and 3D cylindrical lighting.
 */
@Composable
private fun LiquidSegment(
    segment: WaterSegment,
    isTopSegment: Boolean,
    isBottomSegment: Boolean,
    staggerDelayMs: Long,
    modifier: Modifier = Modifier
) {
    val bottomRadius = if (isBottomSegment) 24.dp else 0.dp
    val segmentShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius
    )

    val fillScaleY = remember(segment.id) { Animatable(0f) }
    LaunchedEffect(segment.id) {
        if (staggerDelayMs > 0) delay(staggerDelayMs)
        fillScaleY.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleY = fillScaleY.value
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(segmentShape)
            .background(segment.color.color)
    ) {
        // 3D cylindrical lighting gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0x45FFFFFF),
                        0.25f to Color(0x10FFFFFF),
                        0.65f to Color.Transparent,
                        1.0f to Color(0x38000000)
                    )
                )
        )

        // Specular meniscus highlight on top of the liquid column
        if (isTopSegment) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0x55FFFFFF))
            )
        }
    }
}
