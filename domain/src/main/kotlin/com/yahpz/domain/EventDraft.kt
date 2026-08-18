package com.yahpz.domain

/** A closed-list row (roads, event types, שלוחות) used by the create forms. */
data class LookupOption(
    val id: String,
    val name: String,
    val code: String? = null,
)

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
const val EVENT_EDIT_TITLE = "עריכת אירוע"
const val EVENT_EDIT_LOAD_FAILED = "טעינת האירוע נכשלה. בדקו את החיבור ונסו שוב."

/** Single system שלוחה that makes מיקום mandatory. Matches the web `systemDistricts`. */
const val SYSTEM_DISTRICT_CODE = "station_other_duplicated"

data class EventDraft(
    val eventDate: String,
    val policeEventId: String = "",
    val eventTypeId: String = "",
    val roadId: String = "",
    val districtId: String = "",
    val location: String = "",
    val notes: String = "",
    val responderIds: List<String> = emptyList(),
    val isCancelled: Boolean = false,
)

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

/** A new event with no responders is a draft; adding pending crew opens it for documentation. */
fun eventDraftStatus(responderCount: Int): EventStatus =
    if (responderCount == 0) EventStatus.DRAFT else EventStatus.IN_PROGRESS

fun eventDraftSummary(responderCount: Int): String = when (responderCount) {
    0 -> "לא שובצו כוננים — האירוע יישמר כטיוטה"
    1 -> "כונן אחד משובץ"
    else -> "$responderCount כוננים משובצים"
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
fun toggleEventResponder(selected: List<String>, responderId: String): List<String> =
    if (selected.contains(responderId)) selected.filterNot { it == responderId } else selected + responderId

fun filterAssignableProfiles(profiles: List<AssignableProfile>, query: String): List<AssignableProfile> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return profiles
    return profiles.filter { fieldsMatchQuery(it.searchFields, trimmed) }
}

const val EVENT_CANCEL_ADMIN_ONLY = "רק מנהל יכול לבטל סימון בוטל."

fun eventCancelToggleLabel(isCancelled: Boolean): String =
    if (isCancelled) "ביטול סימון “בוטל”" else "סימון האירוע כבוטל"

fun eventCancelToast(isCancelled: Boolean): String =
    if (isCancelled) "האירוע סומן כבוטל." else "סימון הביטול הוסר."

/** Clearing `is_cancelled` is admin-only, same rule as the web event form. */
fun canToggleEventCancelled(next: Boolean, viewerIsAdmin: Boolean): String? =
    if (!next && !viewerIsAdmin) EVENT_CANCEL_ADMIN_ONLY else null
