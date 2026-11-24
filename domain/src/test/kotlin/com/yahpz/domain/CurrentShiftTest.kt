package com.yahpz.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentShiftTest {
    private fun currentShift(
        kind: String = "morning",
        endsAt: String = "2026-11-22T13:00:00Z",
    ) = CurrentShift(
        id = "shift-1",
        shiftKind = kind,
        shiftDate = "2026-11-22",
        startsAt = Instant.parse("2026-11-22T05:00:00Z"),
        endsAt = Instant.parse(endsAt),
        vehicleType = "patrol_north",
    )

    private val noon = Instant.parse("2026-11-22T10:00:00Z")

    @Test
    fun activeBarNamesTheKindAndTheEndTimeOnTheJerusalemClock() {
        val shift = currentShift(kind = "midday", endsAt = "2026-11-22T21:00:00Z")
        assertEquals("במשמרת צהריים עכשיו · עד 23:00", barText(shift, ShiftPhase.ACTIVE, noon))
    }

    @Test
    fun finishingBarCountsHoursLeftToDocument() {
        val shift = currentShift(endsAt = "2026-11-22T10:00:00Z")
        val twoHoursAfterEnd = Instant.parse("2026-11-22T12:00:00Z")
        assertEquals(
            "המשמרת הסתיימה, אפשר להשלים תיעוד · עוד 46 שעות",
            barText(shift, ShiftPhase.FINISHING, twoHoursAfterEnd),
        )
    }

    @Test
    fun finishingHoursAreRoundedUp() {
        val shift = currentShift(endsAt = "2026-11-22T10:00:00Z")
        val tenMinutesIntoTheHour = Instant.parse("2026-11-22T12:10:00Z")
        assertTrue(barText(shift, ShiftPhase.FINISHING, tenMinutesIntoTheHour).endsWith("עוד 46 שעות"))
        val oneSecondAfterEnd = Instant.parse("2026-11-22T10:00:01Z")
        assertTrue(barText(shift, ShiftPhase.FINISHING, oneSecondAfterEnd).endsWith("עוד 48 שעות"))
    }

    @Test
    fun screenOpensOnlyOnAppStartWhileActive() {
        assertTrue(shouldOpenShiftScreen(ShiftPhase.ACTIVE, alreadyOpened = false))
        assertFalse(shouldOpenShiftScreen(ShiftPhase.ACTIVE, alreadyOpened = true))
    }

    @Test
    fun screenNeverOpensOutsideActive() {
        for (phase in listOf(ShiftPhase.LEGACY, ShiftPhase.PLANNED, ShiftPhase.FINISHING, ShiftPhase.CLOSED)) {
            assertFalse(shouldOpenShiftScreen(phase, alreadyOpened = false))
        }
    }

    @Test
    fun barShowsForActiveAndFinishingOnOtherScreens() {
        assertTrue(showBar(ShiftPhase.ACTIVE, onShiftScreenOfThisShift = false))
        assertTrue(showBar(ShiftPhase.FINISHING, onShiftScreenOfThisShift = false))
    }

    @Test
    fun barHidesOnTheScreenOfThatShift() {
        assertFalse(showBar(ShiftPhase.ACTIVE, onShiftScreenOfThisShift = true))
        assertFalse(showBar(ShiftPhase.FINISHING, onShiftScreenOfThisShift = true))
    }

    @Test
    fun barHidesForPlannedClosedAndLegacy() {
        for (phase in listOf(ShiftPhase.PLANNED, ShiftPhase.CLOSED, ShiftPhase.LEGACY)) {
            assertFalse(showBar(phase, onShiftScreenOfThisShift = false))
        }
    }

    private fun crewCanDelete(
        phase: ShiftPhase = ShiftPhase.ACTIVE,
        empty: Boolean = true,
        done: Boolean = false,
    ) = canDeleteShiftEvent(canManage = false, phase = phase, empty = empty, done = done)

    @Test
    fun crewDeletesAnEmptyOpenEventWhileTheWindowIsOpen() {
        assertTrue(crewCanDelete(ShiftPhase.ACTIVE))
        assertTrue(crewCanDelete(ShiftPhase.FINISHING))
    }

    @Test
    fun crewCannotDeleteAFilledEvent() {
        assertFalse(crewCanDelete(empty = false))
    }

    @Test
    fun crewCannotDeleteADoneEvent() {
        assertFalse(crewCanDelete(done = true))
    }

    @Test
    fun crewCannotDeleteOutsideTheWindow() {
        assertFalse(crewCanDelete(ShiftPhase.CLOSED))
        assertFalse(crewCanDelete(ShiftPhase.PLANNED))
        assertFalse(crewCanDelete(ShiftPhase.LEGACY))
    }

    @Test
    fun leadsAndAdminsAlwaysDelete() {
        for (phase in ShiftPhase.values()) {
            assertTrue(canDeleteShiftEvent(canManage = true, phase = phase, empty = false, done = true))
        }
    }

    @Test
    fun primaryTypesKeepTheirFixedOrder() {
        assertEquals(listOf("תאונה", "תקוע", "נטוש", "הפוך"), NEW_EVENT_PRIMARY_TYPES)
    }

    private val types = listOf("שריפה", "הפוך", "תקוע", "אחר", "תאונה", "נטוש")

    @Test
    fun primaryTypesComeFirstInFixedOrder() {
        val ordered = orderEventTypesForNewEvent(types) { it }
        assertEquals(listOf("תאונה", "תקוע", "נטוש", "הפוך"), ordered.primary)
    }

    @Test
    fun otherTypesKeepListOrder() {
        val ordered = orderEventTypesForNewEvent(types) { it }
        assertEquals(listOf("שריפה", "אחר"), ordered.more)
    }

    @Test
    fun aMissingPrimaryTypeIsSkipped() {
        val ordered = orderEventTypesForNewEvent(types.filter { it != "נטוש" }) { it }
        assertEquals(listOf("תאונה", "תקוע", "הפוך"), ordered.primary)
    }
}
