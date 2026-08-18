package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUserDraftTest {
    private val valid = InviteDraft(
        fullName = "דנה כהן",
        email = "dana@yahpz.com",
        callsign = "112",
        phone = "0501234567",
        roles = listOf("responder"),
    )

    @Test
    fun `a complete draft passes`() {
        assertTrue(validateInviteDraft(valid).isEmpty)
        assertNull(validateInviteDraft(valid).formMessage)
    }

    @Test
    fun `name, email and callsign are all required`() {
        assertEquals(
            INVITE_IDENTITY_ERROR,
            validateInviteDraft(valid.copy(fullName = "  ")).formMessage,
        )
        assertEquals(
            INVITE_IDENTITY_ERROR,
            validateInviteDraft(valid.copy(email = "")).formMessage,
        )
        assertEquals(
            INVITE_IDENTITY_ERROR,
            validateInviteDraft(valid.copy(callsign = "")).formMessage,
        )
    }

    @Test
    fun `malformed email is called out before the identity message`() {
        val errors = validateInviteDraft(valid.copy(fullName = "", email = "dana@yahpz"))
        assertEquals(INVITE_EMAIL_ERROR, errors.formMessage)
        assertEquals(INVITE_IDENTITY_ERROR, errors.fullName)
    }

    @Test
    fun `email shape check accepts real addresses and rejects typos`() {
        assertTrue(looksLikeEmail("dana@yahpz.com"))
        assertTrue(looksLikeEmail(" dana.cohen@sub.yahpz.co.il "))
        assertFalse(looksLikeEmail("dana@yahpz"))
        assertFalse(looksLikeEmail("dana.yahpz.com"))
        assertFalse(looksLikeEmail("@yahpz.com"))
        assertFalse(looksLikeEmail("dana@@yahpz.com"))
        assertFalse(looksLikeEmail("dana@yahpz.com."))
        assertFalse(looksLikeEmail("dana cohen@yahpz.com"))
    }

    @Test
    fun `phone is optional but must be an israeli mobile when present`() {
        assertNull(validateInviteDraft(valid.copy(phone = "")).phone)
        assertEquals(INVITE_PHONE_ERROR, validateInviteDraft(valid.copy(phone = "031234567")).phone)
        assertNull(validateInviteDraft(valid.copy(phone = "052-111-1111")).phone)
    }

    @Test
    fun `at least one role is required, matching the edge function`() {
        val errors = validateInviteDraft(valid.copy(roles = emptyList()))
        assertEquals(INVITE_ROLE_ERROR, errors.roles)
        assertEquals(INVITE_ROLE_ERROR, errors.formMessage)
    }

    @Test
    fun `role toggle adds and removes`() {
        val once = toggleInviteRole(listOf("responder"), "shift_lead")
        assertEquals(listOf("responder", "shift_lead"), once)
        assertEquals(listOf("responder"), toggleInviteRole(once, "shift_lead"))
    }

    @Test
    fun `the phone cannot hand out super admin`() {
        assertEquals(
            listOf(AppRole.RESPONDER, AppRole.SHIFT_LEAD, AppRole.ADMIN),
            INVITABLE_ROLES,
        )
        assertFalse(INVITABLE_ROLES.contains(AppRole.SUPER_ADMIN))
    }

    @Test
    fun `active toggle copy speaks about the state being moved to`() {
        assertEquals("השבתת החשבון", setActiveActionLabel(next = false))
        assertEquals("הפעלת החשבון", setActiveActionLabel(next = true))
        assertTrue(setActiveConfirm(next = false, name = "דנה כהן").startsWith("דנה כהן"))
        assertTrue(setActiveConfirm(next = false, name = "  ").startsWith("המשתמש"))
        assertEquals("החשבון הופעל.", setActiveToast(next = true))
        assertEquals("החשבון הושבת.", setActiveToast(next = false))
    }

    @Test
    fun `default draft invites a responder who is an active volunteer`() {
        val draft = InviteDraft()
        assertEquals(listOf("responder"), draft.roles)
        assertEquals(VolunteerStatus.ACTIVE_VOLUNTEER, draft.volunteerStatus)
    }
}
