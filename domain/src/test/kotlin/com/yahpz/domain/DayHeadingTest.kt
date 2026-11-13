package com.yahpz.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayHeadingTest {
    private val today = LocalDate.parse("2026-11-13")

    @Test
    fun recentDaysReadRelative() {
        assertEquals("היום · ${formatDate("2026-11-13")}", formatDayHeading("2026-11-13", today))
        assertEquals("אתמול · ${formatDate("2026-11-12")}", formatDayHeading("2026-11-12", today))
        assertEquals("לפני 4 ימים · ${formatDate("2026-11-09")}", formatDayHeading("2026-11-09", today))
    }

    @Test
    fun olderDaysAreTheDateAlone() {
        assertEquals(formatDate("2026-11-01"), formatDayHeading("2026-11-01", today))
    }
}
