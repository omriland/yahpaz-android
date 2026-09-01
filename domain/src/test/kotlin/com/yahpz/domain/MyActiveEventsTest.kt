package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyActiveEventsTest {
    @Test
    fun `empty active copy is the lead-facing line`() {
        assertEquals("אין אירועים פעילים באחמ״ש שלך", MY_ACTIVE_EVENTS_EMPTY)
    }

    @Test
    fun `cannot pin done or cancelled events`() {
        assertTrue(canAddEventToMyActive(isCancelled = false, status = EventStatus.IN_PROGRESS))
        assertTrue(canAddEventToMyActive(isCancelled = false, status = EventStatus.DRAFT))
        assertTrue(canAddEventToMyActive(isCancelled = false, status = EventStatus.PARTIAL))
        assertFalse(canAddEventToMyActive(isCancelled = false, status = EventStatus.DONE))
        assertFalse(canAddEventToMyActive(isCancelled = true, status = EventStatus.IN_PROGRESS))
    }

    @Test
    fun `visible active is server minus dismissed plus pins`() {
        assertEquals(
            listOf("a", "c"),
            visibleMyActiveIds(
                serverIds = listOf("a", "b"),
                pinnedIds = setOf("c", "b"),
                dismissedIds = setOf("b"),
            ),
        )
    }

    @Test
    fun `shift-lead can delete only when no responders are assigned`() {
        assertTrue(canDeleteUnassignedEvent(canManageUnit = true, responderCount = 0))
        assertFalse(canDeleteUnassignedEvent(canManageUnit = true, responderCount = 1))
        assertFalse(canDeleteUnassignedEvent(canManageUnit = false, responderCount = 0))
    }
}
