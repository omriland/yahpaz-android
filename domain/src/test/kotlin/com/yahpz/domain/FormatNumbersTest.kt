package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatNumbersTest {
    @Test
    fun `whole numbers lose the fraction and gain grouping`() {
        assertEquals("0", formatNumber(0.0))
        assertEquals("60", formatNumber(60.0))
        assertEquals("1,234", formatNumber(1234.0))
        assertEquals("12,345", formatNumber(12345.04))
    }

    @Test
    fun `fractions keep one digit`() {
        assertEquals("1.5", formatNumber(1.5))
        assertEquals("1,200.5", formatNumber(1200.49))
        assertEquals("-12.3", formatNumber(-12.34))
    }

    @Test
    fun `numeric road names sort before named ones`() {
        val roads = listOf(
            LookupOption("a", "עירוני (101)"),
            LookupOption("b", "70"),
            LookupOption("c", "6"),
        )
        assertEquals(listOf("6", "70", "עירוני (101)"), sortByRoadName(roads) { it.name }.map { it.name })
        assertTrue(compareRoadNames("6", "70") < 0)
        assertTrue(compareRoadNames("עירוני", "70") > 0)
        assertEquals(0, compareRoadNames(" 6 ", "6"))
    }
}
