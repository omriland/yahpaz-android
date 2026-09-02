package com.yahpz.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Mirrors web `COCKPIT_WINDOW_MS` — events created in the last two hours. */
const val COCKPIT_WINDOW_MS = 2L * 60L * 60L * 1000L

const val COCKPIT_TITLE = "הקוקפיט"
const val COCKPIT_CAPTION = "אירועים פתוחים מהשעתיים האחרונות"
const val COCKPIT_SEARCH_PLACEHOLDER = "אירוע, כביש, מיקום או אחמ״ש"
const val COCKPIT_LOAD_FAILED = "טעינת המצב המבצעי נכשלה"
const val COCKPIT_LOAD_FAILED_CAPTION = "בדקו את החיבור ונסו שוב."
const val COCKPIT_EMPTY = "אין אירועים פתוחים מהשעתיים האחרונות."
const val COCKPIT_NO_RESULTS = "לא נמצאו אירועים תואמים"
const val COCKPIT_OPEN_MAPS = "ניווט למיקום"
const val COCKPIT_NO_LOCATION = "אין מיקום לפתיחה במפות"
const val COCKPIT_MAPS_FAILED = "לא נמצאה אפליקציית מפות שתוכל לפתוח את המיקום."
const val COCKPIT_NEW_EVENT_TITLE = "אירוע חדש"

private val OPEN_COCKPIT_STATUSES = setOf(EventStatus.IN_PROGRESS, EventStatus.PARTIAL)

data class CockpitLead(
    val fullName: String,
    val callsign: String,
)

data class CockpitEventInput(
    val id: String,
    val createdAt: String,
    val policeEventId: String? = null,
    val status: EventStatus,
    val isCancelled: Boolean = false,
    val location: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val eventTypeName: String? = null,
    val roadName: String? = null,
    val leadFullName: String? = null,
    val leadCallsign: String? = null,
    val responders: List<CockpitResponderInput> = emptyList(),
)

data class CockpitResponderInput(
    val id: String,
    val responderId: String? = null,
    val endedAt: String? = null,
    val status: ParticipationStatus? = null,
)

fun isInCockpitWindow(createdAt: String, now: Instant = Instant.now()): Boolean {
    val created = runCatching { Instant.parse(normalizeInstant(createdAt)) }.getOrNull() ?: return false
    val age = Duration.between(created, now).toMillis()
    return age in 0..COCKPIT_WINDOW_MS
}

/** Active ops list: recent window, not cancelled, still in progress / partial. */
fun isCockpitListCandidate(event: CockpitEventInput, now: Instant = Instant.now()): Boolean {
    if (event.isCancelled) return false
    if (event.status !in OPEN_COCKPIT_STATUSES) return false
    return isInCockpitWindow(event.createdAt, now)
}

fun filterCockpitEvents(
    events: List<CockpitEventInput>,
    now: Instant = Instant.now(),
): List<CockpitEventInput> =
    events
        .filter { isCockpitListCandidate(it, now) }
        .sortedWith(
            compareByDescending<CockpitEventInput> {
                runCatching { Instant.parse(normalizeInstant(it.createdAt)) }.getOrNull() ?: Instant.EPOCH
            }.thenByDescending { it.id },
        )

fun cockpitReelTitle(policeEventId: String?): String {
    val policeId = policeEventId?.trim().orEmpty()
    return if (policeId.isNotEmpty()) policeId else COCKPIT_NEW_EVENT_TITLE
}

fun cockpitReelPlace(roadName: String?, location: String?): String? =
    placeDisplay(roadName, location).takeIf { it.isNotEmpty() }

fun cockpitReelDetail(
    eventTypeName: String?,
    roadName: String?,
    location: String?,
): String? {
    val parts = listOf(eventTypeName, roadName, location)
        .mapNotNull { it?.trim()?.takeIf { part -> part.isNotEmpty() } }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

fun cockpitReelLead(fullName: String?, callsign: String?): CockpitLead? {
    val name = fullName?.trim().orEmpty()
    val sign = callsign?.trim().orEmpty()
    if (name.isEmpty() && sign.isEmpty()) return null
    return CockpitLead(fullName = name, callsign = sign)
}

fun cockpitLeadDisplay(lead: CockpitLead?): String =
    listOfNotNull(
        lead?.fullName?.takeIf { it.isNotEmpty() },
        lead?.callsign?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")

fun cockpitEventStillOpenOnMap(responders: List<CockpitResponderInput>): Boolean {
    if (responders.isEmpty()) return true
    return responders.any { it.endedAt.isNullOrBlank() }
}

fun roadNumberForGeocode(roadName: String): String? {
    val paren = Regex("""\((\d+)\)""").find(roadName)?.groupValues?.getOrNull(1)
    if (!paren.isNullOrEmpty()) return paren
    return Regex("""\d+""").find(roadName)?.value
}

/** Google query: road number first, then the free-text location. */
fun eventGeocodeQuery(road: String?, location: String?): String? {
    val roadName = road?.trim().orEmpty()
    val place = location?.trim().orEmpty()
    val number = if (roadName.isNotEmpty()) roadNumberForGeocode(roadName) else null
    val roadPart = when {
        !number.isNullOrEmpty() -> "כביש $number"
        else -> roadName
    }
    if (roadPart.isEmpty() && place.isEmpty()) return null
    if (roadPart.isEmpty()) return place
    if (place.isEmpty()) return roadPart
    if (place.contains(roadPart) || place.contains(roadName)) return place
    return "$roadPart $place"
}

/**
 * External maps intent URI — no Google Maps SDK / API key.
 * Coordinates → `geo:` (fallback `https://maps.google.com/?q=lat,lng` via [cockpitMapsWebUri]).
 * Otherwise search query from road + location text.
 */
fun cockpitMapsGeoUri(lat: Double?, lng: Double?): String? {
    if (lat == null || lng == null) return null
    return "geo:$lat,$lng?q=$lat,$lng"
}

fun cockpitMapsWebUri(lat: Double?, lng: Double?, roadName: String?, location: String?): String? {
    if (lat != null && lng != null) {
        return "https://maps.google.com/?q=$lat,$lng"
    }
    val query = eventGeocodeQuery(roadName, location) ?: return null
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
    return "https://maps.google.com/?q=$encoded"
}

/** Preferred open order: geo when coords exist, else web search / web lat,lng. */
fun cockpitMapsOpenUris(
    lat: Double?,
    lng: Double?,
    roadName: String?,
    location: String?,
): List<String> {
    val uris = mutableListOf<String>()
    cockpitMapsGeoUri(lat, lng)?.let { uris += it }
    cockpitMapsWebUri(lat, lng, roadName, location)?.let { uris += it }
    return uris.distinct()
}

fun formatCockpitClock(iso: String): String {
    val instant = runCatching { Instant.parse(normalizeInstant(iso)) }.getOrNull() ?: return iso
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.Builder().setLanguage("he").setRegion("IL").build())
        .withZone(ZoneId.of("Asia/Jerusalem"))
    return formatter.format(instant)
}

fun formatCockpitAge(iso: String, now: Instant = Instant.now()): String {
    val created = runCatching { Instant.parse(normalizeInstant(iso)) }.getOrNull() ?: return "עכשיו"
    val elapsed = Duration.between(created, now).toMillis()
    if (elapsed < 60_000) return "עכשיו"
    val minutes = (elapsed / 60_000).toInt()
    if (minutes == 1) return "לפני דקה"
    return "לפני $minutes דק׳"
}

fun cockpitResponderSummary(responders: List<CockpitResponderInput>): String {
    if (responders.isEmpty()) return "אין מתנדבים משובצים"
    val open = responders.count { it.endedAt.isNullOrBlank() }
    val doneFill = responders.count { it.status == ParticipationStatus.DONE }
    return when {
        open == 0 -> "${responders.size} מתנדבים · הסתיים"
        doneFill > 0 -> "${responders.size} מתנדבים · $open פעילים · $doneFill תיעוד הושלם"
        else -> "${responders.size} מתנדבים · $open פעילים"
    }
}

fun cockpitWindowCountLabel(count: Int): String = "$count בחלון"

fun filterCockpitEventsByQuery(
    events: List<CockpitEventInput>,
    query: String,
): List<CockpitEventInput> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return events
    return events.filter { event ->
        fieldsMatchQuery(
            listOf(
                event.policeEventId,
                event.roadName,
                event.location,
                event.eventTypeName,
                event.leadFullName,
                event.leadCallsign,
                formatCockpitClock(event.createdAt),
            ),
            trimmed,
        )
    }
}

fun cockpitOwnParticipation(
    responders: List<CockpitResponderInput>,
    userId: String?,
): ParticipationStatus? {
    if (userId.isNullOrBlank()) return null
    return responders.firstOrNull { it.responderId == userId }?.status
}

private fun normalizeInstant(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.contains(' ') && !trimmed.contains('T')) {
        return trimmed.replace(' ', 'T')
    }
    return trimmed
}
