package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitEventsScopeTest {
    @Test
    fun `lead only defaults to own created events`() {
        assertTrue(shouldFilterUnitEventsToOwnCreated(listOf("shift_lead")))
        assertTrue(shouldFilterUnitEventsToOwnCreated(listOf("shift_lead", "responder")))
        assertEquals(
            "lead-a",
            unitEventsCreatedByFilter(
                roles = listOf("shift_lead"),
                showOthersCreated = false,
                userId = "lead-a",
            ),
        )
    }

    @Test
    fun `lead only toggle on returns the full unit list`() {
        assertNull(
            unitEventsCreatedByFilter(
                roles = listOf("shift_lead", "responder"),
                showOthersCreated = true,
                userId = "lead-a",
            ),
        )
    }

    @Test
    fun `admin plus lead never gets the own-created default`() {
        assertFalse(shouldFilterUnitEventsToOwnCreated(listOf("admin")))
        assertFalse(shouldFilterUnitEventsToOwnCreated(listOf("admin", "shift_lead")))
        assertNull(
            unitEventsCreatedByFilter(
                roles = listOf("admin", "shift_lead"),
                showOthersCreated = false,
                userId = "admin-1",
            ),
        )
    }

    @Test
    fun `super admin never gets the own-created default`() {
        assertFalse(shouldFilterUnitEventsToOwnCreated(listOf("super_admin")))
        assertFalse(shouldFilterUnitEventsToOwnCreated(listOf("super_admin", "shift_lead")))
        assertFalse(
            shouldFilterUnitEventsToOwnCreated(listOf("admin", "super_admin", "shift_lead")),
        )
        assertNull(
            unitEventsCreatedByFilter(
                roles = listOf("super_admin"),
                showOthersCreated = false,
                userId = "sa-1",
            ),
        )
    }

    @Test
    fun `uses the locked Hebrew label`() {
        assertEquals("הצג אירועים שנוצרו על ידי אחרים", SHOW_OTHERS_CREATED_EVENTS_LABEL)
    }
}
