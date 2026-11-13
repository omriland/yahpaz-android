package com.yahpz.domain

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class UnitEventIdTest {
    @Test
    fun acceptsSixUppercaseCharactersWithALetter() {
        assertTrue(isUnitEventId("K7Q2M9"))
        assertTrue(isUnitEventId("ABCDEF"))
    }

    @Test
    fun rejectsPoliceNumbersLowercaseAndWrongLengths() {
        assertFalse(isUnitEventId("123456"))
        assertFalse(isUnitEventId("k7q2m9"))
        assertFalse(isUnitEventId("K7Q2M"))
        assertFalse(isUnitEventId("K7Q2M90"))
        assertFalse(isUnitEventId(""))
        assertFalse(isUnitEventId(null))
    }

    @Test
    fun inputKeepsAUnitIdWholeAndPoliceNumbersToFiveDigits() {
        assertEquals("K7Q2M9", policeEventIdForInput("K7Q2M9"))
        assertEquals("12345", policeEventIdForInput("12-345 6"))
        assertEquals("729", policeEventIdForInput("k7q2m9"))
    }

    @Test
    fun sameDayCheckDoesNotConfuseAUnitIdWithItsDigits() {
        assertFalse(
            sameDayPoliceEventIdCollides(
                eventDate = "2026-11-13",
                policeEventId = "K7Q2M9",
                currentEventId = null,
                existing = listOf(SameDayPoliceEventRow(id = "other", eventDate = "2026-11-13", policeEventId = "729", isCancelled = false)),
            ),
        )
    }
}
