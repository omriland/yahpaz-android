package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftDraftTest {
    private fun draft(
        date: String = "2026-02-02",
        kind: String = "morning",
        vehicleType: String = "patrol_north",
        crew: List<String> = listOf("r1"),
    ) = ShiftDraft(shiftDate = date, shiftKind = kind, vehicleType = vehicleType, responderIds = crew)

    @Test
    fun `date, kind, vehicle and crew are required`() {
        assertTrue(validateShiftDraft(draft()).isEmpty)
        assertEquals(SHIFT_DRAFT_DATE_ERROR, validateShiftDraft(draft(date = "")).shiftDate)
        assertEquals(SHIFT_DRAFT_KIND_ERROR, validateShiftDraft(draft(kind = "")).shiftKind)
        assertEquals(SHIFT_DRAFT_VEHICLE_ERROR, validateShiftDraft(draft(vehicleType = "")).vehicleType)
    }

    @Test
    fun `crew must be one to three`() {
        assertEquals(SHIFT_DRAFT_CREW_ERROR, validateShiftDraft(draft(crew = emptyList())).crew)
        assertNull(validateShiftDraft(draft(crew = listOf("a", "b", "c"))).crew)
        assertEquals(SHIFT_DRAFT_CREW_ERROR, validateShiftDraft(draft(crew = listOf("a", "b", "c", "d"))).crew)
    }

    @Test
    fun `crew-only failure reports the crew message`() {
        assertEquals(SHIFT_DRAFT_CREW_ERROR, validateShiftDraft(draft(crew = emptyList())).formMessage)
        assertEquals(SHIFT_DRAFT_FORM_ERROR, validateShiftDraft(draft(kind = "", crew = emptyList())).formMessage)
        assertNull(validateShiftDraft(draft()).formMessage)
    }

    @Test
    fun `toggling crew respects the ceiling`() {
        var crew = toggleCrewSelection(emptyList(), "a")
        crew = toggleCrewSelection(crew, "b")
        crew = toggleCrewSelection(crew, "c")
        assertEquals(listOf("a", "b", "c"), crew)
        assertEquals(crew, toggleCrewSelection(crew, "d"))
        assertEquals(listOf("a", "c"), toggleCrewSelection(crew, "b"))
    }

    @Test
    fun `every offered kind and vehicle type has a hebrew label`() {
        SHIFT_KIND_ORDER.forEach { kind ->
            assertEquals(SHIFT_KIND_LABELS.getValue(kind), shiftKindLabel(kind))
        }
        SHIFT_VEHICLE_TYPE_ORDER.forEach { type ->
            assertEquals(VEHICLE_TYPE_LABELS.getValue(type), shiftVehicleTypeLabel(type))
        }
        assertEquals("unknown", shiftKindLabel("unknown"))
    }

    @Test
    fun `crew summary counts in hebrew`() {
        assertEquals(SHIFT_DRAFT_CREW_ERROR, shiftCrewSummary(0))
        assertEquals("כונן אחד משובץ", shiftCrewSummary(1))
        assertEquals("2 כוננים משובצים", shiftCrewSummary(2))
    }
}
