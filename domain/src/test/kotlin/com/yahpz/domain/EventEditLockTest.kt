package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventEditLockTest {
    private val now = Instant.parse("2026-09-10T08:00:00Z").toEpochMilli()

    @Test
    fun locksShiftLeadAfterMoreThanSevenDays() {
        val created = Instant.ofEpochMilli(now - EVENT_EDIT_LOCK_MS - 1).toString()
        assertTrue(isEventEditAgeLocked(created, listOf("shift_lead"), now))
    }

    @Test
    fun staysOpenAtExactlySevenDays() {
        val created = Instant.ofEpochMilli(now - EVENT_EDIT_LOCK_MS).toString()
        assertFalse(isEventEditAgeLocked(created, listOf("shift_lead"), now))
    }

    @Test
    fun neverLocksAdmin() {
        val created = Instant.ofEpochMilli(now - EVENT_EDIT_LOCK_MS * 3).toString()
        assertFalse(isEventEditAgeLocked(created, listOf("admin"), now))
        assertFalse(isEventEditAgeLocked(created, listOf("super_admin", "shift_lead"), now))
    }
}
