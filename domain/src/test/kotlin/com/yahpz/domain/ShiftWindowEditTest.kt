package com.yahpz.domain

import java.time.Instant
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class ShiftWindowEditTest {
    private val morning = ShiftWindow(
        startsAt = Instant.parse("2026-11-22T05:00:00Z"),
        endsAt = Instant.parse("2026-11-22T13:00:00Z"),
    )
    private val before = EditedShift("2026-11-22", "morning")

    @Test
    fun unchangedDateAndKindKeepTheWindow() {
        assertEquals(morning, windowAfterShiftEdit(before, "2026-11-22", "morning", morning))
    }

    @Test
    fun newDateMovesTheWindow() {
        val moved = windowAfterShiftEdit(before, "2026-11-25", "morning", morning)
        assertEquals(Instant.parse("2026-11-25T05:00:00Z"), moved?.startsAt)
    }

    @Test
    fun newKindUsesItsDefaultHours() {
        val midday = windowAfterShiftEdit(before, "2026-11-22", "midday", morning)
        assertEquals(Instant.parse("2026-11-22T13:00:00Z"), midday?.startsAt)
        assertEquals(Instant.parse("2026-11-22T21:00:00Z"), midday?.endsAt)
    }

    @Test
    fun kindWithoutDefaultsKeepsTheOldTimeOfDay() {
        val moved = windowAfterShiftEdit(before, "2026-11-25", "escort", morning)
        assertEquals(Instant.parse("2026-11-25T05:00:00Z"), moved?.startsAt)
        assertEquals(Instant.parse("2026-11-25T13:00:00Z"), moved?.endsAt)
    }

    @Test
    fun legacyShiftStaysLegacy() {
        assertNull(windowAfterShiftEdit(before, "2026-11-25", "midday", null))
    }
}
