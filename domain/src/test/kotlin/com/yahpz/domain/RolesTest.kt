package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesTest {
    @Test
    fun `unknown roles are dropped`() {
        assertEquals(setOf(AppRole.RESPONDER), roleSet(listOf("responder", "kitchen_staff")))
        assertNull(AppRole.fromRaw("kitchen_staff"))
        assertEquals(AppRole.SHIFT_LEAD, AppRole.fromRaw("shift_lead"))
    }

    @Test
    fun `managesUnit covers lead admin and super admin`() {
        assertTrue(managesUnit(listOf("shift_lead")))
        assertTrue(managesUnit(listOf("admin")))
        assertTrue(managesUnit(listOf("super_admin")))
        assertTrue(managesUnit(listOf("responder", "shift_lead")))
        assertFalse(managesUnit(listOf("responder")))
        assertFalse(managesUnit(emptyList()))
    }

    @Test
    fun `isAdmin excludes shift lead`() {
        assertTrue(isAdmin(listOf("admin")))
        assertTrue(isAdmin(listOf("super_admin")))
        assertFalse(isAdmin(listOf("shift_lead")))
        assertFalse(isAdmin(listOf("responder")))
        assertFalse(isAdmin(emptyList()))
    }

    @Test
    fun `isResponder includes anyone who manages the unit`() {
        assertTrue(isResponder(listOf("responder")))
        assertTrue(isResponder(listOf("shift_lead")))
        assertTrue(isResponder(listOf("admin")))
        assertFalse(isResponder(emptyList()))
    }

    @Test
    fun `role labels follow the web wording`() {
        assertEquals("אחמ״ש", highestRoleLabel(listOf("responder", "shift_lead")))
        assertEquals("מנהל", highestRoleLabel(listOf("responder", "admin")))
        assertEquals("מנהל־על", highestRoleLabel(listOf("admin", "super_admin")))
        assertNull(highestRoleLabel(emptyList()))
        assertEquals(listOf("אחמ״ש", "מתנדב"), roleLabels(listOf("responder", "shift_lead")))
    }

    @Test
    fun `tools tab label depends on admin rights`() {
        assertEquals(TOOLS_TAB_LEAD_LABEL, toolsTabLabel(listOf("shift_lead")))
        assertEquals(TOOLS_TAB_ADMIN_LABEL, toolsTabLabel(listOf("admin")))
    }

    @Test
    fun `implied assignable roles include every role below the selected one`() {
        assertEquals(
            listOf("admin", "shift_lead", "responder"),
            impliedAssignableRoles(AppRole.ADMIN),
        )
        assertEquals(listOf("shift_lead", "responder"), impliedAssignableRoles(AppRole.SHIFT_LEAD))
        assertEquals(listOf("responder"), impliedAssignableRoles(AppRole.RESPONDER))
    }

    @Test
    fun `lower assignable roles lock when a higher one is selected`() {
        assertTrue(isAssignableRoleLocked(listOf("admin"), AppRole.SHIFT_LEAD))
        assertTrue(isAssignableRoleLocked(listOf("admin"), AppRole.RESPONDER))
        assertFalse(isAssignableRoleLocked(listOf("admin", "shift_lead", "responder"), AppRole.ADMIN))
        assertFalse(isAssignableRoleLocked(listOf("responder"), AppRole.RESPONDER))
    }

    @Test
    fun `toggle assignable role implies the roles beneath it`() {
        assertEquals(
            listOf("shift_lead", "responder"),
            toggleAssignableRole(listOf("responder"), AppRole.SHIFT_LEAD, checked = true),
        )
        assertEquals(
            listOf("admin", "shift_lead", "responder"),
            toggleAssignableRole(listOf("responder"), AppRole.ADMIN, checked = true),
        )
        assertEquals(
            listOf("shift_lead", "responder"),
            toggleAssignableRole(
                listOf("admin", "shift_lead", "responder"),
                AppRole.ADMIN,
                checked = false,
            ),
        )
        assertEquals(
            listOf("responder"),
            toggleAssignableRole(listOf("shift_lead", "responder"), AppRole.SHIFT_LEAD, checked = false),
        )
    }

    @Test
    fun `withImpliedAssignableRoles fills from the highest assigned role`() {
        assertEquals(
            listOf("admin", "shift_lead", "responder"),
            withImpliedAssignableRoles(listOf("admin")),
        )
        assertEquals(emptyList<String>(), withImpliedAssignableRoles(listOf("super_admin")))
    }
}
