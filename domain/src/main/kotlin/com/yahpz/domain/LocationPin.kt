package com.yahpz.domain

val LOCATION_PIN_SOURCES = listOf("places", "geocode", "shift_lead", "responder", "junction")

private val lockedSources = setOf("shift_lead", "responder", "junction")

data class LocationPinFields(
    val location: String = "",
    val locationPlaceId: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationPinSource: String? = null,
    val locationPinnedAt: String? = null,
    val locationPinnedBy: String? = null,
)

fun locationPinIsLocked(source: String?): Boolean = source != null && source in lockedSources

fun formatLocationCoords(lat: Double, lng: Double): String =
    "${"%.5f".format(java.util.Locale.US, lat)}, ${"%.5f".format(java.util.Locale.US, lng)}"

fun emptyLocationPinMeta(): LocationPinFields = LocationPinFields()

fun applyLeadMapPin(
    current: LocationPinFields,
    lat: Double,
    lng: Double,
    userId: String,
    at: String,
): LocationPinFields = current.copy(
    locationPlaceId = null,
    locationLat = lat,
    locationLng = lng,
    locationPinSource = "shift_lead",
    locationPinnedAt = at,
    locationPinnedBy = userId,
)

fun clearLockedLocationPin(current: LocationPinFields): LocationPinFields = LocationPinFields(
    location = current.location,
)

fun applyLocationFieldChange(
    current: LocationPinFields,
    nextLocation: String,
    nextPlaceId: String?,
    nextLat: Double?,
    nextLng: Double?,
): LocationPinFields {
    val pickedJunction =
        nextPlaceId?.startsWith(JUNCTION_PLACE_ID_PREFIX) == true &&
            nextLat != null &&
            nextLng != null
    if (pickedJunction) {
        return LocationPinFields(
            location = nextLocation,
            locationPlaceId = nextPlaceId,
            locationLat = nextLat,
            locationLng = nextLng,
            locationPinSource = "junction",
        )
    }
    val pickedPlace = !nextPlaceId.isNullOrEmpty() && nextLat != null && nextLng != null
    if (pickedPlace) {
        return LocationPinFields(
            location = nextLocation,
            locationPlaceId = nextPlaceId,
            locationLat = nextLat,
            locationLng = nextLng,
            locationPinSource = "places",
        )
    }
    if (locationPinIsLocked(current.locationPinSource)) {
        return current.copy(
            location = nextLocation,
            locationPlaceId = null,
        )
    }
    return LocationPinFields(location = nextLocation)
}

/** Persist location text plus the canonical map pin. Matches web `buildLocationPayload`. */
fun buildLocationPayload(draft: LocationPinFields): LocationPinFields {
    val location = draft.location.trim().ifEmpty { null }
    val locked = locationPinIsLocked(draft.locationPinSource)
    val hasCoords = draft.locationLat != null && draft.locationLng != null

    if (location == null && !locked) {
        return LocationPinFields()
    }

    if (locked && hasCoords) {
        return LocationPinFields(
            location = location.orEmpty(),
            locationPlaceId = null,
            locationLat = draft.locationLat,
            locationLng = draft.locationLng,
            locationPinSource = draft.locationPinSource,
            locationPinnedAt = draft.locationPinnedAt,
            locationPinnedBy = draft.locationPinnedBy,
        )
    }

    val hasPlace = !draft.locationPlaceId.isNullOrEmpty() && hasCoords
    if (hasPlace) {
        return LocationPinFields(
            location = location.orEmpty(),
            locationPlaceId = draft.locationPlaceId,
            locationLat = draft.locationLat,
            locationLng = draft.locationLng,
            locationPinSource = "places",
        )
    }

    if (draft.locationPinSource == "geocode" && hasCoords) {
        return LocationPinFields(
            location = location.orEmpty(),
            locationPlaceId = null,
            locationLat = draft.locationLat,
            locationLng = draft.locationLng,
            locationPinSource = "geocode",
        )
    }

    return LocationPinFields(location = location.orEmpty())
}

fun LocationPinFields.locationOrNull(): String? = location.trim().ifEmpty { null }
