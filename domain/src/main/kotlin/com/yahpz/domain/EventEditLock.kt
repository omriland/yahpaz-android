package com.yahpz.domain

const val EVENT_EDIT_LOCK_MS = 7L * 24 * 60 * 60 * 1000
const val EVENT_EDIT_LOCKED_TOOLTIP = "לא ניתן לערוך אירוע שנוצר לפני מעל ל-7 ימים"

fun isEventEditAgeLocked(
    createdAt: String?,
    roles: Collection<String>,
    nowMs: Long = System.currentTimeMillis(),
): Boolean {
    if ("admin" in roles || "super_admin" in roles) return false
    val created = createdAt?.trim().orEmpty()
    if (created.isEmpty()) return false
    val createdMs = runCatching { java.time.Instant.parse(created).toEpochMilli() }.getOrNull()
        ?: return false
    return nowMs - createdMs > EVENT_EDIT_LOCK_MS
}
