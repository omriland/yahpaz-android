package com.yahpz.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftLeadEventsWindowTest {
    private val now = Instant.parse("2026-11-04T12:00:00Z")

    private fun hoursAgo(hours: Long): String =
        now.minusMillis(hours * 60L * 60L * 1000L).toString()

    private fun complete() = IncompleteEventSnapshot(
        origin = "manual",
        policeEventId = "12345",
        patrolCallsignNumber = "12",
        startedAt = "2026-11-04T08:00:00Z",
        endedAt = "2026-11-04T09:00:00Z",
        hasDistrict = true,
        hasEventType = true,
        hasRoad = true,
        location = "מחלף אייל",
        responders = listOf(IncompleteResponderSnapshot(totalKm = 12.0)),
    )

    private fun pendingPoliceId() = complete().copy(policeEventId = null)

    @Test
    fun `window is a rolling 12 hours`() {
        assertEquals(12L * 60L * 60L * 1000L, SHIFT_LEAD_EVENTS_RECENT_MS)
        assertTrue(eventWithinShiftLeadRecentWindow(hoursAgo(12), now))
        assertFalse(
            eventWithinShiftLeadRecentWindow(
                now.minusMillis(SHIFT_LEAD_EVENTS_RECENT_MS + 1).toString(),
                now,
            ),
        )
    }

    @Test
    fun `event inside 12 hours stays`() {
        assertTrue(
            keepShiftLeadRecentEvent(
                createdAt = hoursAgo(1),
                now = now,
                viewerId = "lead",
                shiftLeadId = "other",
                secondaryLeadIds = emptyList(),
                incomplete = complete(),
            ),
        )
    }

    @Test
    fun `older complete event drops`() {
        assertFalse(
            keepShiftLeadRecentEvent(
                createdAt = hoursAgo(13),
                now = now,
                viewerId = "lead",
                shiftLeadId = "lead",
                secondaryLeadIds = emptyList(),
                incomplete = complete(),
            ),
        )
    }

    @Test
    fun `older event pending this lead stays`() {
        assertTrue(
            keepShiftLeadRecentEvent(
                createdAt = hoursAgo(30),
                now = now,
                viewerId = "lead",
                shiftLeadId = "lead",
                secondaryLeadIds = emptyList(),
                incomplete = pendingPoliceId(),
            ),
        )
    }

    @Test
    fun `older event pending a secondary lead stays for that lead`() {
        assertTrue(
            keepShiftLeadRecentEvent(
                createdAt = hoursAgo(30),
                now = now,
                viewerId = "lead",
                shiftLeadId = "other",
                secondaryLeadIds = listOf("lead"),
                incomplete = pendingPoliceId(),
            ),
        )
    }

    @Test
    fun `older event pending someone else drops`() {
        assertFalse(
            keepShiftLeadRecentEvent(
                createdAt = hoursAgo(30),
                now = now,
                viewerId = "lead",
                shiftLeadId = "other",
                secondaryLeadIds = listOf("sec"),
                incomplete = pendingPoliceId(),
            ),
        )
    }
}
