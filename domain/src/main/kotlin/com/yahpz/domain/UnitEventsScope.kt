package com.yahpz.domain

const val SHOW_OTHERS_CREATED_EVENTS_LABEL = "הצג אירועים שנוצרו על ידי אחרים"

/** אחמ״ש only — not admin, not SuperAdmin. Those roles keep the full unit list. */
fun shouldFilterUnitEventsToOwnCreated(roles: List<String>): Boolean {
    val s = roleSet(roles)
    return AppRole.SHIFT_LEAD in s && AppRole.ADMIN !in s && AppRole.SUPER_ADMIN !in s
}

/** `shift_lead_id` to push into the unit-list query, or null for everyone. */
fun unitEventsCreatedByFilter(
    roles: List<String>,
    showOthersCreated: Boolean,
    userId: String?,
): String? {
    if (!shouldFilterUnitEventsToOwnCreated(roles)) return null
    if (showOthersCreated) return null
    return userId
}
