package com.yahpz.domain

/**
 * Inviting a user from the phone. Mirrors the fields the `admin-users` edge function
 * validates for `action: "invite"`; addresses, vehicles and OTP flags stay on the web.
 */
const val INVITE_TITLE = "הזמנת משתמש"
const val INVITE_IDENTITY_ERROR = "יש למלא שם מלא, דוא״ל ואו״ק."
const val INVITE_ROLE_ERROR = "יש לבחור לפחות תפקיד אחד."
const val INVITE_EMAIL_ERROR = "כתובת הדוא״ל אינה תקינה."
const val INVITE_PHONE_ERROR = "מספר הטלפון אינו תקין."
const val INVITE_SAVE_FAILED = "יצירת ההזמנה נכשלה. בדקו את החיבור ונסו שוב."
const val INVITE_SAVED = "ההזמנה נשלחה."

const val SET_ACTIVE_FAILED = "עדכון החשבון נכשל. נסו שוב."

/** Roles an admin may hand out from the phone; מנהל־על stays a web-only grant. */
val INVITABLE_ROLES = listOf(AppRole.RESPONDER, AppRole.SHIFT_LEAD, AppRole.ADMIN)

data class InviteDraft(
    val fullName: String = "",
    val email: String = "",
    val callsign: String = "",
    val phone: String = "",
    val volunteerStatus: VolunteerStatus = VolunteerStatus.DEFAULT,
    val roles: List<String> = listOf(AppRole.RESPONDER.raw),
)

data class InviteDraftErrors(
    val fullName: String? = null,
    val email: String? = null,
    val callsign: String? = null,
    val phone: String? = null,
    val roles: String? = null,
) {
    val isEmpty: Boolean
        get() = fullName == null && email == null && callsign == null && phone == null && roles == null

    /** The edge function answers with one message per problem class, so match its copy. */
    val formMessage: String?
        get() = when {
            isEmpty -> null
            email != null -> email
            phone != null -> phone
            roles != null -> roles
            else -> INVITE_IDENTITY_ERROR
        }
}

/** Deliberately loose: the invite email is the real check, this only catches typos. */
fun looksLikeEmail(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.contains(' ')) return false
    val at = trimmed.indexOf('@')
    if (at <= 0 || at != trimmed.lastIndexOf('@')) return false
    val domain = trimmed.substring(at + 1)
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}

fun validateInviteDraft(draft: InviteDraft): InviteDraftErrors {
    val email = draft.email.trim()
    val phone = draft.phone.trim()
    return InviteDraftErrors(
        fullName = if (draft.fullName.isBlank()) INVITE_IDENTITY_ERROR else null,
        email = when {
            email.isEmpty() -> INVITE_IDENTITY_ERROR
            !looksLikeEmail(email) -> INVITE_EMAIL_ERROR
            else -> null
        },
        callsign = if (draft.callsign.isBlank()) INVITE_IDENTITY_ERROR else null,
        phone = if (phone.isNotEmpty() && !isValidIlMobile(phone)) INVITE_PHONE_ERROR else null,
        roles = if (draft.roles.isEmpty()) INVITE_ROLE_ERROR else null,
    )
}

fun toggleInviteRole(roles: List<String>, role: String): List<String> =
    if (roles.contains(role)) roles.filterNot { it == role } else roles + role

/** All three take the state being moved to, not the current one. */
fun setActiveActionLabel(next: Boolean): String =
    if (next) "הפעלת החשבון" else "השבתת החשבון"

fun setActiveConfirm(next: Boolean, name: String): String {
    val who = name.trim().ifEmpty { "המשתמש" }
    return if (next) {
        "$who יוכל להתחבר לאפליקציה מחדש. להפעיל?"
    } else {
        "$who לא יוכל להתחבר לאפליקציה עד להפעלה מחדש. להשבית?"
    }
}

fun setActiveToast(next: Boolean): String =
    if (next) "החשבון הופעל." else "החשבון הושבת."
