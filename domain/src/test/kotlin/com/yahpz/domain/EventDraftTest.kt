package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDraftTest {
    private val roads = listOf(
        LookupOption("road-6", "6"),
        LookupOption("road-101", "עירוני (101)"),
    )
    private val districts = listOf(
        LookupOption("d-north", "צפון"),
        LookupOption("d-system", "תחנה / אחר / משוכפל", code = SYSTEM_DISTRICT_CODE),
    )

    private fun draft(
        date: String = "2026-02-02",
        typeId: String = "type-1",
        roadId: String = "road-6",
        districtId: String = "",
        location: String = "",
    ) = EventDraft(
        eventDate = date,
        eventTypeId = typeId,
        roadId = roadId,
        districtId = districtId,
        location = location,
    )

    @Test
    fun `date, type and road are the minimum`() {
        assertTrue(validateEventDraft(draft()).isEmpty)
        assertEquals(EVENT_DRAFT_DATE_ERROR, validateEventDraft(draft(date = "")).eventDate)
        assertEquals(EVENT_DRAFT_TYPE_ERROR, validateEventDraft(draft(typeId = "")).eventType)
        assertEquals(EVENT_DRAFT_ROAD_ERROR, validateEventDraft(draft(roadId = "")).road)
    }

    @Test
    fun `dd MM yyyy input is accepted as a date`() {
        assertTrue(validateEventDraft(draft(date = "02/02/2026")).isEmpty)
        assertEquals(EVENT_DRAFT_DATE_ERROR, validateEventDraft(draft(date = "32/02/2026")).eventDate)
    }

    @Test
    fun `the system district makes location mandatory`() {
        assertTrue(districtNeedsLocation(districts, "d-system"))
        assertFalse(districtNeedsLocation(districts, "d-north"))
        assertFalse(districtNeedsLocation(districts, ""))
        val errors = validateEventDraft(draft(districtId = "d-system"), districts)
        assertEquals(EVENT_DRAFT_LOCATION_ERROR, errors.location)
        assertEquals(EVENT_DRAFT_FORM_LOCATION_ERROR, errors.formMessage)
        assertTrue(validateEventDraft(draft(districtId = "d-system", location = "כיכר"), districts).isEmpty)
    }

    @Test
    fun `form message falls back to the non-location copy`() {
        assertEquals(EVENT_DRAFT_FORM_ERROR, validateEventDraft(draft(roadId = "")).formMessage)
        assertNull(validateEventDraft(draft()).formMessage)
    }

    @Test
    fun `crewless events save as drafts`() {
        assertEquals(EventStatus.DRAFT, eventDraftStatus(0))
        assertEquals(EventStatus.IN_PROGRESS, eventDraftStatus(1))
        assertTrue(eventDraftSummary(0).contains("טיוטה"))
        assertEquals("כונן אחד משובץ", eventDraftSummary(1))
        assertEquals("3 כוננים משובצים", eventDraftSummary(3))
    }

    @Test
    fun `the system district defaults to the 101 road`() {
        assertEquals("road-101", defaultRoadIdForSystemDistrict(roads))
        assertNull(defaultRoadIdForSystemDistrict(listOf(LookupOption("a", "6"))))
    }

    @Test
    fun `entering the system district preselects the 101 road`() {
        assertEquals(
            "road-101",
            applyDistrictRoadDefault("", "d-system", districts, roads, currentRoadId = ""),
        )
        assertEquals(
            "road-6",
            applyDistrictRoadDefault("", "d-north", districts, roads, currentRoadId = "road-6"),
        )
        assertEquals(
            "road-6",
            applyDistrictRoadDefault("d-system", "d-system", districts, roads, currentRoadId = "road-6"),
        )
    }

    @Test
    fun `event crew toggles without a ceiling`() {
        var crew = toggleEventResponder(emptyList(), "a")
        crew = toggleEventResponder(crew, "b")
        crew = toggleEventResponder(crew, "c")
        crew = toggleEventResponder(crew, "d")
        assertEquals(listOf("a", "b", "c", "d"), crew)
        assertEquals(listOf("a", "c", "d"), toggleEventResponder(crew, "b"))
    }

    @Test
    fun `assignable profiles filter by name and callsign`() {
        val profiles = listOf(
            AssignableProfile("r1", "דנה כהן", "12"),
            AssignableProfile("r2", "יוסי לוי", "44"),
        )
        assertEquals(listOf("r1"), filterAssignableProfiles(profiles, "דנה").map { it.id })
        assertEquals(listOf("r2"), filterAssignableProfiles(profiles, "44").map { it.id })
        assertEquals(2, filterAssignableProfiles(profiles, " ").size)
        assertEquals("דנה כהן · 12", profiles[0].display)
    }

    @Test
    fun `clearing the cancelled flag is admin only`() {
        assertNull(canToggleEventCancelled(next = true, viewerIsAdmin = false))
        assertNull(canToggleEventCancelled(next = false, viewerIsAdmin = true))
        assertEquals(EVENT_CANCEL_ADMIN_ONLY, canToggleEventCancelled(next = false, viewerIsAdmin = false))
    }

    @Test
    fun `cancel copy flips with the current flag`() {
        assertEquals("סימון האירוע כבוטל", eventCancelToggleLabel(isCancelled = false))
        assertEquals("ביטול סימון “בוטל”", eventCancelToggleLabel(isCancelled = true))
        assertEquals("האירוע סומן כבוטל.", eventCancelToast(isCancelled = true))
        assertEquals("סימון הביטול הוסר.", eventCancelToast(isCancelled = false))
    }
}
