package com.yahpz.domain

const val MY_ACTIVE_EVENTS_EMPTY = "אין אירועים פעילים באחמ״ש שלך"
const val MY_ACTIVE_EVENT_PINNED = "נוסף לאירועים הפעילים."
const val MY_ACTIVE_DRAG_TO_ACTIVE = "לחיצה ארוכה וגרירה לאירועים הפעילים"
const val MY_ACTIVE_DROP_TO_ADD = "שחררו כאן להוספה לפעילים"
const val MY_ACTIVE_DRAG_TO_ADD = "גררו לכאן להוספה לפעילים"
const val EVENT_DELETE_TITLE = "מחיקת אירוע"
const val EVENT_DELETE_CONFIRM = "למחוק את האירוע? אין כוננים משובצים."
const val EVENT_DELETE_ACTION = "מחיקה"
const val EVENT_DELETED = "האירוע נמחק."
const val EVENT_DELETE_FAILED = "מחיקת האירוע נכשלה. בדקו את החיבור ונסו שוב."

fun canAddEventToMyActive(isCancelled: Boolean, status: EventStatus): Boolean =
    !isCancelled && status != EventStatus.DONE

fun visibleMyActiveIds(
    serverIds: List<String>,
    pinnedIds: Set<String>,
    dismissedIds: Set<String>,
): List<String> {
    val seen = LinkedHashSet<String>()
    for (id in serverIds) if (id !in dismissedIds) seen += id
    for (id in pinnedIds) if (id !in dismissedIds) seen += id
    return seen.toList()
}

fun canDeleteUnassignedEvent(canManageUnit: Boolean, responderCount: Int): Boolean =
    canManageUnit && responderCount == 0
