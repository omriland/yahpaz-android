package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventIncompleteTest {
    private fun responder(
        totalKm: Double? = 12.0,
        startedAt: String? = "2026-09-04T06:00:00+03:00",
        endedAt: String? = "2026-09-04T07:00:00+03:00",
    ) = IncompleteResponderSnapshot(totalKm = totalKm, startedAt = startedAt, endedAt = endedAt)

    private fun event(
        policeEventId: String? = "12345",
        patrolCallsign: String? = "ניידת 1",
        hasDistrict: Boolean = true,
        hasEventType: Boolean = true,
        hasRoad: Boolean = true,
        location: String? = "מחלף אייל",
        responders: List<IncompleteResponderSnapshot> = listOf(responder()),
    ) = IncompleteEventSnapshot(
        policeEventId = policeEventId,
        patrolCallsign = patrolCallsign,
        hasDistrict = hasDistrict,
        hasEventType = hasEventType,
        hasRoad = hasRoad,
        location = location,
        responders = responders,
    )

    @Test
    fun `complete event has no missing fields`() {
        assertEquals(emptySet<IncompleteField>(), missingEventFields(event()))
        assertFalse(isEventIncomplete(event()))
    }

    @Test
    fun `flags each event-level required field`() {
        assertEquals(setOf(IncompleteField.POLICE_EVENT_ID), missingEventFields(event(policeEventId = null)))
        assertEquals(setOf(IncompleteField.POLICE_EVENT_ID), missingEventFields(event(policeEventId = "   ")))
        assertEquals(setOf(IncompleteField.PATROL_CALLSIGN), missingEventFields(event(patrolCallsign = null)))
        assertEquals(setOf(IncompleteField.DISTRICT), missingEventFields(event(hasDistrict = false)))
        assertEquals(setOf(IncompleteField.EVENT_TYPE), missingEventFields(event(hasEventType = false)))
        assertEquals(setOf(IncompleteField.ROAD), missingEventFields(event(hasRoad = false)))
        assertEquals(setOf(IncompleteField.LOCATION), missingEventFields(event(location = null)))
        assertEquals(setOf(IncompleteField.LOCATION), missingEventFields(event(location = "  ")))
    }

    @Test
    fun `KM zero counts as filled and null is missing`() {
        assertEquals(
            setOf(IncompleteField.RESPONDER_KM),
            missingEventFields(event(responders = listOf(responder(totalKm = null)))),
        )
        assertEquals(true, eventHasMissingResponderKm(event(responders = listOf(responder(totalKm = null)))))
        assertEquals(emptySet<IncompleteField>(), missingEventFields(event(responders = listOf(responder(totalKm = 0.0)))))
        assertEquals(false, eventHasMissingResponderKm(event(responders = listOf(responder(totalKm = 0.0)))))
        assertEquals(
            setOf(IncompleteField.RESPONDER_KM),
            missingEventFields(event(responders = listOf(responder(), responder(totalKm = null)))),
        )
    }

    @Test
    fun `flags times when any responder is missing start or end`() {
        assertEquals(
            setOf(IncompleteField.RESPONDER_TIMES),
            missingEventFields(event(responders = listOf(responder(startedAt = null)))),
        )
        assertEquals(
            setOf(IncompleteField.RESPONDER_TIMES),
            missingEventFields(event(responders = listOf(responder(endedAt = "  ")))),
        )
    }

    @Test
    fun `still flags events waiting for documentation`() {
        assertTrue(isEventIncomplete(event(policeEventId = null)))
    }

    @Test
    fun `Hebrew labels stay short and ordered`() {
        val fields = setOf(IncompleteField.RESPONDER_KM, IncompleteField.POLICE_EVENT_ID)
        assertEquals(listOf("מספר אירוע", "ק״מ"), incompleteFieldLabels(fields))
        assertEquals("חסרים: מספר אירוע · ק״מ", incompleteNoticeLabel(fields))
        assertEquals("ק״מ", INCOMPLETE_FIELD_LABELS[IncompleteField.RESPONDER_KM])
        assertEquals("שעות", INCOMPLETE_FIELD_LABELS[IncompleteField.RESPONDER_TIMES])
        assertEquals("דורשים השלמת פרטים", INCOMPLETE_EVENTS_HEADING)
        assertEquals("פרטים חסרים:", INCOMPLETE_NOTICE_MARK)
    }

    @Test
    fun `partition pins incomplete first and keeps input order`() {
        val complete = "ok" to event()
        val incomplete = "gap" to event(location = null)
        val (pinned, rest) = partitionIncompleteEvents(listOf(complete, incomplete)) { it.second }
        assertEquals(listOf(incomplete), pinned)
        assertEquals(listOf(complete), rest)
    }
}
