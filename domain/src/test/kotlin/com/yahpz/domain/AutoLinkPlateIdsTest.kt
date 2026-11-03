package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoLinkPlateIdsTest {
    private fun plate(id: String, number: String) =
        EventMediaPlateOption(id = id, plateNumber = number, model = null, color = null, logoSlug = null)

    private val plates = listOf(
        plate("a", "12-345-67"),
        plate("b", "713 86 301"),
        plate("c", "7654321"),
    )

    @Test
    fun matchesByDigitsRegardlessOfFormatting() {
        assertEquals(listOf("a", "b"), autoLinkPlateIds(listOf("71386301", "1234567"), plates, emptyList()))
    }

    @Test
    fun scannedValuesAreNormalisedToo() {
        assertEquals(listOf("a"), autoLinkPlateIds(listOf("12-345-67"), plates, emptyList()))
    }

    @Test
    fun skipsAlreadyLinked() {
        assertEquals(listOf("b"), autoLinkPlateIds(listOf("1234567", "71386301"), plates, listOf("a")))
    }

    @Test
    fun noDuplicatesWhenScannedTwiceOrPlateListedTwice() {
        val doubled = plates + plate("a", "1234567")
        assertEquals(listOf("a"), autoLinkPlateIds(listOf("1234567", "1234567"), doubled, emptyList()))
    }

    @Test
    fun keepsPlatesOrder() {
        assertEquals(listOf("a", "c"), autoLinkPlateIds(listOf("7654321", "1234567"), plates, emptyList()))
    }

    @Test
    fun emptyWhenNothingMatches() {
        assertEquals(emptyList<String>(), autoLinkPlateIds(listOf("1111111"), plates, emptyList()))
    }

    @Test
    fun emptyWhenNoPlatesOrNoScan() {
        assertEquals(emptyList<String>(), autoLinkPlateIds(listOf("1234567"), emptyList(), emptyList()))
        assertEquals(emptyList<String>(), autoLinkPlateIds(emptyList(), plates, emptyList()))
    }

    @Test
    fun blankScannedValueNeverMatches() {
        assertEquals(emptyList<String>(), autoLinkPlateIds(listOf("", "abc"), listOf(plate("x", "")), emptyList()))
    }
}
