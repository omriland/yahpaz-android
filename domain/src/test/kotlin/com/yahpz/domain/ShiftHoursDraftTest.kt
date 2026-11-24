package com.yahpz.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftHoursDraftTest {
    private fun draft(
        startTime: String = "",
        endTime: String = "",
        isLegacy: Boolean = false,
    ) = ShiftDraft(
        shiftDate = "2026-08-11",
        shiftKind = "morning",
        vehicleType = "patrol_north",
        responderIds = listOf("r1"),
        startTime = startTime,
        endTime = endTime,
        isLegacy = isLegacy,
    )

    @Test
    fun `a new shift without hours is refused with a hebrew message on both fields`() {
        val errors = validateShiftDraft(draft())
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, errors.startTime)
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, errors.endTime)
        assertTrue(SHIFT_HOURS_REQUIRED_ERROR.any { it in 'א'..'ת' })
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, errors.formMessage)
    }

    @Test
    fun `a new shift with only one time is refused on the missing one`() {
        val onlyStart = validateShiftDraft(draft(startTime = "07:00"))
        assertNull(onlyStart.startTime)
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, onlyStart.endTime)
        val onlyEnd = validateShiftDraft(draft(endTime = "15:00"))
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, onlyEnd.startTime)
        assertNull(onlyEnd.endTime)
    }

    @Test
    fun `a new shift with both times passes`() {
        assertTrue(validateShiftDraft(draft(startTime = "07:00", endTime = "15:00")).isEmpty)
    }

    @Test
    fun `an overnight shift passes because an earlier end means the next day`() {
        assertTrue(validateShiftDraft(draft(startTime = "23:00", endTime = "07:00")).isEmpty)
    }

    @Test
    fun `equal start and end is refused`() {
        val errors = validateShiftDraft(draft(startTime = "07:00", endTime = "07:00"))
        assertEquals(SHIFT_HOURS_EQUAL_ERROR, errors.endTime)
        assertNull(errors.startTime)
    }

    @Test
    fun `a legacy shift opened without hours still validates`() {
        assertTrue(validateShiftDraft(draft(isLegacy = true)).isEmpty)
    }

    @Test
    fun `hours missing together with other fields fall back to the general form message`() {
        val errors = validateShiftDraft(draft().copy(shiftKind = ""))
        assertEquals(SHIFT_DRAFT_FORM_ERROR, errors.formMessage)
    }

    @Test
    fun `typing a time clears only that fields error`() {
        val validated = draft()
        val errors = validateShiftDraft(validated)
        val cleared = errors.clearedFor(validated, validated.copy(startTime = "07:00"))
        assertNull(cleared.startTime)
        assertEquals(SHIFT_HOURS_REQUIRED_ERROR, cleared.endTime)
    }

    @Test
    fun `choosing a type fills empty hours`() {
        assertEquals(
            ShiftHours("07:00", "15:00"),
            fillDefaultShiftHours(ShiftHours("", ""), "morning"),
        )
        assertEquals(
            ShiftHours("15:00", "23:00"),
            fillDefaultShiftHours(ShiftHours("", ""), "midday"),
        )
    }

    @Test
    fun `choosing a type never overwrites typed hours`() {
        assertEquals(
            ShiftHours("08:30", "15:00"),
            fillDefaultShiftHours(ShiftHours("08:30", ""), "morning"),
        )
        assertEquals(
            ShiftHours("08:30", "16:45"),
            fillDefaultShiftHours(ShiftHours("08:30", "16:45"), "midday"),
        )
    }

    @Test
    fun `a type without default hours leaves the fields empty`() {
        assertEquals(ShiftHours("", ""), fillDefaultShiftHours(ShiftHours("", ""), "escort"))
    }
}

class CrewOverlapCheckTest {
    private val window = ShiftWindow(
        startsAt = Instant.parse("2026-08-11T04:00:00Z"),
        endsAt = Instant.parse("2026-08-11T12:00:00Z"),
    )
    private val crew = listOf(CrewMember("r1", "דנה"), CrewMember("r2", "יוסי"))
    private val clashing = CrewLiveShift(
        id = "o1",
        shiftKind = "reinforcement",
        startsAt = Instant.parse("2026-08-11T10:00:00Z"),
        endsAt = Instant.parse("2026-08-11T14:00:00Z"),
        responderIds = listOf("r2"),
    )
    private val expected = "לא ניתן לשבץ את יוסי: כבר משובץ במשמרת תגבור באותן שעות (13:00–17:00)."

    @Test
    fun `names the person, the shift type and its hours`() {
        assertEquals(expected, findCrewOverlapMessage(window, crew, listOf(clashing), null))
    }

    @Test
    fun `shifts that only touch at an edge are allowed`() {
        val after = clashing.copy(startsAt = window.endsAt, endsAt = Instant.parse("2026-08-11T20:00:00Z"))
        val before = clashing.copy(startsAt = Instant.parse("2026-08-10T20:00:00Z"), endsAt = window.startsAt)
        assertNull(findCrewOverlapMessage(window, crew, listOf(after, before), null))
    }

    @Test
    fun `overlapping shifts of people outside this crew are ignored`() {
        val stranger = clashing.copy(responderIds = listOf("x"))
        assertNull(findCrewOverlapMessage(window, crew, listOf(stranger), null))
    }

    @Test
    fun `the shift being edited is ignored but another clash is still reported`() {
        assertNull(findCrewOverlapMessage(window, crew, listOf(clashing), "o1"))
        val otherClash = clashing.copy(id = "o2")
        assertEquals(expected, findCrewOverlapMessage(window, crew, listOf(clashing, otherClash), "o1"))
    }

    @Test
    fun `no other shifts means no refusal`() {
        assertNull(findCrewOverlapMessage(window, crew, emptyList(), null))
    }
}
