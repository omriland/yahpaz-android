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
    fun `any event can be added to my active`() {
        assertTrue(canAddEventToMyActive(isCancelled = false, status = EventStatus.DONE))
        assertTrue(canAddEventToMyActive(isCancelled = true, status = EventStatus.IN_PROGRESS))
        assertTrue(canAddEventToMyActive(isCancelled = false, status = EventStatus.DRAFT))
    }

    @Test
    fun `cannot remove own draft בהזנה event`() {
        assertFalse(
            canRemoveFromMyActive(
                viewerId = "lead",
                shiftLeadId = "lead",
                status = EventStatus.DRAFT,
                isCancelled = false,
            ),
        )
        assertTrue(
            canRemoveFromMyActive(
                viewerId = "lead",
                shiftLeadId = "lead",
                status = EventStatus.IN_PROGRESS,
                isCancelled = false,
            ),
        )
        assertTrue(
            canRemoveFromMyActive(
                viewerId = "lead",
                shiftLeadId = "other",
                status = EventStatus.DRAFT,
                isCancelled = false,
            ),
        )
        assertTrue(
            canRemoveFromMyActive(
                viewerId = "lead",
                shiftLeadId = "lead",
                status = EventStatus.DRAFT,
                isCancelled = true,
            ),
        )
    }

    @Test
    fun `visible active is locked then auto minus hides then pins`() {
        assertEquals(
            listOf("lock", "auto", "pin"),
            visibleMyActiveIds(
                lockedIds = listOf("lock"),
                autoIds = listOf("lock", "auto", "hidden"),
                pinnedIds = setOf("pin"),
                hiddenIds = setOf("hidden", "lock"),
            ),
        )
    }

    @Test
    fun `add and remove labels`() {
        assertEquals("הוספה", MY_ACTIVE_ADD)
        assertEquals("הסרה", MY_ACTIVE_REMOVE)
        assertEquals("אירוע בהזנה — לא ניתן להסיר", MY_ACTIVE_REMOVE_LOCKED)
    }

    @Test
    fun `shift-lead can delete only when no responders are assigned`() {
        assertTrue(canDeleteUnassignedEvent(canManageUnit = true, responderCount = 0))
        assertFalse(canDeleteUnassignedEvent(canManageUnit = true, responderCount = 1))
        assertFalse(canDeleteUnassignedEvent(canManageUnit = false, responderCount = 0))
    }
}
