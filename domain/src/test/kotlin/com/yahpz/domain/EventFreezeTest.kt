package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventFreezeTest {
    @Test
    fun `both reasons freeze and exclude from fuel refund`() {
        val flags = computeFreezeFlags(
            matchesOver60km = true,
            matchesSuspiciousDuplicate = true,
            approvedOver60km = false,
            approvedSuspiciousDuplicate = false,
        )
        assertTrue(flags.isFrozen)
        assertFalse(flags.countsTowardFuelRefund)
        assertEquals(
            "האירוע מוקפא בגלל חריגת קילומטרים (מעל 60 ק״מ) ובגלל חשד לאירוע כפול, וממתין לאישור מנהל.",
            flags.tooltipHe,
        )
    }

    @Test
    fun `approving 60km leaves a duplicate freeze in place`() {
        val flags = computeFreezeFlags(
            matchesOver60km = true,
            matchesSuspiciousDuplicate = true,
            approvedOver60km = true,
            approvedSuspiciousDuplicate = false,
        )
        assertTrue(flags.isFrozen)
        assertFalse(flags.frozenOver60km)
        assertTrue(flags.frozenSuspiciousDuplicate)
        assertFalse(flags.countsTowardFuelRefund)
    }

    @Test
    fun `approving both reasons unfreezes and counts for fuel refund`() {
        val flags = computeFreezeFlags(
            matchesOver60km = true,
            matchesSuspiciousDuplicate = true,
            approvedOver60km = true,
            approvedSuspiciousDuplicate = true,
        )
        assertFalse(flags.isFrozen)
        assertTrue(flags.countsTowardFuelRefund)
        assertNull(flags.tooltipHe)
    }
}
