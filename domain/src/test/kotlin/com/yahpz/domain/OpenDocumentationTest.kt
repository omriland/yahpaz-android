package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenDocumentationTest {
    private fun event(
        id: String,
        date: String,
        status: EventStatus = EventStatus.IN_PROGRESS,
        cancelled: Boolean = false,
        leadId: String = "lead-1",
        responders: List<OpenDocResponderInput> = listOf(
            OpenDocResponderInput("r-1", ParticipationStatus.PENDING, "דנה כהן", "12"),
        ),
    ) = OpenDocEventInput(
        id = id,
        eventDate = date,
        status = status,
        isCancelled = cancelled,
        shiftLeadId = leadId,
        leadName = "אופר לוי",
        leadCallsign = "7",
        responders = responders,
    )

    @Test
    fun `only open events and open participations produce rows`() {
        val rows = buildOpenDocRows(
            events = listOf(
                event("e-1", "2026-08-10"),
                event("e-2", "2026-08-11", status = EventStatus.DONE),
                event("e-3", "2026-08-12", cancelled = true),
                event(
                    "e-4",
                    "2026-08-13",
                    responders = listOf(OpenDocResponderInput("r-9", ParticipationStatus.DONE)),
                ),
            ),
            from = "2026-08-01",
            to = "2026-08-31",
            viewerId = "lead-1",
            viewerIsAdmin = false,
        )
        assertEquals(listOf("e-1"), rows.map { it.eventId })
        assertEquals("טרם הוזן", rows[0].fillStatusLabel)
        assertEquals("דנה כהן · 12", rows[0].responderDisplay)
        assertEquals("אופר לוי · 7", rows[0].leadDisplay)
    }

    @Test
    fun `a lead sees only their own events while an admin sees all`() {
        val events = listOf(event("mine", "2026-08-10"), event("theirs", "2026-08-11", leadId = "lead-2"))
        val leadRows = buildOpenDocRows(events, "2026-08-01", "2026-08-31", "lead-1", viewerIsAdmin = false)
        assertEquals(listOf("mine"), leadRows.map { it.eventId })
        val adminRows = buildOpenDocRows(events, "2026-08-01", "2026-08-31", "lead-1", viewerIsAdmin = true)
        assertEquals(listOf("theirs", "mine"), adminRows.map { it.eventId })
    }

    @Test
    fun `rows outside the range are dropped and drafts are labelled`() {
        val rows = buildOpenDocRows(
            events = listOf(
                event("old", "2026-07-01"),
                event(
                    "draft",
                    "2026-08-10",
                    status = EventStatus.PARTIAL,
                    responders = listOf(
                        OpenDocResponderInput("r-2", ParticipationStatus.IN_PROGRESS, "רון", "3"),
                    ),
                ),
            ),
            from = "2026-08-01",
            to = "2026-08-31",
            viewerId = "lead-1",
            viewerIsAdmin = true,
        )
        assertEquals(listOf("draft"), rows.map { it.eventId })
        assertEquals("נשמרה טיוטה", rows[0].fillStatusLabel)
        assertEquals("draft:r-2", rows[0].id)
    }

    @Test
    fun `summary counts in Hebrew`() {
        assertTrue(openDocSummary(0).isNotEmpty())
        assertEquals("דיווח אחד ממתין לתיעוד", openDocSummary(1))
        assertEquals("4 דיווחים ממתינים לתיעוד", openDocSummary(4))
    }
}
