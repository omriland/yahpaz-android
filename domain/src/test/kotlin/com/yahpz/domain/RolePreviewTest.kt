package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolePreviewTest {
    @Test
    fun allowsSuperAdminWhenNotImpersonatingOrPreviewing() {
        assertTrue(
            canStartRolePreview(
                actualRoles = listOf("admin", "super_admin"),
                impersonating = false,
                previewing = false,
            ),
        )
        assertTrue(
            canStartImpersonation(
                actualRoles = listOf("admin", "super_admin"),
                impersonating = false,
            ),
        )
    }

    @Test
    fun rejectsRegularAdminImpersonationAndActivePreview() {
        assertFalse(
            canStartRolePreview(
                actualRoles = listOf("admin"),
                impersonating = false,
                previewing = false,
            ),
        )
        assertFalse(
            canStartRolePreview(
                actualRoles = listOf("admin", "super_admin"),
                impersonating = true,
                previewing = false,
            ),
        )
        assertFalse(
            canStartRolePreview(
                actualRoles = listOf("admin", "super_admin"),
                impersonating = false,
                previewing = true,
            ),
        )
        assertFalse(
            canStartImpersonation(
                actualRoles = listOf("admin", "super_admin"),
                impersonating = true,
            ),
        )
    }

    @Test
    fun effectiveRolesMasksToSelectedRole() {
        assertEquals(
            listOf("admin", "shift_lead", "super_admin"),
            effectiveRoles(listOf("admin", "shift_lead", "super_admin"), null),
        )
        assertEquals(listOf("responder"), effectiveRoles(listOf("admin", "super_admin"), AppRole.RESPONDER))
        assertEquals(listOf("shift_lead"), effectiveRoles(listOf("admin", "super_admin"), AppRole.SHIFT_LEAD))
        assertEquals(listOf("admin"), effectiveRoles(listOf("admin", "super_admin"), AppRole.ADMIN))
    }

    @Test
    fun parseRolePreviewAcceptsAssignableOnly() {
        assertEquals(AppRole.RESPONDER, parseRolePreviewRole("responder"))
        assertEquals(AppRole.SHIFT_LEAD, parseRolePreviewRole("shift_lead"))
        assertEquals(AppRole.ADMIN, parseRolePreviewRole("admin"))
        assertNull(parseRolePreviewRole("super_admin"))
        assertNull(parseRolePreviewRole("nope"))
        assertNull(parseRolePreviewRole(null))
    }

    @Test
    fun previewLabelsMatchTheRestOfTheApp() {
        assertEquals("מתנדב", rolePreviewLabel(AppRole.RESPONDER))
        assertEquals("אחמ״ש", rolePreviewLabel(AppRole.SHIFT_LEAD))
        assertEquals("מנהל", rolePreviewLabel(AppRole.ADMIN))
        assertEquals("צופה כתפקיד מתנדב", rolePreviewBannerText(AppRole.RESPONDER))
        assertEquals("צופה כ־דנה · או״ק 112", impersonationBannerText("דנה", "112"))
    }
}
