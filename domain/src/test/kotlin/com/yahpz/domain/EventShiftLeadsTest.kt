package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventShiftLeadsTest {
    private fun lead(userId: String, locked: Boolean = false, name: String = userId, callsign: String = userId) =
        SecondaryLead(userId = userId, locked = locked, fullName = name, callsign = callsign)

    @Test
    fun managesSecondariesForLeadAdminSuperAdmin() {
        assertTrue(canManageSecondaryLeads(listOf("shift_lead")))
        assertTrue(canManageSecondaryLeads(listOf("admin")))
        assertTrue(canManageSecondaryLeads(listOf("super_admin")))
        assertFalse(canManageSecondaryLeads(listOf("responder")))
    }

    @Test
    fun creatingLeadMayPickMainBeforeSecondariesExist() {
        assertTrue(
            canChangeEventMainLead(
                roles = listOf("shift_lead"),
                eventExists = false,
                viewerIsCurrentMain = true,
                hasSecondaries = false,
            ),
        )
        assertTrue(
            canChangeEventMainLead(
                roles = listOf("shift_lead"),
                eventExists = true,
                viewerIsCurrentMain = true,
                hasSecondaries = false,
            ),
        )
        assertFalse(
            canChangeEventMainLead(
                roles = listOf("shift_lead"),
                eventExists = true,
                viewerIsCurrentMain = true,
                hasSecondaries = true,
            ),
        )
        assertTrue(
            canChangeEventMainLead(
                roles = listOf("admin"),
                eventExists = true,
                viewerIsCurrentMain = false,
                hasSecondaries = true,
            ),
        )
    }

    @Test
    fun nobodyRemovesLockedSecondaries() {
        assertTrue(canRemoveSecondaryLead(listOf("shift_lead"), locked = false))
        assertFalse(canRemoveSecondaryLead(listOf("super_admin"), locked = true))
        assertFalse(canRemoveSecondaryLead(listOf("responder"), locked = false))
    }

    @Test
    fun foreignEditPopupSkipsOnlyMain() {
        assertFalse(isForeignShiftLeadEvent("main", "main"))
        assertTrue(isForeignShiftLeadEvent("secondary", "main"))
        assertTrue(isForeignShiftLeadEvent("other-lead", "main"))
    }

    @Test
    fun autoLockOnlyAfterRealPersistByNonMainShiftLead() {
        assertTrue(
            shouldAutoLockSecondary(
                viewerId = "dana",
                mainLeadId = "omri",
                persistedFieldChange = true,
                viewerHasShiftLead = true,
            ),
        )
        assertFalse(
            shouldAutoLockSecondary(
                viewerId = "dana",
                mainLeadId = "omri",
                persistedFieldChange = false,
                viewerHasShiftLead = true,
            ),
        )
        assertFalse(
            shouldAutoLockSecondary(
                viewerId = "omri",
                mainLeadId = "omri",
                persistedFieldChange = true,
                viewerHasShiftLead = true,
            ),
        )
        assertFalse(
            shouldAutoLockSecondary(
                viewerId = "admin-only",
                mainLeadId = "omri",
                persistedFieldChange = true,
                viewerHasShiftLead = false,
            ),
        )
    }

    @Test
    fun createTimeTransferAddsCreatorAsRemovableSecondary() {
        assertEquals(
            SecondaryLead(userId = "omri", locked = false, fullName = "", callsign = ""),
            createTimeCreatorSecondary(creatorId = "omri", mainLeadId = "dana"),
        )
        assertNull(createTimeCreatorSecondary(creatorId = "omri", mainLeadId = "omri"))
    }

    @Test
    fun reassignMovesNewMainOutAndDemotesOldMain() {
        val next = reassignMainLeads(
            previousMainId = "omri",
            nextMainId = "dana",
            previousMainName = "עמרי",
            previousMainCallsign = "Admin",
            secondaries = listOf(lead("dana", name = "דנה", callsign = "D1"), lead("gil")),
        )
        assertEquals("dana", next.mainId)
        assertEquals(listOf("gil", "omri"), next.secondaries.map { it.userId })
        assertEquals(false, next.secondaries.first { it.userId == "omri" }.locked)
    }

    @Test
    fun leadCopyAndCaption() {
        assertEquals(MAIN_LEAD_LABEL_SHORT, eventLeadFieldLabel(hasSecondaries = false))
        assertEquals(MAIN_LEAD_LABEL, eventLeadFieldLabel(hasSecondaries = true))
        assertEquals("דנה כהן · D1", formatLeadPerson("דנה כהן", "D1"))
        assertEquals(
            "דנה כהן · D1 · עמרי · Admin",
            formatLeadsCaption("דנה כהן", "D1", listOf("עמרי" to "Admin")),
        )
        assertEquals(
            "דנה כהן · D1 +2",
            formatListLeadCaption("דנה כהן", "D1", listOf("עמרי" to "Admin", "גיא" to "G1")),
        )
        assertEquals(
            "",
            eventLeadsCaption("shift", "דנה", "D1", listOf("עמרי" to "Admin")),
        )
        assertEquals(
            "דנה · D1 +1",
            eventLeadsCaption("manual", "דנה", "D1", listOf("עמרי" to "Admin")),
        )
    }
}
