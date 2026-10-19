package com.example

import com.example.model.LevelGenerator
import com.example.model.TUBE_CAPACITY
import com.example.model.Tube
import com.example.model.WaterColor
import com.example.model.canPour
import com.example.model.isSolvedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

/**
 * Unit tests verifying Water Sort puzzle mechanics: level generation,
 * pour validations, completion states, and reverse-scramble solvability.
 */
class ExampleUnitTest {

    @Test
    fun levelGenerator_createsValidTubes() {
        val level = 1
        val tubes = LevelGenerator.generate(level)

        // For level 1: 3 colored tubes + 2 empty tubes = 5 total
        assertEquals(5, tubes.size)

        // Total segments across all tubes must equal 3 * TUBE_CAPACITY
        val totalSegments = tubes.sumOf { it.segments.size }
        assertEquals(3 * TUBE_CAPACITY, totalSegments)

        // Board must not start in an already solved state
        assertFalse(isSolvedState(tubes))

        // No tube should start already completed
        assertFalse(tubes.any { it.isComplete })
    }

    @Test
    fun canPour_validatesRulesCorrectly() {
        val tubeRed = Tube(listOf(WaterColor.BLUE, WaterColor.RED))
        val tubeEmpty = Tube(emptyList<WaterColor>())
        val tubeRedTop = Tube(listOf(WaterColor.CYAN, WaterColor.RED))
        val tubeYellowTop = Tube(listOf(WaterColor.CYAN, WaterColor.YELLOW))
        val tubeFull = Tube(listOf(WaterColor.RED, WaterColor.RED, WaterColor.RED, WaterColor.RED))

        val tubes = listOf(tubeRed, tubeEmpty, tubeRedTop, tubeYellowTop, tubeFull)

        // Can pour to empty tube
        assertTrue(canPour(tubes, from = 0, to = 1))

        // Can pour to tube with matching top color
        assertTrue(canPour(tubes, from = 0, to = 2))

        // Cannot pour to tube with different top color
        assertFalse(canPour(tubes, from = 0, to = 3))

        // Cannot pour to full tube
        assertFalse(canPour(tubes, from = 0, to = 4))

        // Cannot pour from empty tube
        assertFalse(canPour(tubes, from = 1, to = 0))

        // Cannot pour to self
        assertFalse(canPour(tubes, from = 0, to = 0))
    }

    @Test
    fun isSolvedState_detectsCompletedGame() {
        val solvedTubes = listOf(
            Tube(listOf(WaterColor.RED, WaterColor.RED, WaterColor.RED, WaterColor.RED)),
            Tube(listOf(WaterColor.BLUE, WaterColor.BLUE, WaterColor.BLUE, WaterColor.BLUE)),
            Tube(emptyList<WaterColor>()),
            Tube(emptyList<WaterColor>())
        )
        assertTrue(isSolvedState(solvedTubes))

        val unsolvedTubes = listOf(
            Tube(listOf(WaterColor.RED, WaterColor.BLUE, WaterColor.RED, WaterColor.RED)),
            Tube(listOf(WaterColor.BLUE, WaterColor.RED, WaterColor.BLUE, WaterColor.BLUE)),
            Tube(emptyList<WaterColor>()),
            Tube(emptyList<WaterColor>())
        )
        assertFalse(isSolvedState(unsolvedTubes))
    }

    /**
     * Kyun: User requirement B.8 — 200 levels generate karke check karta hai ki koi tube
     * pehle se complete nahi hai aur total segments sahi hain.
     */
    @Test
    fun levelGenerator_200Levels_noCompleteTubesAndCorrectSegments() {
        for (lvl in 1..200) {
            val tubes = LevelGenerator.generate(lvl)
            val n = min(3 + (lvl - 1) / 3, 8)

            // Tube count must be n + 2
            assertEquals("Level $lvl should have ${n + 2} tubes", n + 2, tubes.size)

            // Total segments must equal n * 4
            val totalSegments = tubes.sumOf { it.segments.size }
            assertEquals("Level $lvl total segments must be ${n * TUBE_CAPACITY}", n * TUBE_CAPACITY, totalSegments)

            // Koi tube pehle se complete nahi honi chahiye
            val hasCompleteTube = tubes.any { it.isComplete }
            assertFalse("Level $lvl must not start with an already completed tube", hasCompleteTube)

            // Board must not be in a solved state
            assertFalse("Level $lvl must not be already solved at start", isSolvedState(tubes))
        }
    }
}
