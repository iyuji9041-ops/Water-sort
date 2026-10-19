package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top HUD displaying Level count, Moves counter, Extra Tube (+), Mute toggle, Undo button,
 * and Restart button with translucent glassmorphism aesthetics.
 */
@Composable
fun HudView(
    level: Int,
    moves: Int,
    canUndo: Boolean,
    isBusy: Boolean,
    hasExtraTube: Boolean,
    isStuck: Boolean,
    isMuted: Boolean,
    onUndoClick: () -> Unit,
    onRestartClick: () -> Unit,
    onAddTubeClick: () -> Unit,
    onToggleMuteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Stats: Level & Moves
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(
                label = "LEVEL",
                value = level.toString(),
                modifier = Modifier.testTag("stat_level")
            )
            StatPill(
                label = "MOVES",
                value = moves.toString(),
                modifier = Modifier.testTag("stat_moves")
            )
        }

        // Right Actions: Extra Tube (+), Mute toggle, Undo, Restart
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Kyun: User requirement B.9 — Extra Tube (+) button with highlight when stuck
            val extraTubeBorderColor by animateColorAsState(
                targetValue = if (isStuck && !hasExtraTube) Color(0xFF22D97A) else Color(0x24FFFFFF),
                animationSpec = if (isStuck && !hasExtraTube) {
                    infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                } else tween(200),
                label = "stuck_pulse"
            )

            HudButton(
                onClick = onAddTubeClick,
                enabled = !hasExtraTube && !isBusy,
                testTag = "add_tube_button",
                borderColor = extraTubeBorderColor
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Extra Tube",
                    tint = if (!hasExtraTube && !isBusy) {
                        if (isStuck) Color(0xFF22D97A) else Color.White
                    } else Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Kyun: User requirement C.15 — Mute toggle button with speaker icon
            HudButton(
                onClick = onToggleMuteClick,
                enabled = true,
                testTag = "mute_button"
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFFFF5C8A) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            HudButton(
                onClick = onUndoClick,
                enabled = canUndo && !isBusy,
                testTag = "undo_button"
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo && !isBusy) Color.White else Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(20.dp)
                )
            }

            HudButton(
                onClick = onRestartClick,
                enabled = !isBusy,
                testTag = "reset_button"
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart",
                    tint = if (!isBusy) Color.White else Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .widthIn(min = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = Color.White.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            color = Color.White
        )
    }
}

@Composable
private fun HudButton(
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x24FFFFFF),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .testTag(testTag)
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
