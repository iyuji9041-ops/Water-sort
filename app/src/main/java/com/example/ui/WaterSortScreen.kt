package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Tube
import com.example.viewmodel.WaterSortViewModel

/**
 * Main game screen integrating the HUD, ambient background, tube board,
 * animated droplet effects, and celebratory win dialog.
 */
@Composable
fun WaterSortScreen(
    viewModel: WaterSortViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Screen-space top-center coordinates of each tube for accurate droplet trajectories
    val tubePositions = remember { mutableStateMapOf<Int, Offset>() }

    AmbientBackground(modifier = modifier) {
        // Main UI inside statusBarsPadding and navigationBarsPadding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top HUD: Level, Moves, Extra Tube (+), Mute toggle, Undo, Reset
                HudView(
                    level = uiState.level,
                    moves = uiState.moves,
                    canUndo = uiState.canUndo,
                    isBusy = uiState.isBusy,
                    hasExtraTube = uiState.hasExtraTube,
                    isStuck = uiState.isStuck,
                    isMuted = uiState.isMuted,
                    onUndoClick = { viewModel.undo() },
                    onRestartClick = { viewModel.restartLevel() },
                    onAddTubeClick = { viewModel.addExtraTube() },
                    onToggleMuteClick = { viewModel.toggleMute() }
                )

                // Main Puzzle Board
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val availableWidth = maxWidth
                    val availableHeight = maxHeight

                    val totalTubes = uiState.tubes.size
                    val itemsPerRow = when {
                        totalTubes <= 4 -> totalTubes
                        totalTubes <= 6 -> 3
                        totalTubes <= 8 -> 4
                        else -> 5
                    }

                    // Kyun: User requirement B.7 — Available height ko consider karke tube height cap ki
                    // (2 rows + 28dp gap + 30dp lift + margin availableHeight me fit hon, min 110dp)
                    val targetTubeWidth = ((availableWidth - (itemsPerRow * 12).dp) / itemsPerRow)
                        .coerceIn(42.dp, 60.dp)
                    val maxAllowedHeight = ((availableHeight - 28.dp - 30.dp - 16.dp) / 2)
                        .coerceAtLeast(110.dp)
                    val targetTubeHeight = (targetTubeWidth * 3.4f).coerceIn(110.dp, maxAllowedHeight)

                    val firstRowCount = if (totalTubes <= 4) totalTubes else (totalTubes + 1) / 2
                    val firstRowTubes = uiState.tubes.take(firstRowCount)
                    val secondRowTubes = uiState.tubes.drop(firstRowCount)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Kyun: User requirement E.19 — TubeRow private composable se dono rows ka code deduplicate kiya
                        TubeRow(
                            tubes = firstRowTubes,
                            startIndex = 0,
                            selectedIndex = uiState.selectedIndex,
                            pouringTubeIndex = uiState.pouringTubeIndex,
                            shakingTubeIndex = uiState.shakingTubeIndex,
                            pourTargetIndex = uiState.pourTargetIndex,
                            isWin = uiState.isWin,
                            tubeWidth = targetTubeWidth,
                            tubeHeight = targetTubeHeight,
                            tubePositions = tubePositions,
                            onPositioned = { idx, offset -> tubePositions[idx] = offset },
                            onTubeClick = { viewModel.onTubeClick(it) }
                        )

                        if (secondRowTubes.isNotEmpty()) {
                            TubeRow(
                                tubes = secondRowTubes,
                                startIndex = firstRowCount,
                                selectedIndex = uiState.selectedIndex,
                                pouringTubeIndex = uiState.pouringTubeIndex,
                                shakingTubeIndex = uiState.shakingTubeIndex,
                                pourTargetIndex = uiState.pourTargetIndex,
                                isWin = uiState.isWin,
                                tubeWidth = targetTubeWidth,
                                tubeHeight = targetTubeHeight,
                                tubePositions = tubePositions,
                                onPositioned = { idx, offset -> tubePositions[idx] = offset },
                                onTubeClick = { viewModel.onTubeClick(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confetti animation on win
            ConfettiView(isWin = uiState.isWin)

            // Win Dialog overlay
            WinDialog(
                isWin = uiState.isWin,
                level = uiState.level,
                moves = uiState.moves,
                onNextLevelClick = { viewModel.nextLevel() }
            )
        }

        // Kyun: User requirement B.4 — DropletOverlay padded Box ke bahar root container me rakha hai,
        // jisse positionInRoot() ke exact window coordinates match hote hain bina offset mismatch ke.
        DropletOverlay(
            activeDroplet = uiState.activeDroplet,
            tubePositions = tubePositions
        )
    }
}

/**
 * Kyun: User requirement E.19 — Reusable private composable for rendering a horizontal row of tubes.
 */
@Composable
private fun TubeRow(
    tubes: List<Tube>,
    startIndex: Int,
    selectedIndex: Int,
    pouringTubeIndex: Int,
    shakingTubeIndex: Int,
    pourTargetIndex: Int,
    isWin: Boolean,
    tubeWidth: Dp,
    tubeHeight: Dp,
    tubePositions: Map<Int, Offset>,
    onPositioned: (Int, Offset) -> Unit,
    onTubeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tubes.forEachIndexed { i, tube ->
            val actualIndex = startIndex + i
            val isSelected = selectedIndex == actualIndex
            val isPouring = pouringTubeIndex == actualIndex
            val isShaking = shakingTubeIndex == actualIndex

            // Kyun: User requirement B.5 — pourTargetIndex se continuous constant direction calculate hoti hai
            val targetDirection = if (isPouring && pourTargetIndex != -1) {
                val startX = tubePositions[actualIndex]?.x ?: 0f
                val targetX = tubePositions[pourTargetIndex]?.x ?: 0f
                if (targetX >= startX) 1 else -1
            } else 0

            TubeView(
                tube = tube,
                index = actualIndex,
                isSelected = isSelected,
                isPouring = isPouring,
                isShaking = isShaking,
                pourTargetDirection = targetDirection,
                isWinCelebrating = isWin,
                tubeWidth = tubeWidth,
                tubeHeight = tubeHeight,
                onClick = { onTubeClick(actualIndex) },
                modifier = Modifier.onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val size = coords.size
                    onPositioned(actualIndex, Offset(pos.x + size.width / 2f, pos.y))
                }
            )
        }
    }
}
