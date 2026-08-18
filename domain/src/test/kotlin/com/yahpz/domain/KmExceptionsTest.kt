package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KmExceptionsTest {
    private fun event(
        id: String,
        date: String,
        responders: List<KmExceptionResponderInput>,
    ) = KmExceptionEventInput(
        id = id,
        eventDate = date,
        policeEventId = "P-$id",
        location = "צומת",
        eventTypeName = "תקר",
        roadName = "6",
        leadName = "רון אחמש",
        leadCallsign = "A1",
        responders = responders,
    )

    @Test
    fun `only km at or above the threshold count`() {
        val rows = buildKmExceptionRows(
            listOf(
                event(
                    "e1",
                    "2026-02-02",
                    listOf(
                        KmExceptionResponderInput(59.9, "מתחת", "1"),
                        KmExceptionResponderInput(60.0, "בדיוק", "2"),
                        KmExceptionResponderInput(120.0, "מעל", "3"),
                        KmExceptionResponderInput(null, "בלי ק״מ", "4"),
                    ),
                ),
            ),
        )
        assertEquals(listOf("מעל", "בדיוק"), rows.map { it.responderName })
    }

    @Test
    fun `rows sort by newest date then largest km`() {
        val rows = buildKmExceptionRows(
            listOf(
                event("old", "2026-01-01", listOf(KmExceptionResponderInput(200.0, "א", "1"))),
                event("new", "2026-02-01", listOf(KmExceptionResponderInput(70.0, "ב", "2"))),
                event("new2", "2026-02-01", listOf(KmExceptionResponderInput(90.0, "ג", "3"))),
            ),
        )
        assertEquals(listOf("ג", "ב", "א"), rows.map { it.responderName })
    }

    @Test
    fun `range bounds are inclusive and optional`() {
        val events = listOf(
            event("in", "2026-02-10", listOf(KmExceptionResponderInput(90.0))),
            event("early", "2026-01-31", listOf(KmExceptionResponderInput(90.0))),
        )
        assertEquals(listOf("in"), buildKmExceptionRows(events, "2026-02-01", "2026-02-28").map { it.eventId })
        assertEquals(2, buildKmExceptionRows(events).size)
    }

    @Test
    fun `report rows show grouped km and unique ids`() {
        val rows = kmExceptionReportRows(
            buildKmExceptionRows(
                listOf(
                    event(
                        "e1",
                        "2026-02-02",
                        listOf(
                            KmExceptionResponderInput(1500.0, "דנה כהן", "12"),
                            KmExceptionResponderInput(1500.0, "יוסי לוי", "12"),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(2, rows.map { it.id }.toSet().size)
        assertEquals("1,500 ק״מ", rows[0].trailing)
        assertTrue(rows[0].detail!!.contains("כביש".let { "6" }))
    }
}
