package com.yahpz.responder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventWhenLabelTest {
    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `today and yesterday read as words`() {
        assertEquals("היום 07:40", eventWhenLabel("2026-11-16", "2026-11-16T07:40:00+02:00", today))
        assertEquals("אתמול 22:30", eventWhenLabel("2026-11-15", "2026-11-15T22:30:00+02:00", today))
    }

    @Test
    fun `older days this year drop the year`() {
        assertEquals("12.11 · 18:10", eventWhenLabel("2026-11-12", "2026-11-12T18:10:00+02:00", today))
    }

    @Test
    fun `another year keeps the year`() {
        assertEquals("30.12.2025 · 09:00", eventWhenLabel("2025-12-30", "2025-12-30 09:00:00", today))
    }

    @Test
    fun `no start time shows the day alone`() {
        assertEquals("היום", eventWhenLabel("2026-11-16", null, today))
        assertEquals("12.11", eventWhenLabel("2026-11-12", "", today))
    }

    @Test
    fun `an unparseable date falls back to the raw text`() {
        assertEquals("bad · 07:40", eventWhenLabel("bad", "07:40", today))
    }
}
