package com.yahpz.domain

const val MY_ACTIVE_EVENTS_EMPTY = "אין אירועים פעילים באחמו\"ש שלך"

data class ActivePref(val eventId: String, val kind: String)

fun prefsAfterAddToMyActive(
    prefs: List<ActivePref>,
    eventId: String,
    alreadyAuto: Boolean,
): List<ActivePref> {
    val others = prefs.filterNot { it.eventId == eventId }
    return if (alreadyAuto) others else others + ActivePref(eventId, "pin")
}

fun prefsRestoringEvent(
    prefs: List<ActivePref>,
    eventId: String,
    previous: List<ActivePref>,
): List<ActivePref> = prefs.filterNot { it.eventId == eventId } + previous

const val MY_ACTIVE_EVENT_PINNED = "נוסף לאירועים הפעילים."
const val MY_ACTIVE_ADD = "הוספה לפעילים"
const val MY_ACTIVE_REMOVE = "הסרה"
const val MY_ACTIVE_REMOVE_LOCKED = "אירוע בהזנה — לא ניתן להסיר"
const val MY_ACTIVE_PREF_FAILED = "עדכון האירועים הפעילים נכשל. בדקו את החיבור ונסו שוב."
const val MY_ACTIVE_DRAG_TO_ACTIVE = "הוספה לפעילים, או לחיצה ארוכה וגרירה"
const val MY_ACTIVE_DROP_TO_ADD = "שחררו כאן להוספה לפעילים"
const val MY_ACTIVE_DRAG_TO_ADD = "גררו לכאן להוספה לפעילים"

/** Lead-owned events that stay on האירועים הפעילים שלי until done, cancelled, or הסרה. */
val AUTO_MY_ACTIVE_STATUSES = setOf(
    EventStatus.DRAFT,
    EventStatus.IN_PROGRESS,
    EventStatus.PARTIAL,
)

fun isAutoOnMyActive(isCancelled: Boolean, status: EventStatus): Boolean =
    !isCancelled && status in AUTO_MY_ACTIVE_STATUSES

const val EVENT_DELETE_TITLE = "מחיקת אירוע"
const val EVENT_DELETE_CONFIRM = "למחוק את האירוע? אין מתנדבים משובצים."
const val EVENT_DELETE_ACTION = "מחיקה"
const val EVENT_DELETED = "האירוע נמחק."
const val EVENT_DELETE_FAILED = "מחיקת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val EVENT_DELETE_OTHER_LEAD = "אין הרשאה למחוק אירוע שנוצר על ידי אחמ״ש אחר."

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

fun canDeleteUnassignedEvent(
    canManageUnit: Boolean,
    responderCount: Int,
    viewerIsAdmin: Boolean,
    viewerId: String?,
    shiftLeadId: String?,
): Boolean {
    if (!canManageUnit || responderCount != 0) return false
    if (viewerIsAdmin) return true
    return viewerId != null && viewerId == shiftLeadId
}
