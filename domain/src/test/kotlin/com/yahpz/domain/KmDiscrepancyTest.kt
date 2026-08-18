package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KmDiscrepancyTest {
    private fun responder(
        id: String,
        status: ParticipationStatus = ParticipationStatus.DONE,
        totalKm: Double? = 40.0,
        start: Double? = 1_000.0,
        end: Double? = 1_050.0,
        name: String = "דנה כהן",
    ) = KmDiscrepancyResponderInput(
        assignmentId = id,
        status = status,
        totalKm = totalKm,
        odometerStart = start,
        odometerEnd = end,
        name = name,
        callsign = "12",
    )

    private fun event(
        id: String,
        date: String = "2026-03-10",
        responders: List<KmDiscrepancyResponderInput>,
    ) = KmDiscrepancyEventInput(
        id = id,
        eventDate = date,
        policeEventId = "555",
        location = "צומת",
        roadName = "כביש 6",
        leadName = "יוסי לוי",
        leadCallsign = "7",
        responders = responders,
    )

    @Test
    fun `a gap between lead km and the odometer becomes a row`() {
        val rows = buildKmDiscrepancyRows(listOf(event("e1", responders = listOf(responder("a")))))
        assertEquals(1, rows.size)
        assertEquals(40.0, rows[0].leadKm, 0.0)
        assertEquals(50.0, rows[0].responderKm, 0.0)
        assertEquals(10.0, rows[0].diff, 0.0)
    }

    @Test
    fun `aligned, unreported and unfinished participations are skipped`() {
        val rows = buildKmDiscrepancyRows(
            listOf(
                event("aligned", responders = listOf(responder("a", totalKm = 50.0))),
                event("noLeadKm", responders = listOf(responder("b", totalKm = null))),
                event("noOdometer", responders = listOf(responder("c", end = null))),
                event(
                    "openDoc",
                    responders = listOf(responder("d", status = ParticipationStatus.IN_PROGRESS)),
                ),
            ),
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `rows honour the requested range`() {
        val events = listOf(
            event("old", date = "2026-01-01", responders = listOf(responder("a"))),
            event("new", date = "2026-03-10", responders = listOf(responder("b"))),
        )
        val rows = buildKmDiscrepancyRows(events, from = "2026-02-01", to = "2026-03-31")
        assertEquals(listOf("new"), rows.map { it.eventId })
    }

    @Test
    fun `sort is newest first then the widest gap`() {
        val events = listOf(
            event(
                "same-day",
                responders = listOf(
                    responder("small", end = 1_005.0, name = "אבי"),
                    responder("big", end = 1_200.0, name = "בני"),
                ),
            ),
            event("older", date = "2026-03-01", responders = listOf(responder("older"))),
        )
        val rows = buildKmDiscrepancyRows(events)
        assertEquals(listOf("big", "small", "older"), rows.map { it.assignmentId })
    }

    @Test
    fun `replacement resolves to the odometer difference`() {
        assertEquals(
            LeadKmReplacement.Replace(50.0),
            resolveLeadKmReplacement(totalKm = 40.0, odometerStart = 1_000.0, odometerEnd = 1_050.0),
        )
        assertEquals(
            LeadKmReplacement.AlreadyAligned,
            resolveLeadKmReplacement(totalKm = 50.0, odometerStart = 1_000.0, odometerEnd = 1_050.0),
        )
        assertEquals(
            LeadKmReplacement.Invalid,
            resolveLeadKmReplacement(totalKm = null, odometerStart = 1_000.0, odometerEnd = 1_050.0),
        )
        assertEquals(
            LeadKmReplacement.Invalid,
            resolveLeadKmReplacement(totalKm = 40.0, odometerStart = null, odometerEnd = 1_050.0),
        )
    }

    @Test
    fun `report rows carry the apply action and are searchable`() {
        val rows = kmDiscrepancyReportRows(
            buildKmDiscrepancyRows(listOf(event("e1", responders = listOf(responder("a"))))),
        )
        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("דנה כהן · 12", row.title)
        assertEquals("a", row.actionId)
        assertEquals("החלפה ל־50 ק״מ", row.actionTitle)
        assertNotNull(row.actionConfirm)
        assertEquals("אחמ״ש 40 · מתנדב 50 · פער 10", row.trailing)
        assertEquals(listOf(row), filterReportRows(rows, "555"))
        assertEquals(listOf(row), filterReportRows(rows, "צומת"))
    }

    @Test
    fun `km discrepancy is admin only and keeps a date range`() {
        val spec = reportSpec(ReportKindId.KM_DISCREPANCY)
        assertEquals(ReportAudience.ADMIN, spec.audience)
        assertTrue(spec.hasDateRange)
    }
}
