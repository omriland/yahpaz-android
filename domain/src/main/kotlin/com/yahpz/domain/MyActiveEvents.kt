package com.yahpz.domain

const val MY_ACTIVE_EVENTS_EMPTY = "אין אירועים פעילים באחמ״ש שלך"
const val MY_ACTIVE_EVENT_PINNED = "נוסף לאירועים הפעילים."
const val MY_ACTIVE_ADD = "הוספה"
const val MY_ACTIVE_REMOVE = "הסרה"
const val MY_ACTIVE_REMOVE_LOCKED = "אירוע בהזנה — לא ניתן להסיר"
const val MY_ACTIVE_PREF_FAILED = "עדכון האירועים הפעילים נכשל. בדקו את החיבור ונסו שוב."
const val MY_ACTIVE_DRAG_TO_ACTIVE = "לחיצה ארוכה וגרירה לאירועים הפעילים"
const val MY_ACTIVE_DROP_TO_ADD = "שחררו כאן להוספה לפעילים"
const val MY_ACTIVE_DRAG_TO_ADD = "גררו לכאן להוספה לפעילים"
const val EVENT_DELETE_TITLE = "מחיקת אירוע"
const val EVENT_DELETE_CONFIRM = "למחוק את האירוע? אין כוננים משובצים."
const val EVENT_DELETE_ACTION = "מחיקה"
const val EVENT_DELETED = "האירוע נמחק."
const val EVENT_DELETE_FAILED = "מחיקת האירוע נכשלה. בדקו את החיבור ונסו שוב."

@Suppress("UNUSED_PARAMETER")
fun canAddEventToMyActive(isCancelled: Boolean, status: EventStatus): Boolean = true

fun canRemoveFromMyActive(
    viewerId: String,
    shiftLeadId: String?,
    status: EventStatus,
    isCancelled: Boolean,
): Boolean = !isLockedOnMyActive(viewerId, shiftLeadId, status, isCancelled)

fun isLockedOnMyActive(
    viewerId: String,
    shiftLeadId: String?,
    status: EventStatus,
    isCancelled: Boolean,
): Boolean =
    !isCancelled && status == EventStatus.DRAFT && shiftLeadId == viewerId

fun visibleMyActiveIds(
    lockedIds: List<String>,
    autoIds: List<String>,
    pinnedIds: Set<String>,
    hiddenIds: Set<String>,
): List<String> {
    val seen = LinkedHashSet<String>()
    for (id in lockedIds) seen += id
    for (id in autoIds) if (id !in hiddenIds) seen += id
    for (id in pinnedIds) seen += id
    return seen.toList()
}

fun canDeleteUnassignedEvent(canManageUnit: Boolean, responderCount: Int): Boolean =
    canManageUnit && responderCount == 0
