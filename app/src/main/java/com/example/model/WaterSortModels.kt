package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

// Har test tube me maximum 4 segments aa sakte hain (HTML CAP = 4)
const val TUBE_CAPACITY = 4

/**
 * 8 vibrant liquid colors jo original HTML/JS game se match karte hain
 */
enum class WaterColor(
    val code: Char,
    val color: Color,
    val glowColor: Color,
    val label: String
) {
    RED('r', Color(0xFFFF4757), Color(0xCCFF4757), "Red"),
    BLUE('b', Color(0xFF3B6BFF), Color(0xCC3B6BFF), "Blue"),
    GREEN('g', Color(0xFF22D97A), Color(0xCC22D97A), "Green"),
    YELLOW('y', Color(0xFFFFC531), Color(0xCCFFC531), "Yellow"),
    PURPLE('p', Color(0xFFB06BFF), Color(0xCCB06BFF), "Purple"),
    CYAN('c', Color(0xFF22D3EE), Color(0xCC22D3EE), "Cyan"),
    PINK('k', Color(0xFFFF5C8A), Color(0xCCFF5C8A), "Pink"),
    ORANGE('o', Color(0xFFFF8A3D), Color(0xCCFF8A3D), "Orange");

    companion object {
        val ALL_COLORS = entries.toTypedArray()
    }
}

/**
 * Kyun: User requirement D.16 — Segment id stable rakhi hai taaki Compose
 * smooth spring/tween fill animations render kar sake bina state khoe.
 */
data class WaterSegment(
    val id: Long,
    val color: WaterColor
)

/**
 * Tube model: index 0 bottom segment hota hai aur last index top segment.
 */
data class Tube(
    val segments: List<WaterSegment> = emptyList()
) {
    // Secondary constructor testing aur direct color list support ke liye
    constructor(colors: List<WaterColor>, baseId: Long = 0L) : this(
        colors.mapIndexed { index, color -> WaterSegment(id = baseId + index, color = color) }
    )

    val colors: List<WaterColor>
        get() = segments.map { it.color }

    val isComplete: Boolean
        get() = segments.size == TUBE_CAPACITY && segments.all { it.color == segments[0].color }

    val isEmpty: Boolean
        get() = segments.isEmpty()

    val isFull: Boolean
        get() = segments.size >= TUBE_CAPACITY

    val topColor: WaterColor?
        get() = segments.lastOrNull()?.color

    val topSegment: WaterSegment?
        get() = segments.lastOrNull()

    val topMatchingCount: Int
        get() {
            if (segments.isEmpty()) return 0
            val top = segments.last().color
            var count = 0
            for (i in segments.indices.reversed()) {
                if (segments[i].color == top) count++ else break
            }
            return count
        }
}

/**
 * Droplet arc flight state during pour animation
 */
data class FlyingDroplet(
    val id: Long,
    val color: WaterColor,
    val fromTubeIndex: Int,
    val toTubeIndex: Int
)

/**
 * Poori UI screen ka immutable state
 */
data class WaterSortUiState(
    val level: Int = 1,
    val moves: Int = 0,
    val tubes: List<Tube> = emptyList(),
    val selectedIndex: Int = -1,
    val isBusy: Boolean = false,
    val shakingTubeIndex: Int = -1,
    val pouringTubeIndex: Int = -1,
    // Kyun: User requirement B.5 — pourTargetIndex add kiya taaki tilt continuous rahe bina jitter ke
    val pourTargetIndex: Int = -1,
    // Kyun: User requirement B.9 — Extra tube track karne aur no-moves-left highlight ke liye
    val hasExtraTube: Boolean = false,
    val isStuck: Boolean = false,
    // Kyun: User requirement C.15 — Mute toggle state
    val isMuted: Boolean = false,
    val activeDroplet: FlyingDroplet? = null,
    val isWin: Boolean = false,
    val canUndo: Boolean = false
)

/**
 * Solvable puzzle level generation using reverse-move scrambling.
 */
object LevelGenerator {

    private var globalSegmentId = 1L

    /**
     * Kyun: User requirement B.8 — Random shuffle hata kar reverse-move scramble use kiya:
     * - Solved state se shuru karo (n full tubes + 2 empty)
     * - Random (a,b) chuno, a non-empty, b full na ho
     * - Reverse move tabhi legal jab a ka bacha hua top same color ho ya a me 1 hi segment ho
     * - 150 + level*8 successful moves karo, guard 5000 tries
     * - Agar result solved state hai to dobara generate karo
     */
    fun generate(level: Int, random: Random = Random): List<Tube> {
        val n = (3 + (level - 1) / 3).coerceAtMost(8)
        val selectedColors = WaterColor.ALL_COLORS.take(n)
        val targetMoves = 150 + level * 8
        val maxTries = 5000

        var tubes: List<List<WaterSegment>>
        var outerTries = 0

        do {
            outerTries++
            val currentTubes = MutableList(n + 2) { i ->
                if (i < n) {
                    val color = selectedColors[i]
                    MutableList(TUBE_CAPACITY) {
                        WaterSegment(id = globalSegmentId++, color = color)
                    }
                } else {
                    mutableListOf<WaterSegment>()
                }
            }

            var successfulMoves = 0
            var tries = 0
            val totalTubes = n + 2

            while (successfulMoves < targetMoves && tries < maxTries) {
                tries++
                val a = random.nextInt(totalTubes)
                val b = random.nextInt(totalTubes)
                if (a == b) continue

                val tubeA = currentTubes[a]
                val tubeB = currentTubes[b]

                if (tubeA.isNotEmpty() && tubeB.size < TUBE_CAPACITY) {
                    val isLegalReverse = tubeA.size == 1 || tubeA[tubeA.size - 2].color == tubeA.last().color
                    if (isLegalReverse) {
                        val segment = tubeA.removeAt(tubeA.size - 1)
                        tubeB.add(segment)
                        successfulMoves++
                    }
                }
            }

            tubes = currentTubes
        } while (
            outerTries < 50 && (
                tubes.any { t -> t.size == TUBE_CAPACITY && t.all { it.color == t[0].color } } ||
                isSolvedState(tubes.map { Tube(it) })
            )
        )

        return tubes.map { Tube(it) }
    }
}

/**
 * Validation rules for pouring water between tubes.
 */
fun canPour(tubes: List<Tube>, from: Int, to: Int): Boolean {
    if (from == to) return false
    if (from !in tubes.indices || to !in tubes.indices) return false
    val f = tubes[from]
    val t = tubes[to]
    if (f.isEmpty) return false
    if (t.isFull) return false
    if (t.isEmpty) return true
    return f.topColor == t.topColor
}

/**
 * Checks if all tubes are either empty or completely filled with a single color.
 */
fun isSolvedState(tubes: List<Tube>): Boolean {
    return tubes.all { it.isEmpty || it.isComplete }
}

/**
 * Kyun: User requirement B.9 — Checks if there are any legal moves left on the board.
 */
fun hasAnyLegalMove(tubes: List<Tube>): Boolean {
    for (i in tubes.indices) {
        for (j in tubes.indices) {
            if (i != j && canPour(tubes, i, j)) {
                return true
            }
        }
    }
    return false
}
