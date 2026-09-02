package com.yahpz.domain

/** A closed-list row (roads, event types, שלוחות) used by the create forms. */
data class LookupOption(
    val id: String,
    val name: String,
    val code: String? = null,
    val sortOrder: Int = 0,
)

fun sortLookupsBySortOrder(items: List<LookupOption>): List<LookupOption> =
    items.sortedWith(compareBy({ it.sortOrder }, { it.name }))

/** An active profile that a shift lead may assign to an event or a shift. */
data class AssignableProfile(
    val id: String,
    val fullName: String,
    val callsign: String,
) {
    val display: String get() = personDisplay(fullName, callsign)

    val searchFields: List<String?> get() = listOf(fullName, callsign)
}

const val EVENT_DRAFT_DATE_ERROR = "יש לבחור תאריך."
const val EVENT_DRAFT_TYPE_ERROR = "יש לבחור סוג אירוע."
const val EVENT_DRAFT_ROAD_ERROR = "יש לבחור כביש."
const val EVENT_DRAFT_LOCATION_ERROR = "יש לבחור או להזין מיקום."
const val EVENT_DRAFT_FORM_ERROR = "יש למלא תאריך, סוג אירוע וכביש כדי ליצור אירוע."
const val EVENT_DRAFT_FORM_LOCATION_ERROR = "יש למלא תאריך, סוג אירוע, כביש ומיקום כדי ליצור אירוע."
const val EVENT_DRAFT_SAVE_FAILED = "שמירת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val EVENT_DRAFT_SAVED = "האירוע נשמר."
const val EVENT_NEW_TITLE = "אירוע חדש"
const val EVENT_EDIT_TITLE = "עריכת אירוע"
const val EVENT_SAVE_TITLE = "שמירת אירוע"
const val EVENT_SAVE_DRAFT_TITLE = "שמירת טיוטה"
const val EVENT_DRAFT_PARTIAL_SAVED = "הטיוטה נשמרה."
const val EVENT_PATROL_CALLSIGN_LABEL = "או״ק ניידת"
const val MY_ACTIVE_EVENTS_TITLE = "האירועים הפעילים שלי"
const val MY_ACTIVE_EVENT_DISMISSED = "הוסר מהאירועים הפעילים."
const val NO_VEHICLE_KM_PLACEHOLDER = "מתנדב ללא רכב"
const val EVENT_ASSIGN_OPEN = "מתנדבים"
const val EVENT_ASSIGN_CLOSE = "סגירת הקצאה"
const val EVENT_ASSIGN_REMOVE = "הסרת מתנדב"
const val EVENT_ASSIGN_EMPTY = "בלי מתנדב משובץ האירוע נשאר בהזנה ואינו מוצג למתנדבים."
const val EVENT_SELF_ASSIGN_ON_CREATE_ERROR = "לא ניתן לשבץ את יוצר האירוע כמתנדב."
const val EVENT_SELF_ASSIGN_DISABLED_HINT = "לא ניתן לשבץ"
const val EVENT_EDIT_LOAD_FAILED = "טעינת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val UNIT_EVENTS_LOAD_FAILED = "טעינת האירועים נכשלה. בדקו את החיבור ונסו שוב."

/** Single system שלוחה that makes מיקום mandatory. Matches the web `systemDistricts`. */
const val SYSTEM_DISTRICT_CODE = "station_other_duplicated"

data class TreatedVehicleDraft(
    val vehicleKindId: String,
    val quantity: Int,
)

data class EventResponderDraft(
    val responderId: String,
    val assignmentId: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val totalKm: String = "",
    val emergencyMeans: Boolean = false,
    val treated: List<TreatedVehicleDraft> = emptyList(),
    val status: ParticipationStatus = ParticipationStatus.PENDING,
    val hasVehicle: Boolean = true,
)

data class EventDraft(
    val eventDate: String,
    val policeEventId: String = "",
    val patrolCallsign: String = "",
    val eventTypeId: String = "",
    val roadId: String = "",
    val districtId: String = "",
    val location: String = "",
    val notes: String = "",
    val responders: List<EventResponderDraft> = emptyList(),
    val isCancelled: Boolean = false,
    val busLane: Boolean = false,
) {
    val responderIds: List<String> get() = responders.map { it.responderId }
}

data class EventDraftErrors(
    val eventDate: String? = null,
    val eventType: String? = null,
    val road: String? = null,
    val location: String? = null,
) {
    val isEmpty: Boolean get() = eventDate == null && eventType == null && road == null && location == null

    /** Matching web copy: the location variant only when מיקום is the missing piece. */
    val formMessage: String?
        get() = when {
            isEmpty -> null
            location != null -> EVENT_DRAFT_FORM_LOCATION_ERROR
            else -> EVENT_DRAFT_FORM_ERROR
        }
}

fun districtNeedsLocation(districts: List<LookupOption>, districtId: String): Boolean {
    if (districtId.isEmpty()) return false
    return districts.firstOrNull { it.id == districtId }?.code == SYSTEM_DISTRICT_CODE
}

/** Draft save: date only, matching web `allowPartial`. */
fun validateEventDraftPartial(draft: EventDraft): EventDraftErrors =
    EventDraftErrors(
        eventDate = if (normalizeReturnDate(draft.eventDate) == null) EVENT_DRAFT_DATE_ERROR else null,
    )

/** Minimum to create an event: date + event type + road (+ מיקום for the system שלוחה). */
fun validateEventDraft(draft: EventDraft, districts: List<LookupOption> = emptyList()): EventDraftErrors =
    EventDraftErrors(
        eventDate = if (normalizeReturnDate(draft.eventDate) == null) EVENT_DRAFT_DATE_ERROR else null,
        eventType = if (draft.eventTypeId.isEmpty()) EVENT_DRAFT_TYPE_ERROR else null,
        road = if (draft.roadId.isEmpty()) EVENT_DRAFT_ROAD_ERROR else null,
        location = if (districtNeedsLocation(districts, draft.districtId) && draft.location.isBlank()) {
            EVENT_DRAFT_LOCATION_ERROR
        } else {
            null
        },
    )

/** End clock earlier than start ⇒ overnight (end on event_date + 1). */
fun isOvernightEnd(startTime: String, endTime: String): Boolean {
    if (startTime.isBlank() || endTime.isBlank()) return false
    return endTime < startTime
}

/** Wall-clock timestamp for Postgres `timestamp without time zone`. */
fun wallTimestamp(eventDate: String, timeHm: String, dayOffset: Int = 0): String? {
    val time = timeHm.trim()
    if (time.isEmpty()) return null
    val ymd = normalizeReturnDate(eventDate) ?: return null
    val date = if (dayOffset == 0) ymd else addCalendarDays(ymd, dayOffset)
    val normalized = if (time.length == 5) "$time:00" else time
    return "${date}T$normalized"
}

fun toTimeInput(value: String?): String = formatTime(value).orEmpty()

/** Lead `total_km` is never stored for a responder with no active vehicle. */
fun leadKmForSave(hasVehicle: Boolean, totalKm: String): Double? {
    if (!hasVehicle) return null
    val trimmed = totalKm.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.toDoubleOrNull()
}

data class FillReadyPreviousRow(val id: String, val totalKm: Double? = null)

data class FillReadyNextRow(val assignmentId: String, val totalKm: Double? = null)

fun assignmentIdsNewlyAssigned(
    previous: List<FillReadyPreviousRow>,
    next: List<FillReadyNextRow>,
): List<String> {
    val prevIds = previous.map { it.id }.toSet()
    return next.filter { it.assignmentId !in prevIds }.map { it.assignmentId }
}

fun assignmentIdsNewlySetKm(
    previous: List<FillReadyPreviousRow>,
    next: List<FillReadyNextRow>,
): List<String> {
    val prevById = previous.associate { it.id to it.totalKm }
    return next.mapNotNull { row ->
        if (row.totalKm == null) return@mapNotNull null
        val prev = prevById[row.assignmentId]
        if (prev == null) row.assignmentId else null
    }
}

/** Notify on first assignment, and still on first km for rows that were already assigned. */
fun fillReadyNotifyIds(
    previous: List<FillReadyPreviousRow>,
    next: List<FillReadyNextRow>,
): List<String> = (assignmentIdsNewlyAssigned(previous, next) + assignmentIdsNewlySetKm(previous, next))
    .distinct()

fun deriveEventStatusFromDraft(responders: List<EventResponderDraft>): EventStatus {
    if (responders.isEmpty()) return EventStatus.DRAFT
    if (responders.all { it.status == ParticipationStatus.DONE }) return EventStatus.DONE
    if (responders.any { it.status == ParticipationStatus.DONE }) return EventStatus.PARTIAL
    return EventStatus.IN_PROGRESS
}

/** A new event with no responders is a draft; adding pending crew opens it for documentation. */
fun eventDraftStatus(responderCount: Int): EventStatus =
    if (responderCount == 0) EventStatus.DRAFT else EventStatus.IN_PROGRESS

fun eventDraftSummary(responderCount: Int): String = when (responderCount) {
    0 -> "טרם הוקצו מתנדבים · אירוע בהזנה"
    1 -> "מתנדב אחד משובץ"
    else -> "$responderCount מתנדבים משובצים"
}

/** When entering the system שלוחה the web defaults כביש to the road containing 101. */
fun defaultRoadIdForSystemDistrict(roads: List<LookupOption>): String? =
    roads.firstOrNull { it.name.contains("101") }?.id

/** Entering the system שלוחה preselects the 101 road; any other change leaves כביש alone. */
fun applyDistrictRoadDefault(
    previousDistrictId: String,
    nextDistrictId: String,
    districts: List<LookupOption>,
    roads: List<LookupOption>,
    currentRoadId: String,
): String {
    val entering = !districtNeedsLocation(districts, previousDistrictId) &&
        districtNeedsLocation(districts, nextDistrictId)
    if (!entering) return currentRoadId
    return defaultRoadIdForSystemDistrict(roads) ?: currentRoadId
}

/** Event crew has no ceiling, unlike a shift. */
fun toggleEventResponder(
    selected: List<EventResponderDraft>,
    responderId: String,
    hasVehicle: Boolean = true,
): List<EventResponderDraft> =
    if (selected.any { it.responderId == responderId }) {
        selected.filterNot { it.responderId == responderId }
    } else {
        selected + EventResponderDraft(responderId = responderId, hasVehicle = hasVehicle)
    }

fun updateEventResponder(
    responders: List<EventResponderDraft>,
    responderId: String,
    transform: (EventResponderDraft) -> EventResponderDraft,
): List<EventResponderDraft> =
    responders.map { if (it.responderId == responderId) transform(it) else it }

fun bumpTreatedVehicle(
    responders: List<EventResponderDraft>,
    responderId: String,
    vehicleKindId: String,
    delta: Int,
): List<EventResponderDraft> = updateEventResponder(responders, responderId) { row ->
    val current = row.treated.firstOrNull { it.vehicleKindId == vehicleKindId }?.quantity ?: 0
    val next = (current + delta).coerceAtLeast(0)
    val treated = if (next == 0) {
        row.treated.filterNot { it.vehicleKindId == vehicleKindId }
    } else {
        val rest = row.treated.filterNot { it.vehicleKindId == vehicleKindId }
        rest + TreatedVehicleDraft(vehicleKindId = vehicleKindId, quantity = next)
    }
    row.copy(treated = treated)
}

fun treatedQuantity(responder: EventResponderDraft, vehicleKindId: String): Int =
    responder.treated.firstOrNull { it.vehicleKindId == vehicleKindId }?.quantity ?: 0

fun filterAssignableProfiles(profiles: List<AssignableProfile>, query: String): List<AssignableProfile> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return profiles
    return profiles.filter { fieldsMatchQuery(it.searchFields, trimmed) }
}

fun createIncludesSelfAssign(shiftLeadId: String, responders: List<EventResponderDraft>): Boolean =
    responders.any { it.responderId == shiftLeadId }

fun isSelfAssignDisabledOnCreate(isCreate: Boolean, currentUserId: String?, profileId: String): Boolean =
    isCreate && !currentUserId.isNullOrEmpty() && profileId == currentUserId

const val EVENT_CANCEL_ADMIN_ONLY = "רק מנהל יכול לבטל סימון בוטל."
const val EVENT_CANCELLED_LABEL = "בוטל"

@Suppress("UNUSED_PARAMETER")
fun eventCancelToggleLabel(isCancelled: Boolean): String = EVENT_CANCELLED_LABEL

fun eventCancelToast(isCancelled: Boolean): String =
    if (isCancelled) "האירוע סומן כבוטל." else "סימון הביטול הוסר."

/** Clearing `is_cancelled` is admin-only, same rule as the web event form. */
fun canToggleEventCancelled(next: Boolean, viewerIsAdmin: Boolean): String? =
    if (!next && !viewerIsAdmin) EVENT_CANCEL_ADMIN_ONLY else null
