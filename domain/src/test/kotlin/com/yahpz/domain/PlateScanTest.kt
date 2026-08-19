package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateScanTest {
    @Test
    fun extractsSevenDigitPlateWithDashes() {
        assertEquals(listOf("1234567"), extractIsraeliPlateCandidates("12-345-67"))
    }

    @Test
    fun extractsEightDigitPlateWithDashes() {
        assertEquals(listOf("71386301"), extractIsraeliPlateCandidates("713-86-301"))
    }

    @Test
    fun mapsCommonOcrGlyphsToDigits() {
        assertEquals(listOf("1234567"), extractIsraeliPlateCandidates("I2-34S-67"))
    }

    @Test
    fun ignoresShortOrLongDigitRuns() {
        assertTrue(extractIsraeliPlateCandidates("123456").isEmpty())
        assertTrue(extractIsraeliPlateCandidates("123456789").none { it.length !in 7..8 })
    }

    @Test
    fun prefersEightDigitWhenOverlapping() {
        val hits = extractIsraeliPlateCandidates("71386301")
        assertEquals("71386301", hits.first())
    }

    @Test
    fun confirmRequiresStreak() {
        var state = PlateScanConfirmState()
        var confirmed: String?
        val step1 = advancePlateScanConfirm(state, "1234567", requiredStreak = 3)
        state = step1.first
        confirmed = step1.second
        assertNull(confirmed)
        assertEquals(1, state.streak)

        val step2 = advancePlateScanConfirm(state, "1234567", requiredStreak = 3)
        state = step2.first
        assertNull(step2.second)

        val step3 = advancePlateScanConfirm(state, "1234567", requiredStreak = 3)
        assertEquals("1234567", step3.second)
    }

    @Test
    fun confirmResetsOnDifferentCandidate() {
        var state = PlateScanConfirmState(digits = "1234567", streak = 2)
        val (next, confirmed) = advancePlateScanConfirm(state, "7654321", requiredStreak = 3)
        assertNull(confirmed)
        assertEquals("7654321", next.digits)
        assertEquals(1, next.streak)
    }

    @Test
    fun confirmClearsOnEmpty() {
        val (next, confirmed) = advancePlateScanConfirm(
            PlateScanConfirmState(digits = "1234567", streak = 2),
            null,
            requiredStreak = 3,
        )
        assertNull(confirmed)
        assertNull(next.digits)
        assertEquals(0, next.streak)
    }
}
