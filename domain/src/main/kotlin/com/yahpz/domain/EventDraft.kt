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
const val EVENT_DRAFT_FORM_ERROR = "יש למלא תאריך וסוג אירוע כדי ליצור אירוע."
const val EVENT_DRAFT_FORM_LOCATION_ERROR = "יש למלא תאריך, סוג אירוע ומיקום כדי ליצור אירוע."
const val EVENT_DRAFT_SAVE_FAILED = "שמירת האירוע נכשלה. בדקו את החיבור ונסו שוב."

/** Event-level complete is blocked until `events.ended_at` is set. */
const val EVENT_DONE_NEEDS_END_ERROR = "לא ניתן להשלים אירוע ללא שעת סיום."

/** Event-level complete is blocked until every assigned lead `total_km` is set. */
const val EVENT_DONE_NEEDS_KM_ERROR =
    "לא ניתן להשלים אירוע לפני הזנת קילומטרים לכל הכוננים."
const val EVENT_DRAFT_SAVED = "האירוע נשמר."
const val EVENT_NEW_TITLE = "אירוע חדש"
const val EVENT_EDIT_TITLE = "עריכת אירוע"
const val EVENT_SAVE_TITLE = "שמירת אירוע"
const val EVENT_SAVE_DRAFT_TITLE = "שמירת טיוטה"
const val EVENT_CREATE_TITLE = "יצירת אירוע"
const val EVENT_SAVE_AS_DRAFT_TITLE = "שמירה כטיוטה"
const val EVENT_FORM_DETAILS_SECTION = "פרטי האירוע"
const val EVENT_FORM_RESPONDERS_SECTION = "מתנדבים"
const val EVENT_START_TIME_LABEL = "זמן התחלה"
const val EVENT_END_TIME_LABEL = "זמן סיום"
const val TIME_NOW_LABEL = "עכשיו"

fun eventFormPrimarySaveTitle(editing: Boolean): String =
    if (editing) EVENT_SAVE_TITLE else EVENT_CREATE_TITLE

fun eventFormSecondarySaveTitle(editing: Boolean): String =
    if (editing) EVENT_SAVE_DRAFT_TITLE else EVENT_SAVE_AS_DRAFT_TITLE

/** Create-event write: [error] on failure, [eventId] after a successful insert. */
data class EventWriteOutcome(val error: String? = null, val eventId: String? = null)
const val EVENT_DRAFT_PARTIAL_SAVED = "הטיוטה נשמרה."
const val EVENT_PATROL_CALLSIGN_LABEL = PATROL_CALLSIGN_NUMBER_LABEL
const val EVENT_STATION_LABEL = "תחנה"
const val STATION_MAX_LENGTH = 80
const val MY_ACTIVE_EVENTS_TITLE = "האירועים הפעילים שלי"
const val MY_ACTIVE_EVENT_DISMISSED = "הוסר מהאירועים הפעילים."
const val NO_VEHICLE_KM_PLACEHOLDER = "מתנדב ללא רכב"
const val EVENT_ASSIGN_OPEN = "מתנדבים"
const val EVENT_ASSIGN_CLOSE = "סגירת הקצאה"
const val EVENT_ASSIGN_REMOVE = "הסרת מתנדב"
const val EVENT_ASSIGN_REMOVE_CANCEL = "ביטול"
const val EVENT_ASSIGN_EMPTY = "בלי מתנדב משובץ האירוע נשאר בהזנה ואינו מוצג למתנדבים."
const val EVENT_ASSIGN_EDIT_HINT = "ק״מ"
const val EVENT_SELF_ASSIGN_ON_CREATE_ERROR = "לא ניתן לשבץ את יוצר האירוע כמתנדב."
const val EVENT_SELF_ASSIGN_DISABLED_HINT = "לא ניתן לשבץ"
const val EVENT_EDIT_LOAD_FAILED = "טעינת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val UNIT_EVENTS_LOAD_FAILED = "טעינת האירועים נכשלה. בדקו את החיבור ונסו שוב."
const val NEW_RESPONDER_EMERGENCY_MEANS = true

/** Single system שלוחה that makes מיקום mandatory. Matches the web `systemDistricts`. */
const val SYSTEM_DISTRICT_CODE = "station_other_duplicated"

data class TreatedVehicleDraft(
    val vehicleKindId: String,
    val quantity: Int,
)

data class EventResponderDraft(
    val responderId: String,
    val assignmentId: String = "",
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
    val patrolCallsignPrefix: String = "",
    val patrolCallsignNumber: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val createdAt: String = "",
    val eventTypeId: String = "",
    val roadId: String = "",
    val districtId: String = "",
    val location: String = "",
    val locationPlaceId: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationPinSource: String? = null,
    val locationPinnedAt: String? = null,
    val locationPinnedBy: String? = null,
    val station: String = "",
    val notes: String = "",
    val responders: List<EventResponderDraft> = emptyList(),
    val isCancelled: Boolean = false,
    val busLane: Boolean = false,
    val shiftLeadId: String = "",
    val secondaryLeads: List<SecondaryLead> = emptyList(),
) {
    val responderIds: List<String> get() = responders.map { it.responderId }

    val locationPin: LocationPinFields
        get() = LocationPinFields(
            location = location,
            locationPlaceId = locationPlaceId,
            locationLat = locationLat,
            locationLng = locationLng,
            locationPinSource = locationPinSource,
            locationPinnedAt = locationPinnedAt,
            locationPinnedBy = locationPinnedBy,
        )
}

fun EventDraft.withLocationPin(pin: LocationPinFields): EventDraft = copy(
    location = pin.location,
    locationPlaceId = pin.locationPlaceId,
    locationLat = pin.locationLat,
    locationLng = pin.locationLng,
    locationPinSource = pin.locationPinSource,
    locationPinnedAt = pin.locationPinnedAt,
    locationPinnedBy = pin.locationPinnedBy,
)

/** Equality for confirm-without-change: ignore client-only flags that refresh after load. */
fun EventDraft.forPersistCompare(): EventDraft = copy(
    responders = responders.map { it.copy(hasVehicle = false) },
    secondaryLeads = secondaryLeads.map { it.copy(addedAt = null) },
)

data class EventDraftErrors(
    val eventDate: String? = null,
    val eventType: String? = null,
    val road: String? = null,
    val location: String? = null,
    val patrolCallsignNumber: String? = null,
) {
    val isEmpty: Boolean get() =
        eventDate == null && eventType == null && road == null && location == null &&
            patrolCallsignNumber == null

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

/** Optional תחנה name — same system שלוחה as mandatory מיקום. */
fun districtNeedsStation(districts: List<LookupOption>, districtId: String): Boolean =
    districtNeedsLocation(districts, districtId)

fun stationForSave(districts: List<LookupOption>, districtId: String, station: String): String? {
    if (!districtNeedsStation(districts, districtId)) return null
    val trimmed = station.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.take(STATION_MAX_LENGTH)
}

fun stationAfterDistrictChange(
    districts: List<LookupOption>,
    nextDistrictId: String,
    currentStation: String,
): String = if (districtNeedsStation(districts, nextDistrictId)) currentStation else ""

/** Draft save: date only, matching web `allowPartial`. */
fun validateEventDraftPartial(draft: EventDraft): EventDraftErrors =
    EventDraftErrors(
        eventDate = if (normalizeReturnDate(draft.eventDate) == null) EVENT_DRAFT_DATE_ERROR else null,
    )

/**
 * Minimum to create an event: date + event type (+ מיקום for the system שלוחה).
 * או״ק, כביש, and מספר אירוע may be empty — the event is saved and a responder
 * can be attached, but it stays hidden from responders until all three are filled.
 */
fun validateEventDraft(draft: EventDraft, districts: List<LookupOption> = emptyList()): EventDraftErrors =
    EventDraftErrors(
        eventDate = if (normalizeReturnDate(draft.eventDate) == null) EVENT_DRAFT_DATE_ERROR else null,
        eventType = if (draft.eventTypeId.isEmpty()) EVENT_DRAFT_TYPE_ERROR else null,
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

/**
 * `guard_event_done_requirements` reads assigned `total_km` already in the
 * database. The event row is written before responders, so a derived `done`
 * must wait — otherwise filling the last KM 400s and the generic toast
 * pretends it was a network error.
 */
fun eventStatusForRowWriteBeforeResponders(derived: EventStatus): EventStatus =
    if (derived == EventStatus.DONE) EventStatus.PARTIAL else derived

/** Map the Hebrew done-guard (and keep the generic save toast for anything else). */
fun eventPersistFailure(message: String?): String {
    val raw = message.orEmpty()
    if (raw.contains(EVENT_DONE_NEEDS_KM_ERROR)) return EVENT_DONE_NEEDS_KM_ERROR
    if (raw.contains(EVENT_DONE_NEEDS_END_ERROR)) return EVENT_DONE_NEEDS_END_ERROR
    return EVENT_DRAFT_SAVE_FAILED
}

/** A new event with no responders is a draft; adding pending crew opens it for documentation. */
fun eventDraftStatus(responderCount: Int): EventStatus =
    if (responderCount == 0) EventStatus.DRAFT else EventStatus.IN_PROGRESS

/** Any lead-owned field the אחמ״ש typed or toggled on this assignment. */
fun eventResponderHasFilledFields(row: EventResponderDraft): Boolean {
    if (row.hasVehicle && row.totalKm.isNotBlank()) return true
    // On a not-yet-saved assignment אמצעים is only the default, not entered data.
    if (row.emergencyMeans && row.assignmentId.isNotBlank()) return true
    return row.treated.any { it.quantity > 0 }
}

fun eventResponderRemoveConfirm(name: String): String {
    val trimmed = name.trim().ifEmpty { "מתנדב" }
    return "האם אתה בטוח שברצונך להסיר את $trimmed?"
}

fun eventDraftSummary(responderCount: Int): String = when (responderCount) {
    0 -> "טרם הוקצו מתנדבים · אירוע בהזנה"
    1 -> "מתנדב אחד משובץ"
    else -> "$responderCount מתנדבים משובצים"
}

/** Caption under an assigned responder — teaches that the row opens hours / km. */
fun assignedResponderCaption(
    totalKm: String,
    hasVehicle: Boolean,
): String {
    val km = if (hasVehicle && totalKm.isNotBlank()) {
        "${totalKm.trim()} ק״מ"
    } else {
        null
    }
    return km ?: "ק״מ"
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
        selected + EventResponderDraft(
            responderId = responderId,
            emergencyMeans = NEW_RESPONDER_EMERGENCY_MEANS,
            hasVehicle = hasVehicle,
        )
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

const val EVENT_CANCEL_ADMIN_ONLY = "רק מנהל או אחמ״ש יכולים לבטל סימון בוטל."
const val EVENT_CANCELLED_LABEL = "בוטל"

@Suppress("UNUSED_PARAMETER")
fun eventCancelToggleLabel(isCancelled: Boolean): String = EVENT_CANCELLED_LABEL

fun eventCancelToast(isCancelled: Boolean): String =
    if (isCancelled) "האירוע סומן כבוטל." else "סימון הביטול הוסר."

/** Clearing `is_cancelled` is allowed for admin, super_admin, and shift_lead. */
fun canToggleEventCancelled(next: Boolean, canClearCancelled: Boolean): String? =
    if (!next && !canClearCancelled) EVENT_CANCEL_ADMIN_ONLY else null

const val POLICE_EVENT_ID_DUPLICATE_ERROR = "כבר קיים אירוע עם המספר הזה באותו תאריך."

data class SameDayPoliceEventRow(
    val id: String,
    val shiftLeadId: String? = null,
    val isCancelled: Boolean = false,
    val eventDate: String = "",
    val policeEventId: String? = null,
)

fun sameDayPoliceEventIdCollides(
    eventDate: String,
    policeEventId: String,
    currentEventId: String?,
    existing: List<SameDayPoliceEventRow>,
): Boolean {
    val policeId = digitsOnly(policeEventId)
    val date = eventDate.trim()
    if (policeId.isEmpty() || date.isEmpty()) return false
    return existing.any { row ->
        if (row.isCancelled) return@any false
        if (!currentEventId.isNullOrBlank() && row.id == currentEventId) return@any false
        if (row.eventDate.isNotEmpty() && row.eventDate.trim() != date) return@any false
        digitsOnly(row.policeEventId.orEmpty()) == policeId
    }
}

/** Own same-day מספר אירוע after a create whose response never came back. */
fun ownResumableEventId(
    currentEventId: String?,
    viewerLeadId: String,
    existing: List<SameDayPoliceEventRow>,
): String? {
    if (!currentEventId.isNullOrBlank()) return null
    val mine = existing.filter { !it.isCancelled && it.shiftLeadId == viewerLeadId }
    return mine.singleOrNull()?.id
}

private fun String.nilIfBlank(): String? = trim().ifEmpty { null }

/**
 * Columns an event save is allowed to write. A later edit sends only the keys
 * that differ, so entering kilometers cannot null location, notes, or anything
 * this form did not change.
 */
fun eventPersistColumns(
    draft: EventDraft,
    districts: List<LookupOption>,
    mainLeadId: String,
): Map<String, Any?> {
    val locationPayload = buildLocationPayload(draft.locationPin)
    val callsign = resolvePatrolCallsign(
        draft.patrolCallsignPrefix,
        draft.patrolCallsignNumber,
        draft.patrolCallsign,
    )
    val eventDate = normalizeReturnDate(draft.eventDate) ?: draft.eventDate.trim()
    val overnight = isOvernightEnd(draft.startTime, draft.endTime)
    val prefix = patrolCallsignPrefixForInput(callsign.prefix).trim().nilIfBlank()
    val number = patrolCallsignNumberForInput(callsign.number).nilIfBlank()
    val rowStatus = eventStatusForRowWriteBeforeResponders(
        deriveEventStatusFromDraft(draft.responders),
    )
    return mapOf(
        "event_date" to eventDate,
        "police_event_id" to draft.policeEventId.nilIfBlank(),
        "district_id" to draft.districtId.nilIfBlank(),
        "patrol_callsign" to formatPatrolCallsign(prefix, number).nilIfBlank(),
        "patrol_callsign_prefix" to prefix,
        "patrol_callsign_number" to number,
        "started_at" to wallTimestamp(eventDate, draft.startTime, 0),
        "ended_at" to wallTimestamp(eventDate, draft.endTime, if (overnight) 1 else 0),
        "event_type_id" to draft.eventTypeId.nilIfBlank(),
        "road_id" to draft.roadId.nilIfBlank(),
        "location" to locationPayload.locationOrNull(),
        "location_place_id" to locationPayload.locationPlaceId,
        "location_lat" to locationPayload.locationLat,
        "location_lng" to locationPayload.locationLng,
        "location_pin_source" to locationPayload.locationPinSource,
        "location_pinned_at" to locationPayload.locationPinnedAt,
        "location_pinned_by" to locationPayload.locationPinnedBy,
        "station" to stationForSave(districts, draft.districtId, draft.station),
        "notes" to draft.notes.nilIfBlank(),
        "is_cancelled" to draft.isCancelled,
        "bus_lane" to draft.busLane,
        "status" to rowStatus.raw,
        "shift_lead_id" to mainLeadId,
    )
}

/** Equal blanks stay out of the patch so they cannot null a stored column. */
fun changedPersistColumns(
    previous: Map<String, Any?>,
    next: Map<String, Any?>,
): Map<String, Any?> {
    val keys = previous.keys + next.keys
    return keys.mapNotNull { key ->
        val before = previous[key]
        val after = next[key]
        if (before == after) null else key to after
    }.toMap()
}

fun shouldRewriteTreatedVehicles(
    previous: List<TreatedVehicleDraft>?,
    next: List<TreatedVehicleDraft>,
): Boolean {
    fun key(rows: List<TreatedVehicleDraft>) =
        rows.filter { it.quantity > 0 }
            .map { "${it.vehicleKindId}:${it.quantity}" }
            .sorted()
            .joinToString("|")
    val nextKey = key(next)
    if (previous == null) return nextKey.isNotEmpty()
    return key(previous) != nextKey
}

fun responderLeadPatch(
    previous: EventResponderDraft?,
    next: EventResponderDraft,
    startedAt: String?,
    endedAt: String?,
    previousStartedAt: String?,
    previousEndedAt: String?,
): Map<String, Any?> {
    val km = leadKmForSave(next.hasVehicle, next.totalKm)
    if (previous == null) {
        return buildMap {
            put("started_at", startedAt)
            if (endedAt != null) put("ended_at", endedAt)
            put("total_km", km)
            put("emergency_means", next.emergencyMeans)
        }
    }
    val previousKm = leadKmForSave(previous.hasVehicle, previous.totalKm)
    return buildMap {
        if (previousKm != km) put("total_km", km)
        if (previous.emergencyMeans != next.emergencyMeans) put("emergency_means", next.emergencyMeans)
        if (previousStartedAt != startedAt) put("started_at", startedAt)
        if (endedAt != null && previousEndedAt != endedAt) put("ended_at", endedAt)
    }
}

fun secondaryLeadsUnchanged(
    previous: List<SecondaryLead>?,
    next: List<SecondaryLead>,
): Boolean {
    fun key(rows: List<SecondaryLead>) =
        rows.map { "${it.userId}:${if (it.locked) 1 else 0}" }.sorted().joinToString("|")
    if (previous == null) return next.isEmpty()
    return key(previous) == key(next)
}

/**
 * True once the lead has typed something past the prefilled date and start
 * time. Opening "אירוע חדש" must not insert a row by itself.
 */
fun EventDraft.hasDetailBeyondOpen(): Boolean =
    policeEventId.isNotBlank() ||
        patrolCallsignPrefix.isNotBlank() ||
        patrolCallsignNumber.isNotBlank() ||
        endTime.isNotBlank() ||
        eventTypeId.isNotBlank() ||
        roadId.isNotBlank() ||
        districtId.isNotBlank() ||
        location.isNotBlank() ||
        station.isNotBlank() ||
        notes.isNotBlank() ||
        responders.isNotEmpty() ||
        secondaryLeads.isNotEmpty() ||
        isCancelled ||
        busLane
