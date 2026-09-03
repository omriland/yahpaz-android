package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CockpitTest {
    private val now = Instant.parse("2026-08-16T12:00:00.000Z")

    private fun event(
        id: String,
        createdAt: String,
        status: EventStatus = EventStatus.IN_PROGRESS,
        isCancelled: Boolean = false,
        policeEventId: String? = "12345",
        location: String? = "מחלף",
        roadName: String? = "כביש 20",
        locationLat: Double? = null,
        locationLng: Double? = null,
        responders: List<CockpitResponderInput> = emptyList(),
    ) = CockpitEventInput(
        id = id,
        createdAt = createdAt,
        policeEventId = policeEventId,
        status = status,
        isCancelled = isCancelled,
        location = location,
        locationLat = locationLat,
        locationLng = locationLng,
        eventTypeName = "תאונה",
        roadName = roadName,
        leadFullName = "עמרי לנדמן",
        leadCallsign = "Admin",
        responders = responders,
    )

    @Test
    fun windowKeepsLastTwoHoursOnly() {
        assertTrue(isInCockpitWindow("2026-08-16T10:00:00.000Z", now))
        assertTrue(isInCockpitWindow("2026-08-16T11:59:00.000Z", now))
        assertFalse(isInCockpitWindow("2026-08-16T09:59:59.000Z", now))
        assertFalse(isInCockpitWindow("2026-08-16T12:00:01.000Z", now))
        assertEquals(2L * 60L * 60L * 1000L, COCKPIT_WINDOW_MS)
    }

    @Test
    fun filterKeepsOpenRecentAndSortsNewestFirst() {
        val rows = filterCockpitEvents(
            listOf(
                event("old", "2026-08-16T10:10:00.000Z"),
                event("stale", "2026-08-16T09:00:00.000Z"),
                event("new", "2026-08-16T11:50:00.000Z"),
                event("done", "2026-08-16T11:40:00.000Z", status = EventStatus.DONE),
                event("draft", "2026-08-16T11:45:00.000Z", status = EventStatus.DRAFT),
                event("cancelled", "2026-08-16T11:30:00.000Z", isCancelled = true),
                event("partial", "2026-08-16T11:20:00.000Z", status = EventStatus.PARTIAL),
            ),
            now,
        )
        assertEquals(listOf("new", "partial", "old"), rows.map { it.id })
    }

    @Test
    fun titlesPlacesAndLeadsMatchWeb() {
        assertEquals("12345", cockpitReelTitle("12345"))
        assertEquals(COCKPIT_NEW_EVENT_TITLE, cockpitReelTitle("  "))
        assertEquals(COCKPIT_NEW_EVENT_TITLE, cockpitReelTitle(null))
        assertEquals("כביש 20 · מחלף השלום", cockpitReelPlace("כביש 20", "מחלף השלום"))
        assertNull(cockpitReelPlace(null, null))
        assertEquals(
            CockpitLead("עמרי לנדמן", "Admin"),
            cockpitReelLead("עמרי לנדמן", "Admin"),
        )
        assertNull(cockpitReelLead("  ", "  "))
        assertEquals(
            "תאונה · כביש 20 · מחלף",
            cockpitReelDetail("תאונה", "כביש 20", "מחלף"),
        )
    }

    @Test
    fun geocodeQueryPutsRoadNumberFirst() {
        assertEquals("כביש 20 מחלף השלום", eventGeocodeQuery("כביש 20", "מחלף השלום"))
        assertEquals("כביש 101 דיזנגוף", eventGeocodeQuery("עירוני (101)", "דיזנגוף"))
        assertEquals("כביש החוף נתניה", eventGeocodeQuery("כביש החוף", "נתניה"))
        assertEquals("הרצל 1 תל אביב", eventGeocodeQuery(null, "הרצל 1 תל אביב"))
        assertEquals("כביש 4", eventGeocodeQuery("כביש 4", null))
        assertNull(eventGeocodeQuery(null, null))
    }

    @Test
    fun mapsUrisPreferGeoThenWeb() {
        assertEquals("geo:32.07,34.79?q=32.07,34.79", cockpitMapsGeoUri(32.07, 34.79))
        assertNull(cockpitMapsGeoUri(null, 34.79))
        assertEquals(
            "https://maps.google.com/?q=32.07,34.79",
            cockpitMapsWebUri(32.07, 34.79, "כביש 20", "מחלף"),
        )
        assertEquals(
            "https://maps.google.com/?q=%D7%9B%D7%91%D7%99%D7%A9+20+%D7%9E%D7%97%D7%9C%D7%A3",
            cockpitMapsWebUri(null, null, "כביש 20", "מחלף"),
        )
        assertEquals(
            listOf(
                "geo:32.07,34.79?q=32.07,34.79",
                "https://maps.google.com/?q=32.07,34.79",
            ),
            cockpitMapsOpenUris(32.07, 34.79, null, null),
        )
        assertEquals(
            listOf("https://maps.google.com/?q=%D7%9B%D7%91%D7%99%D7%A9+4"),
            cockpitMapsOpenUris(null, null, "כביש 4", null),
        )
        assertTrue(cockpitMapsOpenUris(null, null, null, null).isEmpty())
    }

    @Test
    fun ageClockAndResponderSummary() {
        assertEquals("12:00", formatCockpitClock("2026-08-16T09:00:00.000Z"))
        assertEquals("עכשיו", formatCockpitAge("2026-08-16T11:59:30.000Z", now))
        assertEquals("לפני דקה", formatCockpitAge("2026-08-16T11:59:00.000Z", now))
        assertEquals("לפני 15 דק׳", formatCockpitAge("2026-08-16T11:45:00.000Z", now))
        assertEquals("אין מתנדבים משובצים", cockpitResponderSummary(emptyList()))
        assertEquals(
            "2 מתנדבים · 1 פעילים",
            cockpitResponderSummary(
                listOf(
                    CockpitResponderInput("a", endedAt = "2026-08-16T10:00:00.000Z"),
                    CockpitResponderInput("b", endedAt = null),
                ),
            ),
        )
        assertEquals(
            "1 מתנדבים · הסתיים",
            cockpitResponderSummary(
                listOf(CockpitResponderInput("a", endedAt = "2026-08-16T10:00:00.000Z")),
            ),
        )
        assertEquals("3 בחלון", cockpitWindowCountLabel(3))
    }

    @Test
    fun stillOpenOnMapAndOwnParticipation() {
        assertTrue(cockpitEventStillOpenOnMap(emptyList()))
        assertTrue(cockpitEventStillOpenOnMap(listOf(CockpitResponderInput("a", endedAt = null))))
        assertFalse(
            cockpitEventStillOpenOnMap(
                listOf(CockpitResponderInput("a", endedAt = "2026-08-16T10:00:00.000Z")),
            ),
        )
        assertEquals(
            ParticipationStatus.PENDING,
            cockpitOwnParticipation(
                listOf(
                    CockpitResponderInput("a", responderId = "u1", status = ParticipationStatus.PENDING),
                ),
                "u1",
            ),
        )
        assertNull(cockpitOwnParticipation(emptyList(), "u1"))
    }

    @Test
    fun searchFiltersByPoliceRoadLocationLead() {
        val rows = listOf(
            event("a", "2026-08-16T11:00:00.000Z", policeEventId = "111"),
            event("b", "2026-08-16T11:10:00.000Z", policeEventId = "222", roadName = "כביש 4"),
        )
        assertEquals(rows, filterCockpitEventsByQuery(rows, ""))
        assertEquals(listOf("b"), filterCockpitEventsByQuery(rows, "כביש 4").map { it.id })
        assertTrue(filterCockpitEventsByQuery(rows, "אין").isEmpty())
    }

    @Test
    fun deleteBlocksOnlyWhileRespondersAreAllocated() {
        val lead = CockpitDeleteViewer(userId = "lead-a", isAdmin = false)
        assertTrue(canDeleteCockpitDraft(0, "lead-a", lead))
        assertEquals(CockpitDeleteBlock.RESPONDERS, cockpitDeleteBlock(1, "lead-a", lead))
        assertNull(cockpitDeleteBlock(0, "lead-a", lead))
        assertEquals(COCKPIT_DELETE_RESPONDERS, cockpitDeleteHint(CockpitDeleteBlock.RESPONDERS))
        assertEquals(COCKPIT_DELETE_CONFIRM_AGAIN, cockpitDeleteHint(null))
        assertEquals(CockpitDeleteClick.Arm, cockpitDeleteClick(false, 0, "lead-a", lead))
        assertEquals(CockpitDeleteClick.Delete, cockpitDeleteClick(true, 0, "lead-a", lead))
        assertEquals(
            CockpitDeleteClick.Blocked(CockpitDeleteBlock.RESPONDERS),
            cockpitDeleteClick(false, 1, "lead-a", lead),
        )
    }

    @Test
    fun deleteBlocksShiftLeadFromAnotherLeadsEvent() {
        val otherLead = CockpitDeleteViewer(userId = "lead-a", isAdmin = false)
        assertEquals(CockpitDeleteBlock.OTHER_LEAD, cockpitDeleteBlock(0, "lead-b", otherLead))
        assertFalse(canDeleteCockpitDraft(0, "lead-b", otherLead))
        assertTrue(canDeleteCockpitDraft(0, "lead-a", otherLead))
        assertTrue(canDeleteCockpitDraft(0, "lead-b", CockpitDeleteViewer("admin", isAdmin = true)))
        assertFalse(shouldShowCockpitDelete(CockpitDeleteBlock.OTHER_LEAD))
        assertTrue(shouldShowCockpitDelete(CockpitDeleteBlock.RESPONDERS))
        assertTrue(shouldShowCockpitDelete(null))
        assertEquals(EVENT_DELETE_OTHER_LEAD, cockpitDeleteHint(CockpitDeleteBlock.OTHER_LEAD))
    }
}
