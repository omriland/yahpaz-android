package com.yahpz.domain

enum class AppRole(val raw: String) {
    RESPONDER("responder"),
    SHIFT_LEAD("shift_lead"),
    ADMIN("admin"),
    SUPER_ADMIN("super_admin");

    companion object {
        fun fromRaw(raw: String?): AppRole? = entries.find { it.raw == raw }
    }
}

fun roleSet(roles: List<String>): Set<AppRole> = roles.mapNotNull { AppRole.fromRaw(it) }.toSet()

fun managesUnit(roles: List<String>): Boolean {
    val s = roleSet(roles)
    return AppRole.ADMIN in s || AppRole.SHIFT_LEAD in s || AppRole.SUPER_ADMIN in s
}

fun isAdmin(roles: List<String>): Boolean {
    val s = roleSet(roles)
    return AppRole.ADMIN in s || AppRole.SUPER_ADMIN in s
}

fun isResponder(roles: List<String>): Boolean {
    val s = roleSet(roles)
    return AppRole.RESPONDER in s || managesUnit(roles)
}

private val ROLE_LABELS = mapOf(
    AppRole.SUPER_ADMIN to "מנהל־על",
    AppRole.ADMIN to "מנהל",
    AppRole.SHIFT_LEAD to "אחמ״ש",
    AppRole.RESPONDER to "כונן",
)

/** Rank order matches the web: later entries win. */
private val ROLE_RANK = listOf(AppRole.RESPONDER, AppRole.SHIFT_LEAD, AppRole.ADMIN, AppRole.SUPER_ADMIN)

fun roleLabel(role: AppRole): String = ROLE_LABELS.getValue(role)

fun highestRole(roles: List<String>): AppRole? {
    val s = roleSet(roles)
    return ROLE_RANK.lastOrNull { it in s }
}

fun highestRoleLabel(roles: List<String>): String? = highestRole(roles)?.let { roleLabel(it) }

fun roleLabels(roles: List<String>): List<String> =
    ROLE_RANK.reversed().filter { it in roleSet(roles) }.map { roleLabel(it) }

const val TOOLS_TAB_LEAD_LABEL = "כלים"
const val TOOLS_TAB_ADMIN_LABEL = "ניהול"

fun toolsTabLabel(roles: List<String>): String =
    if (isAdmin(roles)) TOOLS_TAB_ADMIN_LABEL else TOOLS_TAB_LEAD_LABEL
