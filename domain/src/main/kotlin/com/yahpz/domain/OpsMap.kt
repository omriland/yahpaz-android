package com.yahpz.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class AddressKind(val raw: String) {
    HOME("home"),
    WORK("work"),
    OTHER("other");

    companion object {
        fun fromRaw(raw: String?): AddressKind =
            entries.find { it.raw == raw } ?: OTHER
    }
}

val ADDRESS_KIND_LABELS = mapOf(
    AddressKind.HOME to "בית",
    AddressKind.WORK to "עבודה",
    AddressKind.OTHER to "אחר",
)

fun addressKindLabel(kind: AddressKind, customLabel: String? = null): String {
    if (kind == AddressKind.OTHER) {
        val trimmed = customLabel?.trim().orEmpty()
        return trimmed.ifEmpty { ADDRESS_KIND_LABELS.getValue(AddressKind.OTHER) }
    }
    return ADDRESS_KIND_LABELS.getValue(kind)
}

data class UnitMapPinRow(
    val userId: String,
    val fullName: String,
    val callsign: String,
    val kind: AddressKind,
    val label: String?,
    val formattedAddress: String,
    val lat: Double,
    val lng: Double,
    val volunteerStatus: VolunteerStatus,
    val availability: AvailabilityStatus,
    val availableFrom: String?,
)

data class MapPin(
    val userId: String,
    val fullName: String,
    val callsign: String,
    val kind: AddressKind,
    val name: String,
    val label: String,
    val formattedAddress: String,
    val lat: Double,
    val lng: Double,
    val volunteerStatus: VolunteerStatus,
    val availability: AvailabilityStatus,
    val availableFrom: String?,
)

fun mapPinLabel(callsign: String, kind: AddressKind, customLabel: String? = null): String =
    "${callsign} · ${addressKindLabel(kind, customLabel)}"

/** Visible map chrome: אבן דרך (או״ק) + responder name. */
fun mapResponderPinLabel(callsign: String, fullName: String): String {
    val cs = callsign.trim()
    val name = fullName.trim()
    return when {
        cs.isNotEmpty() && name.isNotEmpty() -> "$cs · $name"
        cs.isNotEmpty() -> cs
        name.isNotEmpty() -> name
        else -> "מתנדב"
    }
}

/** Show name/או״ק chips once pins are unclustered and readable. */
const val MAP_PIN_LABEL_MIN_ZOOM = 12f

fun shouldShowMapPinLabels(zoom: Float): Boolean = zoom >= MAP_PIN_LABEL_MIN_ZOOM

fun isMapVisibleVolunteerStatus(status: VolunteerStatus): Boolean =
    status != VolunteerStatus.ADMINISTRATION &&
        status != VolunteerStatus.BASIC_TRAINING &&
        status != VolunteerStatus.SHIFTS_ONLY

fun mapAvailabilityHoverLabel(
    status: AvailabilityStatus,
    availableFrom: String?,
    today: String = israelToday(),
): String? {
    if (effectiveAvailability(status, availableFrom, today) != AvailabilityStatus.UNAVAILABLE) {
        return null
    }
    return if (!availableFrom.isNullOrBlank()) {
        "לא זמין עד ${formatDate(availableFrom)}"
    } else {
        AVAILABILITY_LABELS.getValue(AvailabilityStatus.UNAVAILABLE)
    }
}

enum class MapUserPinTone { VOLUNTEER, PHONE }

data class MapUserPinChrome(
    val unavailable: Boolean,
    val tone: MapUserPinTone,
    val tooltip: String,
)

fun mapUserPinChrome(pin: MapPin, today: String = israelToday()): MapUserPinChrome {
    val tone =
        if (pin.volunteerStatus == VolunteerStatus.PHONE_TRAINING) {
            MapUserPinTone.PHONE
        } else {
            MapUserPinTone.VOLUNTEER
        }
    val hover = mapAvailabilityHoverLabel(pin.availability, pin.availableFrom, today)
    return if (hover != null) {
        MapUserPinChrome(unavailable = true, tone = tone, tooltip = hover)
    } else {
        MapUserPinChrome(
            unavailable = false,
            tone = tone,
            tooltip = VOLUNTEER_STATUS_LABELS.getValue(pin.volunteerStatus),
        )
    }
}

fun mapPinsFromUnitRows(rows: List<UnitMapPinRow>): List<MapPin> {
    return rows.mapNotNull { row ->
        if (!isMapVisibleVolunteerStatus(row.volunteerStatus)) return@mapNotNull null
        val name = addressKindLabel(row.kind, row.label)
        MapPin(
            userId = row.userId,
            fullName = row.fullName,
            callsign = row.callsign,
            kind = row.kind,
            name = name,
            label = mapPinLabel(row.callsign, row.kind, row.label),
            formattedAddress = row.formattedAddress,
            lat = row.lat,
            lng = row.lng,
            volunteerStatus = row.volunteerStatus,
            availability = row.availability,
            availableFrom = row.availableFrom,
        )
    }
}

data class NearbyResponder(
    val userId: String,
    val fullName: String,
    val callsign: String,
    val kind: AddressKind,
    val name: String,
    val formattedAddress: String,
    val lat: Double,
    val lng: Double,
    val km: Double,
)

const val SEARCH_VIEW_RADIUS_KM = 30.0
private const val KM_PER_DEG_LAT = 111.32

fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    fun toRad(deg: Double) = deg * Math.PI / 180.0
    val dLat = toRad(lat2 - lat1)
    val dLng = toRad(lng2 - lng1)
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(toRad(lat1)) * cos(toRad(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatMapDistanceKm(km: Double): String {
    if (km < 1) {
        return "${formatNumber(kotlin.math.round(km * 1000).toInt())} מ׳"
    }
    val tenths = kotlin.math.round(km * 10) / 10.0
    return "${formatNumber(tenths)} ק״מ"
}

fun mapBoundsForRadiusKm(lat: Double, lng: Double, radiusKm: Double): LatLngBbox {
    val dLat = radiusKm / KM_PER_DEG_LAT
    val cosLat = cos(lat * Math.PI / 180.0)
    val dLng = radiusKm / (KM_PER_DEG_LAT * kotlin.math.max(kotlin.math.abs(cosLat), 0.01))
    return LatLngBbox(
        south = lat - dLat,
        west = lng - dLng,
        north = lat + dLat,
        east = lng + dLng,
    )
}

fun nearbyResponders(
    pins: List<MapPin>,
    originLat: Double,
    originLng: Double,
    maxKm: Double = SEARCH_VIEW_RADIUS_KM,
): List<NearbyResponder> {
    val best = linkedMapOf<String, NearbyResponder>()
    for (pin in pins) {
        val km = haversineKm(originLat, originLng, pin.lat, pin.lng)
        if (km > maxKm) continue
        val current = best[pin.userId]
        if (current != null && current.km <= km) continue
        best[pin.userId] = NearbyResponder(
            userId = pin.userId,
            fullName = pin.fullName,
            callsign = pin.callsign,
            kind = pin.kind,
            name = pin.name,
            formattedAddress = pin.formattedAddress,
            lat = pin.lat,
            lng = pin.lng,
            km = km,
        )
    }
    return best.values.sortedWith(
        compareBy<NearbyResponder> { it.km }.thenBy { it.callsign },
    )
}

data class LatLngBbox(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
)

const val CATALOG_CLUSTER_MAX_ZOOM = 10
const val VIEWPORT_PAD = 0.25

data class CatalogCluster(
    val key: String,
    val lat: Double,
    val lng: Double,
    val count: Int,
)

fun padBbox(bbox: LatLngBbox, pad: Double = VIEWPORT_PAD): LatLngBbox {
    val latSpan = bbox.north - bbox.south
    val lngSpan = bbox.east - bbox.west
    return LatLngBbox(
        south = bbox.south - latSpan * pad,
        west = bbox.west - lngSpan * pad,
        north = bbox.north + latSpan * pad,
        east = bbox.east + lngSpan * pad,
    )
}

fun pointInBbox(lat: Double, lng: Double, bbox: LatLngBbox): Boolean =
    lat >= bbox.south && lat <= bbox.north && lng >= bbox.west && lng <= bbox.east

fun shouldClusterCatalog(zoom: Float): Boolean = zoom <= CATALOG_CLUSTER_MAX_ZOOM

fun zoomAfterCatalogClusterClick(currentZoom: Float): Float =
    maxOf(CATALOG_CLUSTER_MAX_ZOOM + 1f, currentZoom + 2f)

fun catalogCellDegrees(zoom: Float): Double =
    (360.0 / 256.0 / Math.pow(2.0, zoom.toDouble())) * 64.0

fun catalogViewForViewport(
    pins: List<MapPin>,
    bbox: LatLngBbox,
    zoom: Float,
): Pair<List<CatalogCluster>, List<MapPin>> {
    val padded = padBbox(bbox)
    val inView = pins.filter { pointInBbox(it.lat, it.lng, padded) }
    if (!shouldClusterCatalog(zoom)) {
        return emptyList<CatalogCluster>() to inView
    }
    val cell = catalogCellDegrees(zoom)
    val buckets = linkedMapOf<String, MutableList<MapPin>>()
    for (pin in inView) {
        val key = "${kotlin.math.floor(pin.lat / cell).toInt()}:${kotlin.math.floor(pin.lng / cell).toInt()}"
        buckets.getOrPut(key) { mutableListOf() }.add(pin)
    }
    val clusters = mutableListOf<CatalogCluster>()
    val points = mutableListOf<MapPin>()
    for ((key, group) in buckets) {
        if (group.size == 1) {
            points.add(group[0])
        } else {
            clusters.add(
                CatalogCluster(
                    key = key,
                    count = group.size,
                    lat = group.map { it.lat }.average(),
                    lng = group.map { it.lng }.average(),
                ),
            )
        }
    }
    return clusters to points
}

val ISRAEL_VIEW_BBOX = LatLngBbox(south = 29.4, west = 34.2, north = 33.4, east = 35.9)
const val ISRAEL_MAP_LAT = 31.5
const val ISRAEL_MAP_LNG = 34.85
const val ISRAEL_MAP_ZOOM = 8f

data class MilePost(
    val road: String,
    val km: Int,
    val lat: Double,
    val lng: Double,
)

const val MILE_POST_MIN_ZOOM = 14f
const val MILE_POST_DENSE_ZOOM = 15f
const val MILE_POST_VIEW_CAP = 400
const val MILE_POST_LAYER_LOAD_ERROR = "טעינת השכבה נכשלה. בדקו את החיבור ונסו שוב."

fun milePostTooltip(post: MilePost): String =
    "כביש ${post.road} · ק״מ ${formatNumber(post.km)}"

fun shouldShowMilePosts(layerOn: Boolean, zoom: Float, inViewCount: Int): Boolean {
    if (!layerOn || inViewCount <= 0) return false
    if (zoom < MILE_POST_MIN_ZOOM) return false
    if (inViewCount > MILE_POST_VIEW_CAP && zoom < MILE_POST_DENSE_ZOOM) return false
    return true
}

fun milePostsInView(posts: List<MilePost>, bbox: LatLngBbox): List<MilePost> {
    val padded = padBbox(bbox)
    return posts.filter { pointInBbox(it.lat, it.lng, padded) }
}

data class OpsMapLayers(
    val policeStations: Boolean = false,
    val milePosts: Boolean = true,
)

fun defaultOpsMapLayers(): OpsMapLayers = OpsMapLayers()

const val LIVE_PIN_STALE_AFTER_MS = 30_000L
const val LIVE_MAP_POLL_MS = 5_000L

data class LiveMapPin(
    val assignmentId: String,
    val lat: Double,
    val lng: Double,
    val label: String,
    val tooltip: String,
    val recordedAt: String,
)

fun isLivePinFresh(recordedAt: String, nowMs: Long): Boolean {
    val at = runCatching { java.time.Instant.parse(recordedAt).toEpochMilli() }.getOrNull()
        ?: runCatching {
            java.time.OffsetDateTime.parse(recordedAt).toInstant().toEpochMilli()
        }.getOrNull()
        ?: return false
    return nowMs - at <= LIVE_PIN_STALE_AFTER_MS
}

fun freshLivePins(pins: List<LiveMapPin>, nowMs: Long): List<LiveMapPin> =
    pins.filter { isLivePinFresh(it.recordedAt, nowMs) }

fun livePinLabel(callsign: String?, fullName: String): String {
    val who = callsign?.trim()?.takeIf { it.isNotEmpty() }
        ?: fullName.trim().ifEmpty { "מתנדב" }
    return "$who · בדרך"
}

fun liveEventLine(eventType: String?, road: String?, location: String?): String? {
    val typeName = eventType?.trim().orEmpty()
    val roadName = road?.trim().orEmpty()
    val place = location?.trim().orEmpty()
    val paren = Regex("""\((\d+)\)""").find(roadName)
    val digits = Regex("""\d+""").find(roadName)
    val roadBit = paren?.groupValues?.getOrNull(1) ?: digits?.value ?: roadName
    val placePart =
        if (place.isNotEmpty() && roadBit.isNotEmpty() &&
            (place.startsWith(roadBit) || place.contains(" $roadBit "))
        ) {
            place
        } else {
            listOf(roadBit, place).filter { it.isNotEmpty() }.joinToString(" ")
        }
    return when {
        typeName.isNotEmpty() && placePart.isNotEmpty() -> "$typeName · $placePart"
        typeName.isNotEmpty() -> typeName
        placePart.isNotEmpty() -> placePart
        else -> null
    }
}

fun livePinTooltip(eventLine: String?, recordedAtClock: String): String {
    val line = eventLine?.trim()
    return if (!line.isNullOrEmpty()) "$line · $recordedAtClock" else recordedAtClock
}

const val MAP_UNAVAILABLE_TITLE = "המפה אינה זמינה"
const val MAP_SEARCH_PLACEHOLDER = "חיפוש כתובת"
const val MAP_NEARBY_TITLE = "מתנדבים קרובים"
const val MAP_LAYERS_TITLE = "שכבות מפה"
const val MAP_LAYER_POLICE = "תחנות משטרה"
const val MAP_LAYER_MILE_POSTS = "אבני קילומטר"
const val MAP_LEGEND_ACTIVE = "פעיל / חניכה ברכב פרטי"
const val MAP_LEGEND_PHONE = "חניכה טלפונית"
const val MAP_LEGEND_UNAVAILABLE = "לא זמין"
const val MAP_TITLE = "מפה"
const val MAP_CAPTION = "חפשו כתובת כדי לראות מי המתנדבים הקרובים. כל סיכה היא כתובת אחת של משתמש פעיל."
const val MAP_PLACES_ONLY_ERROR = "יש לבחור כתובת מרשימת Google."
const val MAP_LOAD_FAILED = "טעינת המפה נכשלה."
