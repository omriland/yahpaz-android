package com.yahpz.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun leadsAndAdminsDeleteAnEmptyEventInAnyPhaseDoneOrNot() {
        for (phase in ShiftPhase.values()) {
            assertTrue(canDeleteShiftEvent(canManage = true, phase = phase, empty = true, done = false))
            assertTrue(canDeleteShiftEvent(canManage = true, phase = phase, empty = true, done = true))
        }
    }

    @Test
    fun leadsAndAdminsNeverDeleteAFilledEvent() {
        for (phase in ShiftPhase.values()) {
            assertFalse(canDeleteShiftEvent(canManage = true, phase = phase, empty = false, done = false))
            assertFalse(canDeleteShiftEvent(canManage = true, phase = phase, empty = false, done = true))
        }
    }

    private val shiftEnd = Instant.parse("2026-11-22T13:00:00Z")

    @Test
    fun activeTimeLeftCountsTheHoursUntilTheShiftEnds() {
        val now = Instant.parse("2026-11-22T10:00:00Z")
        assertEquals("עוד 3 שעות", shiftTimeLeftLabel(ShiftPhase.ACTIVE, shiftEnd, now))
    }

    @Test
    fun activeTimeLeftRoundsUpAndSaysOneHourInTheLastHour() {
        val fiftyMinutesLeft = Instant.parse("2026-11-22T12:10:00Z")
        assertEquals("עוד שעה", shiftTimeLeftLabel(ShiftPhase.ACTIVE, shiftEnd, fiftyMinutesLeft))
        val threeHoursTenLeft = Instant.parse("2026-11-22T09:50:00Z")
        assertEquals("עוד 4 שעות", shiftTimeLeftLabel(ShiftPhase.ACTIVE, shiftEnd, threeHoursTenLeft))
    }

    @Test
    fun finishingTimeLeftCountsTheHoursToDocumentUntilFortyEightHoursAfterTheEnd() {
        val twoHoursAfterEnd = Instant.parse("2026-11-22T15:00:00Z")
        assertEquals("עוד 46 שעות", shiftTimeLeftLabel(ShiftPhase.FINISHING, shiftEnd, twoHoursAfterEnd))
    }

    @Test
    fun noTimeLeftOutsideActiveAndFinishing() {
        val now = Instant.parse("2026-11-22T10:00:00Z")
        for (phase in listOf(ShiftPhase.LEGACY, ShiftPhase.PLANNED, ShiftPhase.CLOSED)) {
            assertNull(shiftTimeLeftLabel(phase, shiftEnd, now))
        }
    }

    @Test
    fun liveStampSaysOnShiftWhileActive() {
        assertEquals(LIVE_SHIFT_ACTIVE_STAMP, liveShiftStamp(ShiftPhase.ACTIVE, ShiftStatus.IN_PROGRESS).label)
        assertEquals("במשמרת", LIVE_SHIFT_ACTIVE_STAMP)
    }

    @Test
    fun liveStampSaysPlannedBeforeTheShift() {
        assertEquals(LIVE_SHIFT_PLANNED_STAMP, liveShiftStamp(ShiftPhase.PLANNED, ShiftStatus.DRAFT).label)
        assertEquals("מתוכננת", LIVE_SHIFT_PLANNED_STAMP)
    }

    @Test
    fun liveStampAfterTheShiftIsTheOrdinaryShiftStamp() {
        for (phase in listOf(ShiftPhase.FINISHING, ShiftPhase.CLOSED)) {
            for (status in ShiftStatus.values()) {
                assertEquals(shiftStamp(status), liveShiftStamp(phase, status))
            }
        }
    }

    @Test
    fun eventsTitleCarriesTheCount() {
        assertEquals("אירועי המשמרת (0)", liveShiftEventsTitle(0))
        assertEquals("אירועי המשמרת (3)", liveShiftEventsTitle(3))
    }

    @Test
    fun eventTypeLabelMarksAShiftEventAndHandlesAMissingName() {
        assertEquals("תאונה (משמרת)", liveShiftEventTypeLabel("תאונה"))
        assertEquals("(משמרת)", liveShiftEventTypeLabel(""))
    }

    @Test
    fun liveShiftTextsAreExact() {
        assertEquals("אירוע חדש", LIVE_SHIFT_NEW_EVENT_ACTION)
        assertEquals("קילומטרים", LIVE_SHIFT_ODOMETER_ACTION)
        assertEquals("למשמרת", ON_SHIFT_BAR_ACTION)
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
