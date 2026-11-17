package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LeadCardGapsTest {
    private fun event(
        policeEventId: String? = "4417",
        patrolCallsign: String? = "12",
        hasEventType: Boolean = true,
        hasRoad: Boolean = true,
        hasDistrict: Boolean = true,
        location: String? = "מחלף שורק",
        responders: List<IncompleteResponderSnapshot> = listOf(IncompleteResponderSnapshot(totalKm = null)),
    ) = IncompleteEventSnapshot(
        policeEventId = policeEventId,
        patrolCallsign = patrolCallsign,
        hasEventType = hasEventType,
        hasRoad = hasRoad,
        hasDistrict = hasDistrict,
        location = location,
        responders = responders,
    )

    @Test
    fun `responder km and hours are not the lead's card gap`() {
        assertEquals(emptyList<String>(), leadCardGapLabels(event()))
    }

    @Test
    fun `the four lead-owned fields show in a fixed order`() {
        val gaps = leadCardGapLabels(
            event(policeEventId = null, patrolCallsign = null, hasEventType = false, hasRoad = false),
        )
        assertEquals(listOf("מספר אירוע", "אוק - מס", "סוג אירוע", "כביש"), gaps)
    }

    @Test
    fun `district, location and an empty responder list stay off the card`() {
        assertEquals(
            emptyList<String>(),
            leadCardGapLabels(event(hasDistrict = false, location = null, responders = emptyList())),
        )
    }
}
