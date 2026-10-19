package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Victory modal displayed upon completing a level, featuring glassmorphism,
 * gradient headline, and a "Next Level →" action.
 */
@Composable
fun WinDialog(
    isWin: Boolean,
    level: Int,
    moves: Int,
    onNextLevelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isWin,
        enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xB8050810))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume backdrop touches
                ),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(26.dp)

            Column(
                modifier = Modifier
                    .testTag("win_dialog")
                    .padding(32.dp)
                    .widthIn(min = 280.dp, max = 360.dp)
                    .clip(cardShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x30FFFFFF), cardShape)
                    .padding(horizontal = 36.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Gradient Headline
                Text(
                    text = stringResource(R.string.level_complete),
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF6EE7FF), Color(0xFFA78BFA), Color(0xFFF472B6))
                        )
                    ),
                    modifier = Modifier.testTag("win_title")
                )

                // Stats: Level & moves
                Text(
                    text = "Level $level · $moves moves",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 26.dp)
                        .testTag("win_stats")
                )

                // Next Level Button
                val buttonShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .testTag("next_level_button")
                        .shadow(12.dp, buttonShape, spotColor = Color(0x806366F1))
                        .clip(buttonShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
                            onClick = onNextLevelClick
                        )
                        .padding(horizontal = 36.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.next_level),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
