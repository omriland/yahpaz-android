package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MineShiftsTest {
    @Test
    fun futureShiftIsNotPendingLog() {
        assertTrue(isShiftFuture("2026-08-18", "2026-08-17"))
        assertFalse(isShiftPendingLog("2026-08-18", null, null, "2026-08-17"))
    }

    @Test
    fun pastShiftWithoutOdometersIsPending() {
        assertTrue(isShiftPendingLog("2026-08-16", null, 10.0, "2026-08-17"))
    }

    @Test
    fun pastShiftWithBothOdometersIsLogged() {
        assertFalse(isShiftPendingLog("2026-08-16", 1.0, 20.0, "2026-08-17"))
    }

    @Test
    fun partitionSplitsPendingFutureAndLogged() {
        val sections = partitionMineShifts(
            listOf(
                MineShiftItem("future", "2026-08-20", null, null),
                MineShiftItem("pending", "2026-08-16", null, null),
                MineShiftItem("logged", "2026-08-15", 1.0, 8.0),
            ),
            "2026-08-17",
            1,
        )
        assertEquals(listOf("pending"), sections.pending.map { it.id })
        assertEquals(listOf("future"), sections.future.map { it.id })
        assertEquals(listOf("logged"), sections.logged.map { it.id })
        assertFalse(sections.hasMoreLogged)
    }

    @Test
    fun shiftStamps() {
        assertEquals("טיוטה", shiftStamp(ShiftStatus.DRAFT).label)
        assertEquals("במשמרת", shiftStamp(ShiftStatus.IN_PROGRESS).label)
        assertEquals("נסגרה", shiftStamp(ShiftStatus.CLOSED).label)
    }

    @Test
    fun hebrewWeekdayLetter() {
        assertEquals("א", hebrewWeekdayLetter("2026-08-16"))
        assertEquals("ב", hebrewWeekdayLetter("2026-08-17"))
        assertEquals("ש", hebrewWeekdayLetter("2026-08-22"))
    }
}
