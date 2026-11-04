package com.yahpz.domain

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Default אירועים catalog for a viewer who has אחמ״ש.
 *
 * Recency is `events.created_at`, the same clock the cockpit uses. `event_date`
 * is a calendar day and cannot express a rolling 12 hours.
 *
 * An older event stays only when this viewer is its primary or secondary
 * אחמ״ש and the unit list's פרטים חסרים rule (`isEventIncomplete` /
 * `missingEventFields`) still has gaps.
 */
const val SHIFT_LEAD_EVENTS_RECENT_MS = 12L * 60L * 60L * 1000L

const val SHOW_OLDER_UNIT_EVENTS_LABEL = "הצג אירועים ישנים יותר"

const val SHIFT_LEAD_EVENTS_RECENT_CAPTION =
    "מציג אירועים מ-12 השעות האחרונות ופרטים שחסרים לך."

const val SHIFT_LEAD_EVENTS_RECENT_EMPTY = "אין אירועים מ-12 השעות האחרונות"

/** Shared אירועים screen: anyone with shift_lead, including admin + אחמ״ש. */
fun shouldApplyShiftLeadRecentWindow(roles: List<String>): Boolean =
    AppRole.SHIFT_LEAD in roleSet(roles)

fun eventWithinShiftLeadRecentWindow(createdAt: String?, now: Instant): Boolean {
    val created = parseEventCreatedAt(createdAt) ?: return false
    return !created.isBefore(now.minusMillis(SHIFT_LEAD_EVENTS_RECENT_MS))
}

fun keepShiftLeadRecentEvent(
    createdAt: String?,
    now: Instant,
    viewerId: String?,
    shiftLeadId: String?,
    secondaryLeadIds: Collection<String?>,
    incomplete: IncompleteEventSnapshot,
): Boolean {
    if (eventWithinShiftLeadRecentWindow(createdAt, now)) return true
    if (!viewerIsThisEventLead(viewerId, shiftLeadId, secondaryLeadIds)) return false
    return isEventIncomplete(incomplete)
}

private fun viewerIsThisEventLead(
    viewerId: String?,
    shiftLeadId: String?,
    secondaryLeadIds: Collection<String?>,
): Boolean {
    if (sameLeadId(viewerId, shiftLeadId)) return true
    return secondaryLeadIds.any { sameLeadId(viewerId, it) }
}

private fun sameLeadId(viewerId: String?, otherId: String?): Boolean {
    val viewer = viewerId?.trim().orEmpty()
    val other = otherId?.trim().orEmpty()
    return viewer.isNotEmpty() && viewer == other
}

private fun parseEventCreatedAt(raw: String?): Instant? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val normalized = if (trimmed.contains(' ') && !trimmed.contains('T')) {
        trimmed.replace(' ', 'T')
    } else {
        trimmed
    }
    return runCatching { Instant.parse(normalized) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(normalized).toInstant() }.getOrNull()
}
