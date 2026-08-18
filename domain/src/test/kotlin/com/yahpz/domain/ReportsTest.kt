package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsTest {
    @Test
    fun `shift lead sees unit reports but not the admin ones`() {
        val ids = visibleReportSpecs(listOf("shift_lead")).map { it.id }
        assertTrue(ids.contains(ReportKindId.OPEN_DOCUMENTATION))
        assertTrue(ids.contains(ReportKindId.EVENTS_BY_RESPONDER))
        assertTrue(ids.contains(ReportKindId.KM_EXCEPTIONS))
        assertFalse(ids.contains(ReportKindId.FUEL_REFUND))
    }

    @Test
    fun `admin sees every report`() {
        assertEquals(REPORT_SPECS.size, visibleReportSpecs(listOf("admin")).size)
    }

    @Test
    fun `plain responder sees no reports`() {
        assertTrue(visibleReportSpecs(listOf("responder")).isEmpty())
    }

    @Test
    fun `rolling reports default to thirty days back`() {
        val range = defaultReportRange(reportSpec(ReportKindId.EVENTS_BY_RESPONDER), "2026-03-15")
        assertEquals("2026-02-13" to "2026-03-15", range)
    }

    @Test
    fun `fuel refund defaults to the first of the month`() {
        val range = defaultReportRange(reportSpec(ReportKindId.FUEL_REFUND), "2026-03-15")
        assertEquals("2026-03-01" to "2026-03-15", range)
    }

    @Test
    fun `range validation rejects reversed and empty days`() {
        assertTrue(isValidReportRange("2026-01-01", "2026-01-01"))
        assertFalse(isValidReportRange("2026-02-01", "2026-01-01"))
        assertFalse(isValidReportRange("", "2026-01-01"))
    }

    @Test
    fun `report filter matches search text and hebrew keyboard slips`() {
        val rows = listOf(
            ReportRow(id = "a", title = "דנה כהן", searchText = "דנה כהן 12345"),
            ReportRow(id = "b", title = "יוסי לוי", searchText = "יוסי לוי 99999"),
        )
        assertEquals(listOf("a"), filterReportRows(rows, "12345").map { it.id })
        // `s` maps to `ד` on a Hebrew keyboard.
        assertEquals(listOf("a"), filterReportRows(rows, "sbv").map { it.id })
        assertEquals(2, filterReportRows(rows, "   ").size)
    }

    @Test
    fun `person and place displays drop blanks`() {
        assertEquals("דנה כהן · 12", personDisplay("דנה כהן", "12"))
        assertEquals("דנה כהן", personDisplay("דנה כהן", "  "))
        assertEquals("כונן", personDisplay(null, null))
        assertEquals("", personDisplay(null, null, fallback = ""))
        assertEquals("כביש 6 · צומת", placeDisplay("כביש 6", "צומת"))
        assertEquals("", placeDisplay(null, ""))
    }

    @Test
    fun `police label marks cancelled events first`() {
        assertEquals("בוטל · 55", policeEventLabel("55", isCancelled = true))
        assertEquals("בוטל", policeEventLabel(null, isCancelled = true))
        assertEquals("55", policeEventLabel("55", isCancelled = false))
        assertEquals("—", policeEventLabel("  ", isCancelled = false))
    }

    @Test
    fun `every spec id resolves and is unique`() {
        assertEquals(REPORT_SPECS.size, REPORT_SPECS.map { it.id }.toSet().size)
        REPORT_SPECS.forEach { spec ->
            assertEquals(spec, reportSpec(spec.id))
            assertEquals(spec.id, ReportKindId.fromRaw(spec.id.raw))
        }
        assertNull(ReportKindId.fromRaw("cockpit"))
    }

    @Test
    fun `row summary counts in hebrew`() {
        assertEquals("אין שורות בדוח", reportRowSummary(0))
        assertEquals("שורה אחת בדוח", reportRowSummary(1))
        assertEquals("4 שורות בדוח", reportRowSummary(4))
    }
}
