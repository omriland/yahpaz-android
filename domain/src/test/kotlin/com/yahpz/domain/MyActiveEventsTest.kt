package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyActiveEventsTest {
    @Test
    fun `empty active copy is the lead-facing line`() {
        assertEquals("אין אירועים פעילים באחמו\"ש שלך", MY_ACTIVE_EVENTS_EMPTY)
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
    fun `optimistic add pins a catalog event and drops hide`() {
        assertEquals(
            listOf(ActivePref("keep", "hide"), ActivePref("new", "pin")),
            prefsAfterAddToMyActive(
                prefs = listOf(ActivePref("keep", "hide"), ActivePref("new", "hide")),
                eventId = "new",
                alreadyAuto = false,
            ),
        )
    }

    @Test
    fun `optimistic add of an auto event only clears hide`() {
        assertEquals(
            listOf(ActivePref("other", "pin")),
            prefsAfterAddToMyActive(
                prefs = listOf(ActivePref("auto", "hide"), ActivePref("other", "pin")),
                eventId = "auto",
                alreadyAuto = true,
            ),
        )
    }

    @Test
    fun `failed add restores only that event's previous prefs`() {
        assertEquals(
            listOf(ActivePref("keep", "pin"), ActivePref("new", "hide")),
            prefsRestoringEvent(
                prefs = listOf(ActivePref("keep", "pin"), ActivePref("new", "pin")),
                eventId = "new",
                previous = listOf(ActivePref("new", "hide")),
            ),
        )
    }

    @Test
    fun `shift-lead can delete only their own unassigned event`() {
        assertTrue(
            canDeleteUnassignedEvent(
                canManageUnit = true,
                responderCount = 0,
                viewerIsAdmin = false,
                viewerId = "lead-a",
                shiftLeadId = "lead-a",
            ),
        )
        assertFalse(
            canDeleteUnassignedEvent(
                canManageUnit = true,
                responderCount = 0,
                viewerIsAdmin = false,
                viewerId = "lead-a",
                shiftLeadId = "lead-b",
            ),
        )
        assertFalse(
            canDeleteUnassignedEvent(
                canManageUnit = true,
                responderCount = 1,
                viewerIsAdmin = false,
                viewerId = "lead-a",
                shiftLeadId = "lead-a",
            ),
        )
        assertFalse(
            canDeleteUnassignedEvent(
                canManageUnit = false,
                responderCount = 0,
                viewerIsAdmin = false,
                viewerId = "lead-a",
                shiftLeadId = "lead-a",
            ),
        )
    }

    @Test
    fun `admin can delete another lead's unassigned event`() {
        assertTrue(
            canDeleteUnassignedEvent(
                canManageUnit = true,
                responderCount = 0,
                viewerIsAdmin = true,
                viewerId = "admin",
                shiftLeadId = "lead-b",
            ),
        )
        assertEquals(EVENT_DELETE_OTHER_LEAD, "אין הרשאה למחוק אירוע שנוצר על ידי אחמ״ש אחר.")
    }
}
