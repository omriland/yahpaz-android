package com.yahpz.domain

/**
 * Admin users on the phone. Mirrors web `AdminUsersPage` + `adminUsers.ts`,
 * minus impersonation and Google Places addresses.
 */
const val USERS_TITLE = "משתמשים"
const val INVITE_TITLE = "משתמש חדש"
const val USER_EDIT_TITLE = "עריכת משתמש"
const val USER_SAVE_LABEL = "שמירת משתמש"
const val USERS_SEARCH_PLACEHOLDER = "שם, או״ק, דוא״ל או סטטוס"

const val FIELD_FULL_NAME = "שם מלא"
const val FIELD_EMAIL = "דוא״ל"
const val FIELD_CALLSIGN = "או״ק"
const val FIELD_PHONE = "טלפון"
const val FIELD_VOLUNTEER_STATUS = "סטטוס מתנדב"
const val FIELD_ROLES = "תפקידים"
const val FIELD_VEHICLES = "רכבים"

const val OVERFLOW_EDIT = "עריכה"
const val OVERFLOW_DEACTIVATE = "השבתת משתמש"
const val OVERFLOW_REACTIVATE = "הפעלה מחדש"
const val OVERFLOW_DELETE = "מחיקת משתמש"
const val OVERFLOW_RESEND_INVITE = "שליחת הזמנה מחדש"
const val OVERFLOW_COPY_INVITE_LINK = "העתקת קישור הזמנה"

const val INVITE_IDENTITY_ERROR = "יש למלא שם מלא, דוא״ל ואו״ק."
const val INVITE_ROLE_ERROR = "יש לבחור לפחות תפקיד אחד."
const val INVITE_EMAIL_ERROR = "כתובת הדוא״ל אינה תקינה."
const val INVITE_PHONE_ERROR = "מספר הטלפון אינו תקין."
const val INVITE_SAVE_FAILED = "יצירת ההזמנה נכשלה. בדקו את החיבור ונסו שוב."
const val INVITE_SAVED = "ההזמנה נשלחה."
const val USER_CREATED = "משתמש נוצר בהצלחה"
const val USER_CREATED_COPIED = "משתמש נוצר בהצלחה וקישור ההזמנה הועתק."
const val USER_SAVED = "המשתמש נשמר"
const val USER_DELETED = "המשתמש נמחק"
const val SAVE_USER_FAILED = "שמירת המשתמש נכשלה. בדקו את החיבור ונסו שוב."
const val SAVE_ROLES_FAILED = "שמירת התפקידים נכשלה."
const val SAVE_VEHICLES_FAILED = "שמירת הרכבים נכשלה."
const val SET_ACTIVE_FAILED = "עדכון החשבון נכשל. נסו שוב."
const val DELETE_USER_FAILED = "מחיקת המשתמש נכשלה. בדקו את החיבור ונסו שוב."
const val RESEND_INVITE_FAILED = "שליחת ההזמנה מחדש נכשלה. נסו שוב."
const val COPY_INVITE_FAILED = "יצירת קישור ההזמנה נכשלה. נסו שוב."
const val INVITE_RESENT_COPIED = "ההזמנה נשלחה מחדש וקישור ההזמנה הועתק."
const val INVITE_LINK_COPIED = "קישור ההזמנה הועתק."
const val INVITE_LINK_COPY_FAILED = "נוצר קישור הזמנה, אך ההעתקה נכשלה. נסו שוב."

const val FORM_NAME_CALLSIGN_ERROR = "יש למלא שם מלא ואו״ק."
const val FORM_EMAIL_REQUIRED = "יש למלא דוא״ל."
const val FORM_EMAIL_INVALID = "יש להזין כתובת דוא״ל תקינה."
const val FORM_PHONE_ERROR = "יש להזין מספר טלפון בן 10 ספרות."
const val DUPLICATE_PLATE_ERROR = "לא ניתן לשייך את אותה לוחית רישוי יותר מפעם אחת לאותו משתמש."
const val CANNOT_REMOVE_OWN_ADMIN = "לא ניתן להסיר מעצמך את תפקיד המנהל."
const val SUPER_ADMIN_LOCK_ERROR = "לא ניתן לערוך מנהל־על."
const val SUPER_ADMIN_CAPTION = "מנהל־על"
const val SELF_DELETE_ERROR = "לא ניתן למחוק את המשתמש המחובר כעת."
const val DELETE_USER_TITLE = "מחיקת משתמש"
const val DELETE_USER_BODY =
    "המשתמש יימחק לצמיתות מאימות וממערכת המשתמשים. לא ניתן לשחזר — רק להזמין מחדש. אם הוא אחמ״ש על אירועים או משמרות, המחיקה תיחסם."
const val DELETE_USER_ACTION = "מחיקה"
const val DEACTIVATE_USER_ACTION = "השבתה"
const val DEACTIVATE_USER_BODY = "הוא לא יוכל להתחבר, והנתונים ההיסטוריים יישמרו."

const val OTP_PHONE_REQUIRED = "יש להזין מספר נייד ישראלי תקין לפני הפעלת OTP."
const val OTP_LOGIN_ENABLED_TOAST = "OTP בכניסה הופעל"
const val OTP_LOGIN_DISABLED_TOAST = "OTP בכניסה כובה"
const val OTP_USERS_PAGE_ENABLED_TOAST = "OTP לניהול משתמשים הופעל"
const val OTP_USERS_PAGE_DISABLED_TOAST = "OTP לניהול משתמשים כובה"
const val OTP_ENABLE_LOGIN_TITLE = "להפעיל אימות SMS בכניסה למשתמש זה?"
const val OTP_ENABLE_USERS_PAGE_TITLE = "להפעיל אימות SMS לניהול משתמשים למשתמש זה?"
const val OTP_ENABLE_ACTION = "הפעלה"
const val OTP_SET_FAILED = "עדכון OTP נכשל. נסו שוב."

const val ADD_VEHICLE = "הוספת רכב"
const val VEHICLE_PLATE_LABEL = "לוחית רישוי"
const val VEHICLE_MODEL_LABEL = "דגם"
const val VEHICLE_DELETE_CONFIRM = "האם למחוק את הרכב הזה? לא ניתן לשחזר אותו לאחר המחיקה."
const val VEHICLE_ARCHIVE_CONFIRM =
    "לא ניתן למחוק רכב זה כי הוא מקושר לאירוע קיים. האם להעביר אותו לארכיון כדי שאיש לא יוכל להשתמש בו יותר במערכת?"
const val VEHICLE_ARCHIVED_CAPTION = "בארכיון — לא ניתן לשייך לאירועים חדשים"
const val VEHICLE_DELETE_FAILED = "מחיקת הרכב נכשלה."
const val VEHICLE_ARCHIVE_FAILED = "העברת הרכב לארכיון נכשלה."
const val VEHICLE_UNARCHIVE_FAILED = "שחזור הרכב מהארכיון נכשל."
const val EMAIL_LOCKED_HINT = "לא ניתן לשנות דוא״ל לאחר יצירה."
const val EMAIL_INVITE_HINT = "נשלחת הזמנה לכתובת זו."
const val PHONE_HINT = "10 ספרות, למשל: 050-1234567"
const val ROLES_HINT = "בחירת תפקיד כוללת את התפקידים שמתחתיו."
const val INVITE_PENDING_LABEL = "ממתין להרשמה"
const val INACTIVE_ACCOUNT_LABEL = "מושבת"

/** Roles an admin may hand out from the phone; מנהל־על stays a web-only grant. */
val INVITABLE_ROLES = listOf(AppRole.RESPONDER, AppRole.SHIFT_LEAD, AppRole.ADMIN)

data class AdminVehicleDraft(
    val key: String,
    val id: String? = null,
    val plateNumber: String = "",
    val model: String = "",
    val archived: Boolean = false,
)

data class InviteDraft(
    val id: String? = null,
    val fullName: String = "",
    val email: String = "",
    val callsign: String = "",
    val phone: String = "",
    val volunteerStatus: VolunteerStatus = VolunteerStatus.DEFAULT,
    val roles: List<String> = listOf(AppRole.RESPONDER.raw),
    val vehicles: List<AdminVehicleDraft> = emptyList(),
)

data class InviteDraftErrors(
    val fullName: String? = null,
    val email: String? = null,
    val callsign: String? = null,
    val phone: String? = null,
    val roles: String? = null,
    val form: String? = null,
) {
    val isEmpty: Boolean
        get() = fullName == null && email == null && callsign == null &&
            phone == null && roles == null && form == null

    val formMessage: String?
        get() = when {
            isEmpty -> null
            form != null -> form
            email != null -> email
            phone != null -> phone
            roles != null -> roles
            else -> FORM_NAME_CALLSIGN_ERROR
        }
}

data class RoleSyncDiff(
    val toAdd: List<String>,
    val toRemove: List<String>,
)

data class AdminUserSortKey(
    val fullName: String,
    val active: Boolean,
    val invitePending: Boolean,
)

data class AdminUserSearchInput(
    val fullName: String,
    val callsign: String,
    val email: String,
    val volunteerStatus: String?,
    val availability: AvailabilityStatus,
    val availableFrom: String?,
    val active: Boolean = true,
    val invitePending: Boolean = false,
)

/** Deliberately loose: the invite email is the real check, this only catches typos. */
fun looksLikeEmail(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.contains(' ')) return false
    val at = trimmed.indexOf('@')
    if (at <= 0 || at != trimmed.lastIndexOf('@')) return false
    val domain = trimmed.substring(at + 1)
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}

fun isValidPhone(raw: String?): Boolean = phoneDigits(raw).length == 10

fun createUserEmailError(raw: String): String? {
    if (raw.trim().isEmpty()) return null
    return if (looksLikeEmail(raw)) null else FORM_EMAIL_INVALID
}

fun canSubmitCreateUser(draft: InviteDraft): Boolean =
    draft.fullName.isNotBlank() &&
        looksLikeEmail(draft.email) &&
        draft.callsign.isNotBlank() &&
        isValidPhone(draft.phone)

fun validateInviteDraft(draft: InviteDraft): InviteDraftErrors {
    val email = draft.email.trim()
    return InviteDraftErrors(
        fullName = if (draft.fullName.isBlank()) FORM_NAME_CALLSIGN_ERROR else null,
        email = when {
            email.isEmpty() -> FORM_EMAIL_REQUIRED
            !looksLikeEmail(email) -> FORM_EMAIL_INVALID
            else -> null
        },
        callsign = if (draft.callsign.isBlank()) FORM_NAME_CALLSIGN_ERROR else null,
        phone = if (!isValidPhone(draft.phone)) FORM_PHONE_ERROR else null,
        roles = if (draft.roles.isEmpty()) INVITE_ROLE_ERROR else null,
        form = if (findDuplicatePlate(draft.vehicles.map { it.plateNumber }) != null) {
            DUPLICATE_PLATE_ERROR
        } else {
            null
        },
    )
}

fun validateAdminUserDraft(
    draft: InviteDraft,
    actorUserId: String?,
    isSuperAdmin: Boolean,
    existingRoles: List<String> = draft.roles,
): InviteDraftErrors {
    val base = if (draft.id == null) {
        validateInviteDraft(draft)
    } else {
        InviteDraftErrors(
            fullName = if (draft.fullName.isBlank()) FORM_NAME_CALLSIGN_ERROR else null,
            callsign = if (draft.callsign.isBlank()) FORM_NAME_CALLSIGN_ERROR else null,
            phone = if (!isValidPhone(draft.phone)) FORM_PHONE_ERROR else null,
            roles = if (draft.roles.isEmpty()) INVITE_ROLE_ERROR else null,
            form = if (findDuplicatePlate(draft.vehicles.map { it.plateNumber }) != null) {
                DUPLICATE_PLATE_ERROR
            } else {
                null
            },
        )
    }
    if (!base.isEmpty) return base
    if (draft.id != null && draft.id == actorUserId && !draft.roles.contains(AppRole.ADMIN.raw)) {
        return base.copy(roles = CANNOT_REMOVE_OWN_ADMIN)
    }
    if (draft.id != null && !canMutateAdminUser(isSuperAdmin, existingRoles)) {
        return base.copy(form = SUPER_ADMIN_LOCK_ERROR)
    }
    return base
}

fun toggleInviteRole(roles: List<String>, role: String): List<String> =
    if (roles.contains(role)) roles.filterNot { it == role } else roles + role

fun setActiveActionLabel(next: Boolean): String =
    if (next) OVERFLOW_REACTIVATE else OVERFLOW_DEACTIVATE

fun setActiveConfirm(next: Boolean, name: String): String {
    val who = name.trim().ifEmpty { "המשתמש" }
    return if (next) {
        "$who יוכל להתחבר לאפליקציה מחדש. להפעיל?"
    } else {
        "להשבית את המשתמש $who?"
    }
}

fun setActiveToast(next: Boolean): String =
    if (next) "החשבון הופעל." else "החשבון הושבת."

fun deactivateConfirmTitle(name: String): String {
    val who = name.trim().ifEmpty { "המשתמש" }
    return "להשבית את המשתמש $who?"
}

fun deleteUserConfirm(name: String): String {
    val who = name.trim().ifEmpty { "המשתמש" }
    return "למחוק את המשתמש $who?"
}

fun otpLoginActionLabel(enabled: Boolean): String =
    if (enabled) "כבה OTP בכניסה" else "הפעל OTP בכניסה"

fun otpUsersPageActionLabel(enabled: Boolean): String =
    if (enabled) "כבה OTP לניהול משתמשים" else "הפעל OTP לניהול משתמשים"

fun otpUserLabel(otpLoginEnabled: Boolean, otpUsersPageEnabled: Boolean): String? = when {
    otpLoginEnabled && otpUsersPageEnabled -> "שניהם"
    otpLoginEnabled -> "כניסה"
    otpUsersPageEnabled -> "משתמשים"
    else -> null
}

fun otpFlagToast(kind: String, enabled: Boolean): String = when {
    kind == "users_page" && enabled -> OTP_USERS_PAGE_ENABLED_TOAST
    kind == "users_page" -> OTP_USERS_PAGE_DISABLED_TOAST
    enabled -> OTP_LOGIN_ENABLED_TOAST
    else -> OTP_LOGIN_DISABLED_TOAST
}

fun canToggleUsersPageOtp(roles: List<String>): Boolean = roles.contains(AppRole.ADMIN.raw)

fun hasSuperAdminRole(roles: List<String>): Boolean = roles.contains(AppRole.SUPER_ADMIN.raw)

fun canMutateAdminUser(actorIsSuperAdmin: Boolean, targetRoles: List<String>): Boolean =
    actorIsSuperAdmin || !hasSuperAdminRole(targetRoles)

fun isInvitePending(active: Boolean, invitePending: Boolean): Boolean = active && invitePending

fun hasAvailability(active: Boolean, invitePending: Boolean): Boolean = !isInvitePending(active, invitePending)

fun compareAdminUsers(a: AdminUserSortKey, b: AdminUserSortKey): Int {
    fun rank(user: AdminUserSortKey): Int = when {
        isInvitePending(user.active, user.invitePending) -> 2
        !user.active -> 1
        else -> 0
    }
    val byRank = rank(a) - rank(b)
    if (byRank != 0) return byRank
    return a.fullName.compareTo(b.fullName)
}

fun adminUserMatchesQuery(row: AdminUserSearchInput, query: String, today: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val fields = buildList {
        add(row.fullName)
        add(row.callsign)
        add(row.email)
        add(volunteerStatusLabel(row.volunteerStatus))
        if (hasAvailability(row.active, row.invitePending)) {
            add(availabilitySearchLabel(row.availability, row.availableFrom, today))
        }
    }
    return fieldsMatchQuery(fields, trimmed)
}

fun addressKindLabel(kind: String?, customLabel: String? = null): String = when (kind) {
    "home" -> "בית"
    "work" -> "עבודה"
    "other" -> customLabel?.trim()?.ifEmpty { null } ?: "אחר"
    else -> customLabel?.trim()?.ifEmpty { null } ?: "כתובת"
}

private val PROTECTED_ROLES = setOf(AppRole.SUPER_ADMIN.raw)

fun syncUserRolesDiff(current: List<String>, next: List<String>): RoleSyncDiff {
    val currentSet = current.toSet()
    val nextAssignable = next.filterNot { it in PROTECTED_ROLES }
    val nextSet = nextAssignable.toSet()
    return RoleSyncDiff(
        toAdd = nextAssignable.filterNot { it in currentSet },
        toRemove = currentSet.filter { it !in PROTECTED_ROLES && it !in nextSet },
    )
}
