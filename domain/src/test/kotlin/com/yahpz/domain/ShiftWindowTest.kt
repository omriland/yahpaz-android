package com.yahpz.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftWindowTest {
    private val startsAt = Instant.parse("2026-11-22T07:00:00Z")
    private val endsAt = Instant.parse("2026-11-22T15:00:00Z")

    private fun phaseAt(now: Instant) = shiftWindowPhase(startsAt, endsAt, now)

    @Test
    fun beforeStartIsPlanned() {
        assertEquals(ShiftPhase.PLANNED, phaseAt(startsAt.minusMillis(1)))
    }

    @Test
    fun exactlyAtStartIsActive() {
        assertEquals(ShiftPhase.ACTIVE, phaseAt(startsAt))
    }

    @Test
    fun justBeforeEndIsStillActive() {
        assertEquals(ShiftPhase.ACTIVE, phaseAt(endsAt.minusMillis(1)))
    }

    @Test
    fun exactlyAtEndIsFinishing() {
        assertEquals(ShiftPhase.FINISHING, phaseAt(endsAt))
    }

    @Test
    fun justBeforeGraceEndsIsStillFinishing() {
        val closeMoment = endsAt.plus(Duration.ofHours(48))
        assertEquals(ShiftPhase.FINISHING, phaseAt(closeMoment.minusMillis(1)))
    }

    @Test
    fun exactlyAtEndPlus48HoursIsClosed() {
        assertEquals(ShiftPhase.CLOSED, phaseAt(endsAt.plus(Duration.ofHours(48))))
    }

    @Test
    fun missingHoursAreLegacy() {
        assertEquals(ShiftPhase.LEGACY, shiftWindowPhase(null, null, startsAt))
        assertEquals(ShiftPhase.LEGACY, shiftWindowPhase(startsAt, null, startsAt))
        assertEquals(ShiftPhase.LEGACY, shiftWindowPhase(null, endsAt, endsAt))
    }

    @Test
    fun crewCanEditOnlyWhileActiveOrFinishing() {
        assertTrue(crewCanEdit(ShiftPhase.ACTIVE))
        assertTrue(crewCanEdit(ShiftPhase.FINISHING))
        assertFalse(crewCanEdit(ShiftPhase.PLANNED))
        assertFalse(crewCanEdit(ShiftPhase.CLOSED))
        assertFalse(crewCanEdit(ShiftPhase.LEGACY))
    }

    private val morning = TimeRange(startsAt, endsAt)

    @Test
    fun partlyOverlappingRangesOverlapInBothOrders() {
        val late = TimeRange(Instant.parse("2026-11-22T14:00:00Z"), Instant.parse("2026-11-22T22:00:00Z"))
        assertTrue(rangesOverlap(morning, late))
        assertTrue(rangesOverlap(late, morning))
    }

    @Test
    fun containedRangeOverlaps() {
        val inside = TimeRange(Instant.parse("2026-11-22T09:00:00Z"), Instant.parse("2026-11-22T10:00:00Z"))
        assertTrue(rangesOverlap(morning, inside))
        assertTrue(rangesOverlap(inside, morning))
    }

    @Test
    fun touchingEdgesDoNotOverlap() {
        val midday = TimeRange(endsAt, Instant.parse("2026-11-22T23:00:00Z"))
        assertFalse(rangesOverlap(morning, midday))
        assertFalse(rangesOverlap(midday, morning))
    }

    @Test
    fun separateRangesDoNotOverlap() {
        val nextDay = TimeRange(Instant.parse("2026-11-23T07:00:00Z"), Instant.parse("2026-11-23T15:00:00Z"))
        assertFalse(rangesOverlap(morning, nextDay))
    }

    @Test
    fun morningDefaultsTo0700To1500() {
        assertEquals(ShiftHours("07:00", "15:00"), defaultShiftHours("morning"))
    }

    @Test
    fun middayDefaultsTo1500To2300() {
        assertEquals(ShiftHours("15:00", "23:00"), defaultShiftHours("midday"))
    }

    @Test
    fun otherKindsHaveNoDefaultHours() {
        assertNull(defaultShiftHours("reinforcement"))
        assertNull(defaultShiftHours("escort"))
        assertNull(defaultShiftHours("other"))
    }

    @Test
    fun winterWallClockIsUtcPlusTwo() {
        val window = buildShiftWindow("2026-11-22", "07:00", "15:00")
        assertEquals(Instant.parse("2026-11-22T05:00:00Z"), window.startsAt)
        assertEquals(Instant.parse("2026-11-22T13:00:00Z"), window.endsAt)
    }

    @Test
    fun summerWallClockIsUtcPlusThree() {
        val window = buildShiftWindow("2026-07-01", "07:00", "15:00")
        assertEquals(Instant.parse("2026-07-01T04:00:00Z"), window.startsAt)
        assertEquals(Instant.parse("2026-07-01T12:00:00Z"), window.endsAt)
    }

    @Test
    fun endEarlierThanStartLandsOnTheNextDay() {
        val window = buildShiftWindow("2026-11-22", "23:00", "07:00")
        assertEquals(Instant.parse("2026-11-22T21:00:00Z"), window.startsAt)
        assertEquals(Instant.parse("2026-11-23T05:00:00Z"), window.endsAt)
    }

    @Test
    fun endAtMonthEndRollsIntoTheNextMonth() {
        val window = buildShiftWindow("2026-11-30", "22:00", "06:00")
        assertEquals(Instant.parse("2026-12-01T04:00:00Z"), window.endsAt)
    }

    @Test
    fun timeOfDayIsShownOnTheJerusalemClock() {
        assertEquals("07:00", formatJerusalemTimeOfDay(Instant.parse("2026-11-22T05:00:00Z")))
        assertEquals("07:00", formatJerusalemTimeOfDay(Instant.parse("2026-07-01T04:00:00Z")))
        assertEquals("00:30", formatJerusalemTimeOfDay(Instant.parse("2026-11-22T22:30:00Z")))
    }

    private val knownShift = OverlapKnownShift(
        id = "shift-9",
        shiftKind = "morning",
        startsAt = Instant.parse("2026-11-22T05:00:00Z"),
        endsAt = Instant.parse("2026-11-22T13:00:00Z"),
    )

    @Test
    fun overlapMessageNamesTheOtherShiftKindAndHours() {
        assertEquals(
            "המשמרת חופפת למשמרת בוקר באותן שעות (07:00–15:00) של אחד מאנשי הצוות.",
            mapShiftOverlapError("shift-9", listOf(knownShift)),
        )
    }

    @Test
    fun overlapMessageFallsBackToTheGeneralSentenceWhenTheShiftIsUnknown() {
        val general = "המשמרת חופפת למשמרת אחרת של אחד מאנשי הצוות."
        assertEquals(general, mapShiftOverlapError("shift-other", listOf(knownShift)))
        assertEquals(general, mapShiftOverlapError(null, listOf(knownShift)))
    }

    @Test
    fun crewOverlapMessageNamesThePersonTheTypeAndTheHours() {
        assertEquals(
            "לא ניתן לשבץ את דנה כהן: כבר משובץ במשמרת בוקר באותן שעות (07:00–15:00).",
            crewOverlapMessage("דנה כהן", knownShift),
        )
    }
}
