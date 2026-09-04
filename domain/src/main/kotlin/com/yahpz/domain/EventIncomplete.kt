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
    RESPONDER_TIMES,
}

val INCOMPLETE_FIELD_LABELS: Map<IncompleteField, String> = mapOf(
    IncompleteField.POLICE_EVENT_ID to "מספר אירוע",
    IncompleteField.PATROL_CALLSIGN to "או״ק ניידת",
    IncompleteField.DISTRICT to "שלוחה",
    IncompleteField.EVENT_TYPE to "סוג אירוע",
    IncompleteField.ROAD to "כביש",
    IncompleteField.LOCATION to "מיקום",
    IncompleteField.RESPONDER_KM to "ק״מ",
    IncompleteField.RESPONDER_TIMES to "שעות",
)

const val INCOMPLETE_EVENTS_HEADING = "דורשים השלמת פרטים"
const val INCOMPLETE_NOTICE_MARK = "פרטים חסרים:"

data class IncompleteResponderSnapshot(
    val totalKm: Double? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
)

data class IncompleteEventSnapshot(
    val policeEventId: String? = null,
    val patrolCallsign: String? = null,
    val hasDistrict: Boolean = false,
    val hasEventType: Boolean = false,
    val hasRoad: Boolean = false,
    val location: String? = null,
    val responders: List<IncompleteResponderSnapshot> = emptyList(),
)

private fun isMissing(value: String?): Boolean = value.isNullOrBlank()

fun missingEventFields(event: IncompleteEventSnapshot): Set<IncompleteField> {
    val missing = linkedSetOf<IncompleteField>()
    if (isMissing(event.policeEventId)) missing += IncompleteField.POLICE_EVENT_ID
    if (isMissing(event.patrolCallsign)) missing += IncompleteField.PATROL_CALLSIGN
    if (!event.hasDistrict) missing += IncompleteField.DISTRICT
    if (!event.hasEventType) missing += IncompleteField.EVENT_TYPE
    if (!event.hasRoad) missing += IncompleteField.ROAD
    if (isMissing(event.location)) missing += IncompleteField.LOCATION

    for (responder in event.responders) {
        if (responder.totalKm == null) missing += IncompleteField.RESPONDER_KM
        if (isMissing(responder.startedAt) || isMissing(responder.endedAt)) {
            missing += IncompleteField.RESPONDER_TIMES
        }
        if (missing.contains(IncompleteField.RESPONDER_KM) &&
            missing.contains(IncompleteField.RESPONDER_TIMES)
        ) {
            break
        }
    }
    return missing
}

fun incompleteFieldLabels(fields: Set<IncompleteField>): List<String> =
    IncompleteField.entries.filter { it in fields }.map { INCOMPLETE_FIELD_LABELS.getValue(it) }

fun incompleteNoticeLabel(fields: Set<IncompleteField>): String =
    "חסרים: ${incompleteFieldLabels(fields).joinToString(" · ")}"

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
