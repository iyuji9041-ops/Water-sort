package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioHapticsManager
import com.example.data.PreferencesManager
import com.example.model.FlyingDroplet
import com.example.model.LevelGenerator
import com.example.model.TUBE_CAPACITY
import com.example.model.Tube
import com.example.model.WaterSortUiState
import com.example.model.canPour
import com.example.model.hasAnyLegalMove
import com.example.model.isSolvedState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * ViewModel orchestrating game state, pour animations, undo history,
 * audio/haptic effects, and level progression.
 */
class WaterSortViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    val audioManager = AudioHapticsManager(application)

    private val _uiState = MutableStateFlow(WaterSortUiState())
    val uiState: StateFlow<WaterSortUiState> = _uiState.asStateFlow()

    // Move history stack for Undo support (snapshots of tubes)
    private val history = ArrayDeque<List<Tube>>()

    // Initial tubes of the current level to enable Restarting the exact same puzzle
    private var initialLevelTubes: List<Tube> = emptyList()

    private var comboPitch: Float = 1.0f
    private var shakeJob: Job? = null
    private var dropletIdCounter = 0L

    // Kyun: User requirement B.11 — Guard flag to prevent rapid double-tap level skipping
    private var isAdvancingLevel = false

    init {
        viewModelScope.launch {
            // Load saved level from DataStore preferences
            val savedLevel = preferencesManager.savedLevelFlow.first()
            startLevel(savedLevel)
        }

        // Kyun: User requirement C.15 — Collect mute setting from DataStore
        viewModelScope.launch {
            preferencesManager.isMutedFlow.collect { muted ->
                audioManager.isMuted = muted
                _uiState.update { it.copy(isMuted = muted) }
            }
        }
    }

    /**
     * Initializes or restarts the specified puzzle level.
     */
    private fun startLevel(level: Int) {
        val newTubes = LevelGenerator.generate(level)
        initialLevelTubes = newTubes.map { Tube(it.segments.toList()) }
        history.clear()
        comboPitch = 1.0f

        val stuck = !isSolvedState(newTubes) && !hasAnyLegalMove(newTubes)

        _uiState.update {
            it.copy(
                level = level,
                moves = 0,
                tubes = newTubes,
                selectedIndex = -1,
                isBusy = false,
                shakingTubeIndex = -1,
                pouringTubeIndex = -1,
                pourTargetIndex = -1,
                hasExtraTube = false,
                isStuck = stuck,
                activeDroplet = null,
                isWin = false,
                canUndo = false
            )
        }
    }

    /**
     * Handles user tap on a tube at the specified index.
     */
    fun onTubeClick(index: Int) {
        val state = _uiState.value
        if (state.isBusy || state.isWin) return

        val currentSelected = state.selectedIndex

        // Deselect if tapping the already selected tube
        if (currentSelected == index) {
            _uiState.update { it.copy(selectedIndex = -1) }
            return
        }

        // First tube selection
        if (currentSelected == -1) {
            val tube = state.tubes.getOrNull(index) ?: return
            if (tube.isEmpty || tube.isComplete) {
                audioManager.playDeny()
                return
            }
            _uiState.update { it.copy(selectedIndex = index) }
            audioManager.playSelect()
            return
        }

        // Attempting to pour from currentSelected -> index
        if (canPour(state.tubes, currentSelected, index)) {
            val from = currentSelected
            val to = index
            _uiState.update { it.copy(selectedIndex = -1) }
            executePour(from, to)
        } else {
            // Invalid move -> deny sound, buzz, and shake animation
            audioManager.playDeny()
            shakeTube(index)
            _uiState.update { it.copy(selectedIndex = -1) }
        }
    }

    /**
     * Shakes the target tube horizontally to indicate an invalid pour.
     */
    private fun shakeTube(index: Int) {
        shakeJob?.cancel()
        shakeJob = viewModelScope.launch {
            _uiState.update { it.copy(shakingTubeIndex = index) }
            delay(340)
            _uiState.update { it.copy(shakingTubeIndex = -1) }
        }
    }

    /**
     * Executes the fluid pour animation.
     * Incorporates User requirements B.5 (pourTargetIndex) and B.10 (early validation before history snapshot).
     */
    private fun executePour(from: Int, to: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            // Kyun: User requirement B.10 — Early validation before touching history or moves
            val sourceTube = _uiState.value.tubes.getOrNull(from)
            val targetTube = _uiState.value.tubes.getOrNull(to)
            val color = sourceTube?.topColor

            if (sourceTube == null || targetTube == null || color == null) {
                _uiState.update { it.copy(isBusy = false) }
                return@launch
            }

            // Snapshot current tubes for Undo ONLY after validation passes
            val currentTubes = _uiState.value.tubes.map { Tube(it.segments.toList()) }
            history.addLast(currentTubes)
            if (history.size > 60) history.removeFirst()

            val moves = _uiState.value.moves + 1
            // Kyun: User requirement B.5 — pourTargetIndex set kiya taaki pour ke dauran rotation constant rahe
            _uiState.update {
                it.copy(
                    moves = moves,
                    canUndo = true,
                    pouringTubeIndex = from,
                    pourTargetIndex = to
                )
            }

            // Count top consecutive segments of the same color
            val matchingCount = sourceTube.topMatchingCount
            val availableSpace = TUBE_CAPACITY - targetTube.segments.size
            val countToPour = min(matchingCount, availableSpace)

            delay(170)

            var runningTubes = _uiState.value.tubes.map { Tube(it.segments.toMutableList()) }

            for (k in 0 until countToPour) {
                // 1. Remove 1 segment from source
                val fromList = runningTubes[from].segments.toMutableList()
                val movedSegment = fromList.removeAt(fromList.size - 1)
                runningTubes = runningTubes.toMutableList().also {
                    it[from] = Tube(fromList)
                }

                // Rising pitch as target tube fills
                val targetCount = runningTubes[to].segments.size
                val pitch = 1.0f + (targetCount * 0.11f) + min(comboPitch * 0.03f, 0.25f)
                audioManager.playDrop(pitch)

                // Spawn droplet
                val droplet = FlyingDroplet(
                    id = ++dropletIdCounter,
                    color = color,
                    fromTubeIndex = from,
                    toTubeIndex = to
                )
                _uiState.update {
                    it.copy(
                        tubes = runningTubes,
                        activeDroplet = droplet
                    )
                }

                delay(125)

                // 2. Add 1 segment to target
                val toList = runningTubes[to].segments.toMutableList()
                toList.add(movedSegment)
                runningTubes = runningTubes.toMutableList().also {
                    it[to] = Tube(toList)
                }

                audioManager.playPop(1.0f + toList.size * 0.08f)

                _uiState.update {
                    it.copy(
                        tubes = runningTubes,
                        activeDroplet = null
                    )
                }

                if (runningTubes[to].isComplete) {
                    audioManager.playDone()
                }

                delay(60)
            }

            delay(140)

            val solved = isSolvedState(runningTubes)
            val stuck = !solved && !hasAnyLegalMove(runningTubes)

            // Kyun: User requirement B.5 — pourTargetIndex reset kiya
            _uiState.update {
                it.copy(
                    pouringTubeIndex = -1,
                    pourTargetIndex = -1,
                    isBusy = false,
                    isStuck = stuck
                )
            }

            if (solved) {
                handleWin()
            }
        }
    }

    private fun handleWin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            comboPitch = (comboPitch + 0.2f).coerceAtMost(2.0f)

            delay(200)
            audioManager.playWin()

            delay(700)
            _uiState.update { it.copy(isWin = true, isBusy = false) }
        }
    }

    /**
     * Undoes the last move, restoring previous tube states.
     */
    fun undo() {
        val state = _uiState.value
        if (state.isBusy || history.isEmpty()) return

        val previousTubes = history.removeLast()
        val newMoves = (state.moves - 1).coerceAtLeast(0)
        val stuck = !isSolvedState(previousTubes) && !hasAnyLegalMove(previousTubes)

        _uiState.update {
            it.copy(
                tubes = previousTubes,
                moves = newMoves,
                selectedIndex = -1,
                isStuck = stuck,
                canUndo = history.isNotEmpty()
            )
        }
        audioManager.playSelect()
    }

    /**
     * Kyun: User requirement B.9 — Adds one extra empty tube per level when user is stuck or requests escape.
     */
    fun addExtraTube() {
        val state = _uiState.value
        if (state.hasExtraTube || state.isBusy || state.isWin) return

        val updatedTubes = state.tubes + Tube(emptyList<com.example.model.WaterSegment>())
        _uiState.update {
            it.copy(
                tubes = updatedTubes,
                hasExtraTube = true,
                isStuck = false
            )
        }
        audioManager.playSelect()
    }

    /**
     * Restarts the current level with the exact same initial puzzle arrangement.
     */
    fun restartLevel() {
        val state = _uiState.value
        if (state.isBusy) return

        history.clear()
        val stuck = !isSolvedState(initialLevelTubes) && !hasAnyLegalMove(initialLevelTubes)

        _uiState.update {
            it.copy(
                tubes = initialLevelTubes.map { t -> Tube(t.segments.toList()) },
                moves = 0,
                selectedIndex = -1,
                hasExtraTube = false,
                isStuck = stuck,
                isBusy = false,
                isWin = false,
                canUndo = false
            )
        }
        audioManager.playSelect()
    }

    /**
     * Advances to the next level.
     * Kyun: User requirement B.11 — Guard flag isAdvancingLevel and immediate isWin = false.
     */
    fun nextLevel() {
        if (isAdvancingLevel) return
        isAdvancingLevel = true
        _uiState.update { it.copy(isWin = false, isBusy = true) }

        viewModelScope.launch {
            try {
                val nextLvl = _uiState.value.level + 1
                preferencesManager.saveLevel(nextLvl)
                startLevel(nextLvl)
                audioManager.playSelect()
            } finally {
                isAdvancingLevel = false
            }
        }
    }

    /**
     * Kyun: User requirement C.15 — Toggles mute state and saves to DataStore.
     */
    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        viewModelScope.launch {
            preferencesManager.setMuted(newMuted)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
