package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignedVolunteerEventEditTest {
    @Test
    fun `blocks when the viewer is a responder`() {
        assertTrue(isAssignedVolunteerEventEditBlocked("me", listOf("a", "me"), emptyList()))
    }

    @Test
    fun `blocks when the viewer is a secondary lead without a responder row`() {
        assertTrue(isAssignedVolunteerEventEditBlocked("me", listOf("a"), listOf("me")))
    }

    @Test
    fun `does not block a main-only lead`() {
        assertFalse(isAssignedVolunteerEventEditBlocked("lead", listOf("a", "b"), listOf("other")))
    }

    @Test
    fun `does not block when the viewer id is missing`() {
        assertFalse(isAssignedVolunteerEventEditBlocked(null, listOf("me"), listOf("me")))
        assertFalse(isAssignedVolunteerEventEditBlocked("  ", listOf("me"), emptyList()))
    }

    @Test
    fun `draft helper uses responder and secondary ids`() {
        val draft = EventDraft(
            eventDate = "2026-09-04",
            responders = listOf(EventResponderDraft(responderId = "me")),
            secondaryLeads = listOf(SecondaryLead(userId = "other")),
        )
        assertTrue(draft.blocksAssignedVolunteerEdit("me"))
        assertFalse(draft.blocksAssignedVolunteerEdit("lead"))
        assertTrue(
            EventDraft(
                eventDate = "2026-09-04",
                secondaryLeads = listOf(SecondaryLead(userId = "sec")),
            ).blocksAssignedVolunteerEdit("sec"),
        )
    }

    @Test
    fun `keeps the exact Hebrew reject copy`() {
        assertEquals(
            "לא ניתן לערוך אירוע עליו אתה מוצב כמתנדב. לעדכון פרטים יש לפנות לאחמ\"ש המזין או למנהל מערכת",
            ASSIGNED_VOLUNTEER_EVENT_EDIT_ERROR,
        )
    }
}
