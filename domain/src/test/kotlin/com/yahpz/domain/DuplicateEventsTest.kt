package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateEventsTest {
    private fun participation(
        eventId: String,
        responderId: String = "r1",
        date: String = "2026-04-02",
        location: String? = "מחלף גלילות",
        startedAt: String? = "2026-04-02T08:00:00",
    ) = DuplicateParticipation(
        eventId = eventId,
        responderId = responderId,
        eventDate = date,
        location = location,
        startedAt = startedAt,
        policeEventId = "900$eventId",
        eventTypeName = "פינוי",
        roadName = "כביש 2",
        name = "דנה כהן",
        callsign = "12",
    )

    @Test
    fun `two events within the window cluster as a pair`() {
        val clusters = buildDuplicateClusters(
            listOf(
                participation("a"),
                participation("b", startedAt = "2026-04-02T08:20:00"),
            ),
        )
        assertEquals(1, clusters.size)
        assertEquals("כפול", clusters[0].sizeLabel)
        assertEquals(listOf("a", "b"), clusters[0].members.map { it.eventId })
    }

    @Test
    fun `matching is transitive so a chain becomes one triple`() {
        val clusters = buildDuplicateClusters(
            listOf(
                participation("a", startedAt = "2026-04-02T08:00:00"),
                participation("b", startedAt = "2026-04-02T08:25:00"),
                participation("c", startedAt = "2026-04-02T08:50:00"),
            ),
        )
        assertEquals(1, clusters.size)
        assertEquals("משולש", clusters[0].sizeLabel)
        assertEquals(3, clusters[0].members.size)
    }

    @Test
    fun `outside the window, another responder, or another place is not a duplicate`() {
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a"), participation("b", startedAt = "2026-04-02T09:01:00")),
            ).isEmpty(),
        )
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a"), participation("b", responderId = "r2")),
            ).isEmpty(),
        )
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a"), participation("b", location = "מחלף חולון")),
            ).isEmpty(),
        )
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a"), participation("b", date = "2026-04-03")),
            ).isEmpty(),
        )
    }

    @Test
    fun `a missing place or start time cannot match`() {
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a", location = "   "), participation("b", location = null)),
            ).isEmpty(),
        )
        assertTrue(
            buildDuplicateClusters(
                listOf(participation("a", startedAt = null), participation("b", startedAt = null)),
            ).isEmpty(),
        )
    }

    @Test
    fun `location whitespace does not break a match`() {
        val clusters = buildDuplicateClusters(
            listOf(
                participation("a", location = " מחלף גלילות "),
                participation("b", startedAt = "2026-04-02T08:10:00"),
            ),
        )
        assertEquals(1, clusters.size)
    }

    @Test
    fun `clusters sort newest first and triples before pairs`() {
        val clusters = buildDuplicateClusters(
            listOf(
                participation("old1", date = "2026-03-01", startedAt = "2026-03-01T07:00:00"),
                participation("old2", date = "2026-03-01", startedAt = "2026-03-01T07:10:00"),
                participation("new1"),
                participation("new2", startedAt = "2026-04-02T08:10:00"),
            ),
        )
        assertEquals(2, clusters.size)
        assertEquals("2026-04-02", clusters[0].eventDate)
        assertEquals("2026-03-01", clusters[1].eventDate)
    }

    @Test
    fun `report rows stamp the cluster size and stay searchable`() {
        val rows = duplicateEventsReportRows(
            buildDuplicateClusters(
                listOf(
                    participation("a"),
                    participation("b", startedAt = "2026-04-02T08:20:00"),
                ),
            ),
        )
        assertEquals(2, rows.size)
        assertEquals("דנה כהן · 12", rows[0].title)
        assertEquals("02.04.2026 · 08:00 · פינוי", rows[0].subtitle)
        assertEquals("כפול", rows[0].stampLabel)
        assertEquals("כביש 2 · מחלף גלילות · 900a", rows[0].detail)
        assertEquals(2, filterReportRows(rows, "גלילות").size)
        assertEquals(1, filterReportRows(rows, "900b").size)
    }

    @Test
    fun `duplicate events is open to shift leads and hides the date range`() {
        val spec = reportSpec(ReportKindId.DUPLICATE_EVENTS)
        assertEquals(ReportAudience.MANAGES_UNIT, spec.audience)
        assertFalse(spec.hasDateRange)
    }

    @Test
    fun `wall time formatting reads the string without shifting zones`() {
        assertEquals("08:00", formatTime("2026-04-02T08:00:00"))
        assertEquals("08:00", formatTime("2026-04-02 08:00:00+03"))
        assertEquals(null, formatTime(null))
        assertEquals(null, formatTime("   "))
    }
}
