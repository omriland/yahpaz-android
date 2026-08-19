package com.yahpz.domain

/** Roles a Super Admin can preview. Matches web `rolePreview.ts`. */
val PREVIEWABLE_ROLES: List<AppRole> = listOf(AppRole.RESPONDER, AppRole.SHIFT_LEAD, AppRole.ADMIN)

const val VIEW_AS_USER_LABEL = "צפייה כמשתמש"
const val VIEW_AS_ROLE_LABEL = "צפייה בתפקיד אחר"
const val STOP_ROLE_PREVIEW_LABEL = "חזרה לתפקיד שלי"
const val STOP_IMPERSONATION_LABEL = "חזרה לחשבון שלי"
const val ROLE_PREVIEW_STARTED = "נכנסתם למצב צפייה בתפקיד אחר."
const val ROLE_PREVIEW_STOPPED = "חזרת בהצלחה לתפקיד שלך."
const val IMPERSONATION_STARTED = "נכנסתם למצב צפייה כמשתמש."
const val IMPERSONATION_STOPPED = "חזרתם לחשבון שלכם."
const val IMPERSONATION_ALREADY = "כבר במצב צפייה כמשתמש אחר."
const val IMPERSONATION_OPEN_FAILED = "פתיחת הצפייה נכשלה. נסו שוב."
const val IMPERSONATION_NONE = "אין צפייה פעילה לשחזור."
const val IMPERSONATION_RESTORE_FAILED = "השחזור נכשל — התחברו מחדש."
const val IMPERSONATION_LOAD_FAILED = "טעינת המשתמשים נכשלה."
const val IMPERSONATION_EMPTY = "לא נמצאו משתמשים תואמים."
const val IMPERSONATION_AVAILABILITY_LOCKED = "צפייה כמשתמש — לא ניתן לשנות זמינות."
const val ROLE_PREVIEW_HINT = "תראו את הניווט כפי שמופיע בתפקיד שנבחר."
const val IMPERSONATION_HINT = "תראו את המערכת בדיוק כמו המשתמש שנבחר — כולל שמירות."

fun parseRolePreviewRole(raw: String?): AppRole? =
    PREVIEWABLE_ROLES.find { it.raw == raw }

fun canStartRolePreview(
    actualRoles: List<String>,
    impersonating: Boolean,
    previewing: Boolean,
): Boolean = hasSuperAdminRole(actualRoles) && !impersonating && !previewing

fun canStartImpersonation(
    actualRoles: List<String>,
    impersonating: Boolean,
): Boolean = hasSuperAdminRole(actualRoles) && !impersonating

fun effectiveRoles(actualRoles: List<String>, previewRole: AppRole?): List<String> {
    if (previewRole == null) return actualRoles
    return listOf(previewRole.raw)
}

fun rolePreviewLabel(role: AppRole): String = roleLabel(role)

fun rolePreviewBannerText(role: AppRole): String = "צופה כתפקיד ${rolePreviewLabel(role)}"

fun impersonationBannerText(fullName: String, callsign: String): String =
    "צופה כ־$fullName · או״ק $callsign"
