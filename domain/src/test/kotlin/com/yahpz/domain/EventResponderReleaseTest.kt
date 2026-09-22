package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventResponderReleaseTest {
    @Test
    fun holdsManualEventUntilPoliceId() {
        assertFalse(eventReleasedToResponders("manual", null))
        assertFalse(eventReleasedToResponders("manual", "   "))
        assertFalse(eventReleasedToResponders(null, ""))
        assertTrue(eventReleasedToResponders("manual", "12345"))
    }

    @Test
    fun neverHoldsShiftBorn() {
        assertTrue(eventReleasedToResponders("shift", null))
        assertTrue(eventReleasedToResponders("shift", ""))
    }
}
