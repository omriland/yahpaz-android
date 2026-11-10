package com.yahpz.domain

/**
 * Incomplete-event detection for the unit (אחמ״ש) event list.
 *
 * An event is incomplete when one or more required documentation fields are
 * missing. Incomplete events pin to the top of the list regardless of status
 * (including ממתין לתיעוד).
 */
enum class IncompleteField {
    POLICE_EVENT_ID,
    PATROL_CALLSIGN,
    DISTRICT,
    EVENT_TYPE,
    ROAD,
    LOCATION,
    RESPONDER_KM,
    EVENT_TIMES,
}

val INCOMPLETE_FIELD_LABELS: Map<IncompleteField, String> = mapOf(
    IncompleteField.POLICE_EVENT_ID to "מספר אירוע",
    IncompleteField.PATROL_CALLSIGN to "אוק - מס",
    IncompleteField.DISTRICT to "שלוחה",
    IncompleteField.EVENT_TYPE to "סוג אירוע",
    IncompleteField.ROAD to "כביש",
    IncompleteField.LOCATION to "מיקום",
    IncompleteField.RESPONDER_KM to "ק״מ",
    IncompleteField.EVENT_TIMES to "שעות",
)

const val INCOMPLETE_EVENTS_HEADING = "דורשים השלמת פרטים"
const val INCOMPLETE_NOTICE_MARK = "פרטים חסרים:"
const val HELD_FROM_RESPONDERS_LABEL = "לא נשלח למתנדבים"
const val RESPONDERS_HELD_FOR_POLICE_ID_NOTE =
    "האירוע לא יוצג למתנדב עד שאו״ק, כביש ומספר אירוע מלאים."

data class IncompleteResponderSnapshot(
    val totalKm: Double? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val responderId: String = "",
    val status: ParticipationStatus = ParticipationStatus.PENDING,
)

data class IncompleteEventSnapshot(
    val origin: String? = null,
    val policeEventId: String? = null,
    val patrolCallsign: String? = null,
    val patrolCallsignNumber: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val hasDistrict: Boolean = false,
    val hasEventType: Boolean = false,
    val hasRoad: Boolean = false,
    val location: String? = null,
    val responders: List<IncompleteResponderSnapshot> = emptyList(),
    val status: EventStatus = EventStatus.DRAFT,
    val isCancelled: Boolean = false,
    val shiftLeadId: String? = null,
    val secondaryLeadIds: List<String> = emptyList(),
)

private fun isMissing(value: String?): Boolean = value.isNullOrBlank()

fun missingEventFields(event: IncompleteEventSnapshot): Set<IncompleteField> {
    val missing = linkedSetOf<IncompleteField>()
    if (isMissing(event.policeEventId)) missing += IncompleteField.POLICE_EVENT_ID
    val callsign = resolvePatrolCallsign(null, event.patrolCallsignNumber, event.patrolCallsign)
    if (isMissing(callsign.number)) missing += IncompleteField.PATROL_CALLSIGN
    if (!event.hasDistrict) missing += IncompleteField.DISTRICT
    if (!event.hasEventType) missing += IncompleteField.EVENT_TYPE
    if (!event.hasRoad) missing += IncompleteField.ROAD
    if (isMissing(event.location)) missing += IncompleteField.LOCATION

    if (event.startedAt != null || event.endedAt != null) {
        if (isMissing(event.startedAt) || isMissing(event.endedAt)) {
            missing += IncompleteField.EVENT_TIMES
        }
    } else {
        for (responder in event.responders) {
            if (isMissing(responder.startedAt) || isMissing(responder.endedAt)) {
                missing += IncompleteField.EVENT_TIMES
                break
            }
        }
    }

    for (responder in event.responders) {
        if (responder.totalKm == null) missing += IncompleteField.RESPONDER_KM
        if (missing.contains(IncompleteField.RESPONDER_KM)) break
    }
    return missing
}

fun incompleteFieldLabels(fields: Set<IncompleteField>): List<String> =
    IncompleteField.entries.filter { it in fields }.map { INCOMPLETE_FIELD_LABELS.getValue(it) }

fun incompleteNoticeLabel(fields: Set<IncompleteField>): String =
    "חסרים: ${incompleteFieldLabels(fields).joinToString(" · ")}"

fun incompleteLeadNoticeLabels(event: IncompleteEventSnapshot): List<String> {
    val missing = missingEventFields(event)
    val labels = incompleteFieldLabels(missing)
    val held = event.responders.isNotEmpty() && !eventReleasedToResponders(
        origin = event.origin,
        policeEventId = event.policeEventId,
        patrolCallsignNumber = event.patrolCallsignNumber,
        hasRoad = event.hasRoad,
        patrolCallsignLegacy = event.patrolCallsign,
    )
    return if (held) labels + HELD_FROM_RESPONDERS_LABEL else labels
}

fun isEventIncomplete(event: IncompleteEventSnapshot): Boolean =
    missingEventFields(event).isNotEmpty()

fun eventHasMissingResponderKm(event: IncompleteEventSnapshot): Boolean =
    IncompleteField.RESPONDER_KM in missingEventFields(event)

fun <T> partitionIncompleteEvents(
    events: List<T>,
    snapshot: (T) -> IncompleteEventSnapshot,
): Pair<List<T>, List<T>> {
    val incomplete = mutableListOf<T>()
    val rest = mutableListOf<T>()
    for (event in events) {
        if (isEventIncomplete(snapshot(event))) incomplete += event else rest += event
    }
    return incomplete to rest
}

private fun viewerLeadsEvent(event: IncompleteEventSnapshot, viewerId: String?): Boolean {
    val viewer = viewerId?.trim().orEmpty()
    if (viewer.isEmpty()) return false
    return event.shiftLeadId?.trim() == viewer || event.secondaryLeadIds.any { it.trim() == viewer }
}

/**
 * Lead-facing הוזן בהצלחה: the viewer leads this manual event, every field the
 * אחמ״ש owns is filled (no חסרים chip, so it is also released), and at least one
 * volunteer has not finished. Null when the viewer's own fill is still open, so
 * their to-do stamp wins. Shift-born reports are shared and have no lead part.
 */
fun leadEnteredAwaitingStamp(event: IncompleteEventSnapshot, viewerId: String?): StampDescriptor? {
    if (event.origin == SHIFT_ORIGIN || event.isCancelled) return null
    if (event.status != EventStatus.IN_PROGRESS && event.status != EventStatus.PARTIAL) return null
    if (!viewerLeadsEvent(event, viewerId)) return null
    val own = event.responders.firstOrNull { it.responderId == viewerId }
    if (own != null && own.status != ParticipationStatus.DONE) return null
    if (missingEventFields(event).isNotEmpty()) return null
    val open = event.responders.count { it.status != ParticipationStatus.DONE }
    return if (open > 0) leadEnteredStamp(open) else null
}
