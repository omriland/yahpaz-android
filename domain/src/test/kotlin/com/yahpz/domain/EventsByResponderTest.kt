package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsByResponderTest {
    private fun event(
        id: String,
        date: String,
        responders: List<EventsByResponderResponderInput>,
        isCancelled: Boolean = false,
    ) = EventsByResponderEventInput(
        id = id,
        eventDate = date,
        isCancelled = isCancelled,
        policeEventId = "P-$id",
        location = "צומת",
        eventTypeName = "תקר",
        districtName = "מרכז",
        roadName = "6",
        leadName = "רון אחמש",
        leadCallsign = "A1",
        responders = responders,
    )

    @Test
    fun `each responder becomes its own row`() {
        val rows = buildEventsByResponderRows(
            listOf(
                event(
                    "e1",
                    "2026-02-02",
                    listOf(
                        EventsByResponderResponderInput("r1", 12.0, "דנה כהן", "12"),
                        EventsByResponderResponderInput("r2", null, "יוסי לוי", "44"),
                    ),
                ),
            ),
            from = "2026-01-01",
            to = "2026-03-01",
        )
        assertEquals(2, rows.size)
        assertEquals(listOf("e1:r1", "e1:r2"), rows.map { it.id })
        assertEquals(12.0, rows[0].totalKm)
        assertEquals(null, rows[1].totalKm)
    }

    @Test
    fun `rows sort by responder then newest event first`() {
        val rows = buildEventsByResponderRows(
            listOf(
                event("e1", "2026-01-05", listOf(EventsByResponderResponderInput("r2", 1.0, "יוסי לוי", "44"))),
                event("e2", "2026-01-01", listOf(EventsByResponderResponderInput("r1", 1.0, "דנה כהן", "12"))),
                event("e3", "2026-01-09", listOf(EventsByResponderResponderInput("r1", 1.0, "דנה כהן", "12"))),
            ),
            from = "2026-01-01",
            to = "2026-03-01",
        )
        assertEquals(listOf("e3:r1", "e2:r1", "e1:r2"), rows.map { it.id })
    }

    @Test
    fun `events outside the range drop out`() {
        val rows = buildEventsByResponderRows(
            listOf(
                event("in", "2026-02-10", listOf(EventsByResponderResponderInput("r1"))),
                event("early", "2026-01-31", listOf(EventsByResponderResponderInput("r1"))),
                event("late", "2026-03-01", listOf(EventsByResponderResponderInput("r1"))),
            ),
            from = "2026-02-01",
            to = "2026-02-28",
        )
        assertEquals(listOf("in:r1"), rows.map { it.id })
    }

    @Test
    fun `events with no responders contribute nothing`() {
        val rows = buildEventsByResponderRows(
            listOf(event("e1", "2026-02-02", emptyList())),
            from = "2026-01-01",
            to = "2026-03-01",
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `report rows carry km, place and cancelled stamp`() {
        val rows = eventsByResponderReportRows(
            buildEventsByResponderRows(
                listOf(
                    event(
                        "e1",
                        "2026-02-02",
                        listOf(EventsByResponderResponderInput("r1", 1234.0, "דנה כהן", "12")),
                        isCancelled = true,
                    ),
                ),
                from = "2026-01-01",
                to = "2026-03-01",
            ),
        )
        assertEquals(1, rows.size)
        assertEquals("דנה כהן · 12", rows[0].title)
        assertEquals("1,234 ק״מ", rows[0].trailing)
        assertEquals("בוטל", rows[0].stampLabel)
        assertEquals("e1", rows[0].eventId)
        assertTrue(rows[0].subtitle.contains("02.02.2026"))
        assertTrue(rows[0].detail!!.contains("אחמ״ש: A1 · רון אחמש"))
    }
}
