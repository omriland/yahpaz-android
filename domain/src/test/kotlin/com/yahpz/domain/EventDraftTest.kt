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
    fun `district lookups follow sort_order not name`() {
        val shuffled = listOf(
            LookupOption("c", "צפון", sortOrder = 2),
            LookupOption("a", "דרום", sortOrder = 1),
            LookupOption("b", "אבן", sortOrder = 3),
        )
        assertEquals(
            listOf("a", "c", "b"),
            sortLookupsBySortOrder(shuffled).map { it.id },
        )
    }

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
        assertEquals("טרם הוקצו מתנדבים · אירוע בהזנה", eventDraftSummary(0))
        assertEquals("מתנדב אחד משובץ", eventDraftSummary(1))
        assertEquals("3 מתנדבים משובצים", eventDraftSummary(3))
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
        assertEquals(listOf("a", "b", "c", "d"), crew.map { it.responderId })
        assertEquals(listOf("a", "c", "d"), toggleEventResponder(crew, "b").map { it.responderId })
    }

    @Test
    fun `create blocks the lead from assigning themselves`() {
        val crew = listOf(EventResponderDraft("lead"), EventResponderDraft("r2"))
        assertTrue(createIncludesSelfAssign("lead", crew))
        assertFalse(createIncludesSelfAssign("lead", listOf(EventResponderDraft("r2"))))
        assertTrue(isSelfAssignDisabledOnCreate(true, "me", "me"))
        assertFalse(isSelfAssignDisabledOnCreate(false, "me", "me"))
        assertFalse(isSelfAssignDisabledOnCreate(true, "me", "other"))
        assertEquals("לא ניתן לשבץ את יוצר האירוע כמתנדב.", EVENT_SELF_ASSIGN_ON_CREATE_ERROR)
        assertEquals("לא ניתן לשבץ", EVENT_SELF_ASSIGN_DISABLED_HINT)
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
    fun `clearing the cancelled flag is allowed for unit managers`() {
        assertNull(canToggleEventCancelled(next = true, canClearCancelled = false))
        assertNull(canToggleEventCancelled(next = false, canClearCancelled = true))
        assertEquals(EVENT_CANCEL_ADMIN_ONLY, canToggleEventCancelled(next = false, canClearCancelled = false))
    }

    @Test
    fun `cancel copy is the web checkbox label`() {
        assertEquals("בוטל", EVENT_CANCELLED_LABEL)
        assertEquals("בוטל", eventCancelToggleLabel(isCancelled = false))
        assertEquals("בוטל", eventCancelToggleLabel(isCancelled = true))
        assertEquals("האירוע סומן כבוטל.", eventCancelToast(isCancelled = true))
        assertEquals("סימון הביטול הוסר.", eventCancelToast(isCancelled = false))
        assertEquals("רק מנהל או אחמ״ש יכולים לבטל סימון בוטל.", EVENT_CANCEL_ADMIN_ONLY)
    }

    @Test
    fun `event form copy matches the web`() {
        assertEquals("אירוע חדש", EVENT_NEW_TITLE)
        assertEquals("עריכת אירוע", EVENT_EDIT_TITLE)
        assertEquals("שמירת אירוע", EVENT_SAVE_TITLE)
        assertEquals("שמירת טיוטה", EVENT_SAVE_DRAFT_TITLE)
        assertEquals("האירועים הפעילים שלי", MY_ACTIVE_EVENTS_TITLE)
        assertEquals("מתנדבים", EVENT_ASSIGN_OPEN)
        assertEquals("סגירת הקצאה", EVENT_ASSIGN_CLOSE)
        assertEquals("הסרת מתנדב", EVENT_ASSIGN_REMOVE)
        assertEquals("או״ק ניידת", EVENT_PATROL_CALLSIGN_LABEL)
        assertEquals("טעינת האירועים נכשלה. בדקו את החיבור ונסו שוב.", UNIT_EVENTS_LOAD_FAILED)
    }

    @Test
    fun `draft save only requires a date`() {
        assertTrue(validateEventDraftPartial(EventDraft(eventDate = "2026-02-02")).isEmpty)
        assertEquals(
            EVENT_DRAFT_DATE_ERROR,
            validateEventDraftPartial(EventDraft(eventDate = "")).eventDate,
        )
    }

    @Test
    fun `fill ready notify fires on assignment even without km`() {
        assertEquals(
            listOf("new"),
            fillReadyNotifyIds(
                previous = emptyList(),
                next = listOf(FillReadyNextRow("new", null)),
            ),
        )
    }

    @Test
    fun `own same-day police id is resumed when create has no id yet`() {
        val mine = SameDayPoliceEventRow(id = "evt-mine", shiftLeadId = "lead-1")
        assertEquals(
            "evt-mine",
            ownResumableEventId(currentEventId = null, viewerLeadId = "lead-1", existing = listOf(mine)),
        )
        assertNull(
            ownResumableEventId(
                currentEventId = null,
                viewerLeadId = "lead-1",
                existing = listOf(mine.copy(shiftLeadId = "other")),
            ),
        )
        assertNull(
            ownResumableEventId(currentEventId = "evt-mine", viewerLeadId = "lead-1", existing = listOf(mine)),
        )
    }

    @Test
    fun `fill ready notify still fires when an existing assignment first gets km`() {
        assertEquals(
            listOf("a"),
            fillReadyNotifyIds(
                previous = listOf(FillReadyPreviousRow("a", null)),
                next = listOf(FillReadyNextRow("a", 8.0)),
            ),
        )
    }

    @Test
    fun `תחנה saves only on the system שלוחה`() {
        assertEquals("איילון", stationForSave(districts, "d-system", "  איילון  "))
        assertNull(stationForSave(districts, "d-system", "   "))
        assertNull(stationForSave(districts, "d-north", "איילון"))
        assertEquals("", stationAfterDistrictChange(districts, "d-north", "איילון"))
        assertEquals("איילון", stationAfterDistrictChange(districts, "d-system", "איילון"))
        val long = "א".repeat(STATION_MAX_LENGTH + 10)
        assertEquals(STATION_MAX_LENGTH, stationForSave(districts, "d-system", long)?.length)
    }
}
