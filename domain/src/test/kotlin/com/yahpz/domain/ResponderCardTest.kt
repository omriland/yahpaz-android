package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponderCardTest {
    @Test
    fun showsLeadKmToAnyLeadOrAdminRole() {
        assertTrue(responderCardShowsLeadKm(true))
        assertFalse(responderCardShowsLeadKm(false))
    }

    @Test
    fun labelsViewOnlyLeadKmForShiftLead() {
        assertEquals("ק״מ (אחמ״ש)", LEAD_KM_VIEW_LABEL)
    }
}
