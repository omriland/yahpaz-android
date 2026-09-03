package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForeignEventEditTest {
    @Test
    fun `is true only when another lead created the event`() {
        assertTrue(isForeignShiftLeadEvent("me", "them"))
        assertFalse(isForeignShiftLeadEvent("me", "me"))
    }

    @Test
    fun `is false when either id is missing`() {
        assertFalse(isForeignShiftLeadEvent("me", null))
        assertFalse(isForeignShiftLeadEvent(null, "them"))
        assertFalse(isForeignShiftLeadEvent("  ", "them"))
    }

    @Test
    fun `title uses the lead name`() {
        assertEquals(
            "האם אתה בטוח שברצונך לערוך אירוע שהוזן על ידי דנה כהן?",
            foreignEventEditTitle("דנה כהן"),
        )
        assertEquals("כל שינוי שתבצע יתועד ויישמר במערכת", FOREIGN_EVENT_EDIT_BODY)
    }

    @Test
    fun `lead name falls back from empty name to callsign`() {
        assertEquals("A12", foreignEventEditLeadName("  ", "A12"))
        assertEquals(FOREIGN_EVENT_EDIT_LEAD_FALLBACK, foreignEventEditLeadName("", ""))
    }
}
