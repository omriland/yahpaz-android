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
    fun `web copy is used for titles, save, overflow and otp`() {
        assertEquals("משתמשים", USERS_TITLE)
        assertEquals("משתמש חדש", INVITE_TITLE)
        assertEquals("עריכת משתמש", USER_EDIT_TITLE)
        assertEquals("שמירת משתמש", USER_SAVE_LABEL)
        assertEquals("עריכה", OVERFLOW_EDIT)
        assertEquals("השבתת משתמש", OVERFLOW_DEACTIVATE)
        assertEquals("הפעלה מחדש", OVERFLOW_REACTIVATE)
        assertEquals("מחיקת משתמש", OVERFLOW_DELETE)
        assertEquals("שליחת הזמנה מחדש", OVERFLOW_RESEND_INVITE)
        assertEquals("העתקת קישור הזמנה", OVERFLOW_COPY_INVITE_LINK)
        assertEquals("כבה OTP בכניסה", otpLoginActionLabel(enabled = true))
        assertEquals("הפעל OTP בכניסה", otpLoginActionLabel(enabled = false))
        assertEquals("כבה OTP לניהול משתמשים", otpUsersPageActionLabel(enabled = true))
        assertEquals("הפעל OTP לניהול משתמשים", otpUsersPageActionLabel(enabled = false))
        assertEquals("מנהל", roleLabel(AppRole.ADMIN))
        assertEquals("אחמ״ש", roleLabel(AppRole.SHIFT_LEAD))
        assertEquals("מתנדב", roleLabel(AppRole.RESPONDER))
        assertEquals("שם מלא", FIELD_FULL_NAME)
        assertEquals("דוא״ל", FIELD_EMAIL)
        assertEquals("או״ק", FIELD_CALLSIGN)
        assertEquals("טלפון", FIELD_PHONE)
        assertEquals("סטטוס מתנדב", FIELD_VOLUNTEER_STATUS)
        assertEquals("תפקידים", FIELD_ROLES)
        assertEquals("רכבים", FIELD_VEHICLES)
    }

    @Test
    fun `a complete draft passes`() {
        assertTrue(validateInviteDraft(valid).isEmpty)
        assertNull(validateInviteDraft(valid).formMessage)
        assertTrue(canSubmitCreateUser(valid))
    }

    @Test
    fun `name, email, callsign and a 10-digit phone are all required`() {
        assertEquals(
            FORM_NAME_CALLSIGN_ERROR,
            validateInviteDraft(valid.copy(fullName = "  ")).formMessage,
        )
        assertEquals(
            FORM_EMAIL_REQUIRED,
            validateInviteDraft(valid.copy(email = "")).formMessage,
        )
        assertEquals(
            FORM_NAME_CALLSIGN_ERROR,
            validateInviteDraft(valid.copy(callsign = "")).formMessage,
        )
        assertEquals(
            FORM_PHONE_ERROR,
            validateInviteDraft(valid.copy(phone = "")).formMessage,
        )
        assertFalse(canSubmitCreateUser(valid.copy(phone = "050-123")))
    }

    @Test
    fun `malformed email is called out before the identity message`() {
        val errors = validateInviteDraft(valid.copy(fullName = "", email = "dana@yahpz"))
        assertEquals(FORM_EMAIL_INVALID, errors.formMessage)
        assertEquals(FORM_NAME_CALLSIGN_ERROR, errors.fullName)
        assertEquals(FORM_EMAIL_INVALID, createUserEmailError("dana@yahpz"))
        assertNull(createUserEmailError(""))
        assertNull(createUserEmailError("dana@yahpz.com"))
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
    fun `phone must be ten digits, matching the web form`() {
        assertEquals(FORM_PHONE_ERROR, validateInviteDraft(valid.copy(phone = "031234567")).phone)
        assertNull(validateInviteDraft(valid.copy(phone = "052-111-1111")).phone)
        assertTrue(isValidPhone("050-1234567"))
        assertFalse(isValidPhone("050-123"))
    }

    @Test
    fun `at least one role is required, matching the edge function`() {
        val errors = validateInviteDraft(valid.copy(roles = emptyList()))
        assertEquals(INVITE_ROLE_ERROR, errors.roles)
        assertEquals(INVITE_ROLE_ERROR, errors.formMessage)
    }

    @Test
    fun `duplicate plates on the same draft are rejected`() {
        val draft = valid.copy(
            vehicles = listOf(
                AdminVehicleDraft(key = "a", plateNumber = "12-345-67", model = "טויוטה"),
                AdminVehicleDraft(key = "b", plateNumber = "1234567", model = "קיה"),
            ),
        )
        assertEquals(DUPLICATE_PLATE_ERROR, validateInviteDraft(draft).formMessage)
        assertEquals("1234567", findDuplicatePlate(draft.vehicles.map { it.plateNumber }))
        assertNull(findDuplicatePlate(listOf("1111111", "2222222", "")))
    }

    @Test
    fun `an admin cannot remove their own admin role`() {
        val draft = valid.copy(id = "me", roles = listOf("responder"))
        assertEquals(
            CANNOT_REMOVE_OWN_ADMIN,
            validateAdminUserDraft(draft, actorUserId = "me", isSuperAdmin = true).formMessage,
        )
        assertNull(
            validateAdminUserDraft(
                draft.copy(roles = listOf("admin", "shift_lead", "responder")),
                actorUserId = "me",
                isSuperAdmin = true,
            ).formMessage,
        )
    }

    @Test
    fun `a regular admin cannot mutate a super admin`() {
        val draft = valid.copy(id = "other", roles = listOf("admin", "super_admin"))
        assertEquals(
            SUPER_ADMIN_LOCK_ERROR,
            validateAdminUserDraft(draft, actorUserId = "me", isSuperAdmin = false).formMessage,
        )
        assertNull(
            validateAdminUserDraft(draft, actorUserId = "me", isSuperAdmin = true).formMessage,
        )
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
        assertEquals("השבתת משתמש", setActiveActionLabel(next = false))
        assertEquals("הפעלה מחדש", setActiveActionLabel(next = true))
        assertTrue(deactivateConfirmTitle("דנה כהן").contains("דנה כהן"))
        assertTrue(setActiveConfirm(next = false, name = "דנה כהן").contains("דנה כהן"))
        assertTrue(setActiveConfirm(next = false, name = "  ").contains("המשתמש"))
        assertEquals("החשבון הופעל.", setActiveToast(next = true))
        assertEquals("החשבון הושבת.", setActiveToast(next = false))
    }

    @Test
    fun `default draft invites a responder who is an active volunteer`() {
        val draft = InviteDraft()
        assertEquals(listOf("responder"), draft.roles)
        assertEquals(VolunteerStatus.ACTIVE_VOLUNTEER, draft.volunteerStatus)
        assertTrue(draft.vehicles.isEmpty())
    }

    @Test
    fun `invite pending is only active users who have not registered`() {
        assertTrue(isInvitePending(active = true, invitePending = true))
        assertFalse(isInvitePending(active = true, invitePending = false))
        assertFalse(isInvitePending(active = false, invitePending = true))
    }

    @Test
    fun `pending registration has no availability`() {
        assertFalse(hasAvailability(active = true, invitePending = true))
        assertTrue(hasAvailability(active = true, invitePending = false))
        assertTrue(hasAvailability(active = false, invitePending = false))
    }

    @Test
    fun `admin users sort active then inactive then invite pending, by name`() {
        val rows = listOf(
            AdminUserSortKey(fullName = "דני", active = false, invitePending = false),
            AdminUserSortKey(fullName = "בני", active = true, invitePending = false),
            AdminUserSortKey(fullName = "אלי", active = true, invitePending = true),
            AdminUserSortKey(fullName = "אבי", active = true, invitePending = true),
        )
        assertEquals(
            listOf("בני", "דני", "אבי", "אלי"),
            rows.sortedWith(::compareAdminUsers).map { it.fullName },
        )
    }

    @Test
    fun `search matches name, callsign, email, volunteer status and availability`() {
        val row = AdminUserSearchInput(
            fullName = "דנה כהן",
            callsign = "112",
            email = "dana@yahpz.com",
            volunteerStatus = VolunteerStatus.ACTIVE_VOLUNTEER.raw,
            availability = AvailabilityStatus.UNAVAILABLE,
            availableFrom = "2099-01-01",
            invitePending = false,
            active = true,
        )
        assertTrue(adminUserMatchesQuery(row, "דנה", today = "2026-08-18"))
        assertTrue(adminUserMatchesQuery(row, "112", today = "2026-08-18"))
        assertTrue(adminUserMatchesQuery(row, "dana@", today = "2026-08-18"))
        assertTrue(adminUserMatchesQuery(row, "מתנדב", today = "2026-08-18"))
        assertTrue(adminUserMatchesQuery(row, "לא זמין", today = "2026-08-18"))
        assertFalse(adminUserMatchesQuery(row, "מנהלה", today = "2026-08-18"))
        assertEquals(
            "שם, או״ק, דוא״ל או סטטוס",
            USERS_SEARCH_PLACEHOLDER,
        )
    }

    @Test
    fun `search ignores availability for pending registration`() {
        val row = AdminUserSearchInput(
            fullName = "אבי כהן",
            callsign = "200",
            email = "avi@yahpz.com",
            volunteerStatus = VolunteerStatus.ACTIVE_VOLUNTEER.raw,
            availability = AvailabilityStatus.AVAILABLE,
            availableFrom = null,
            invitePending = true,
            active = true,
        )
        assertTrue(adminUserMatchesQuery(row, "אבי", today = "2026-08-18"))
        assertFalse(adminUserMatchesQuery(row, "זמין", today = "2026-08-18"))
    }

    @Test
    fun `otp compact labels match the web column`() {
        assertEquals("שניהם", otpUserLabel(otpLoginEnabled = true, otpUsersPageEnabled = true))
        assertEquals("כניסה", otpUserLabel(otpLoginEnabled = true, otpUsersPageEnabled = false))
        assertEquals("משתמשים", otpUserLabel(otpLoginEnabled = false, otpUsersPageEnabled = true))
        assertNull(otpUserLabel(otpLoginEnabled = false, otpUsersPageEnabled = false))
    }

    @Test
    fun `users-page otp is only for admins`() {
        assertTrue(canToggleUsersPageOtp(listOf("admin")))
        assertTrue(canToggleUsersPageOtp(listOf("admin", "responder")))
        assertFalse(canToggleUsersPageOtp(listOf("responder")))
        assertFalse(canToggleUsersPageOtp(listOf("shift_lead")))
    }

    @Test
    fun `regular admins cannot mutate a super admin row`() {
        assertTrue(canMutateAdminUser(actorIsSuperAdmin = true, targetRoles = listOf("super_admin")))
        assertFalse(canMutateAdminUser(actorIsSuperAdmin = false, targetRoles = listOf("super_admin")))
        assertTrue(canMutateAdminUser(actorIsSuperAdmin = false, targetRoles = listOf("admin")))
    }

    @Test
    fun `role sync ignores super_admin so the UI cannot grant or strip it`() {
        val diff = syncUserRolesDiff(
            current = listOf("admin", "super_admin"),
            next = listOf("responder"),
        )
        assertEquals(listOf("responder"), diff.toAdd)
        assertEquals(listOf("admin"), diff.toRemove)
        assertFalse(diff.toAdd.contains("super_admin"))
        assertFalse(diff.toRemove.contains("super_admin"))
    }

    @Test
    fun `address kind labels match the web, with custom other names`() {
        assertEquals("בית", addressKindLabel("home"))
        assertEquals("עבודה", addressKindLabel("work"))
        assertEquals("הורים", addressKindLabel("other", "הורים"))
        assertEquals("אחר", addressKindLabel("other", "  "))
        assertEquals("אחר", addressKindLabel("other"))
    }

    @Test
    fun `delete confirm copy matches the web`() {
        assertEquals("מחיקת משתמש", DELETE_USER_TITLE)
        assertTrue(deleteUserConfirm("דנה כהן").contains("דנה כהן"))
        assertEquals("לא ניתן למחוק את המשתמש המחובר כעת.", SELF_DELETE_ERROR)
    }
}
