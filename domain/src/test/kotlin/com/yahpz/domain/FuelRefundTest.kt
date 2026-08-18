package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FuelRefundTest {
    private val profiles = listOf(
        FuelRefundProfileInput("r2", "יוסי לוי", "44"),
        FuelRefundProfileInput("r1", "דנה כהן", "12"),
    )

    @Test
    fun `participations without km are ignored in the total and the count`() {
        val rows = buildFuelRefundRows(
            profiles,
            listOf(
                FuelRefundParticipationInput("r1", "e1", 10.0),
                FuelRefundParticipationInput("r1", "e2", null),
                FuelRefundParticipationInput("r1", "e3", 5.0),
            ),
        )
        val dana = rows.first { it.id == "r1" }
        assertEquals(15.0, dana.totalKm, 0.0)
        assertEquals(2, dana.eventCount)
    }

    @Test
    fun `private vehicle shift credits add km without adding events`() {
        val rows = buildFuelRefundRows(
            profiles,
            listOf(FuelRefundParticipationInput("r1", "e1", 10.0)),
            listOf(FuelRefundCreditInput("r1", 30.0), FuelRefundCreditInput("r1", 2.0)),
        )
        val dana = rows.first { it.id == "r1" }
        assertEquals(42.0, dana.totalKm, 0.0)
        assertEquals(1, dana.eventCount)
    }

    @Test
    fun `every active profile appears even with no km`() {
        val rows = buildFuelRefundRows(profiles, emptyList())
        assertEquals(2, rows.size)
        assertEquals(0.0, rows[0].totalKm, 0.0)
    }

    @Test
    fun `rows sort by full name`() {
        val rows = buildFuelRefundRows(profiles, emptyList())
        assertEquals(listOf("דנה כהן", "יוסי לוי"), rows.map { it.fullName })
    }

    @Test
    fun `report rows label the event count in hebrew`() {
        val rows = fuelRefundReportRows(
            buildFuelRefundRows(
                listOf(FuelRefundProfileInput("r1", "דנה כהן", "12")),
                listOf(FuelRefundParticipationInput("r1", "e1", 1200.5)),
            ),
        )
        assertEquals("דנה כהן · 12", rows[0].title)
        assertEquals("אירוע אחד", rows[0].subtitle)
        assertEquals("1,200.5 ק״מ", rows[0].trailing)
    }
}
