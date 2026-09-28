package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventResponderReleaseTest {
    @Test
    fun holdsManualEventUntilCallsignRoadAndPoliceId() {
        assertFalse(eventReleasedToResponders("manual", null, "411", true))
        assertFalse(eventReleasedToResponders("manual", "   ", "411", true))
        assertFalse(eventReleasedToResponders(null, "", "411", true))
        assertFalse(eventReleasedToResponders("manual", "12345", null, true))
        assertFalse(eventReleasedToResponders("manual", "12345", "411", false))
        assertFalse(eventReleasedToResponders("manual", "12345", "   ", true, patrolCallsignLegacy = "אביב"))
        assertTrue(
            eventReleasedToResponders(
                "manual",
                "12345",
                patrolCallsignNumber = null,
                hasRoad = true,
                patrolCallsignLegacy = "ניידת 1",
            ),
        )
        assertTrue(eventReleasedToResponders("manual", "12345", "411", true))
    }

    @Test
    fun neverHoldsShiftBorn() {
        assertTrue(eventReleasedToResponders("shift", null, null, false))
        assertTrue(eventReleasedToResponders("shift", "", hasRoad = false))
    }
}
