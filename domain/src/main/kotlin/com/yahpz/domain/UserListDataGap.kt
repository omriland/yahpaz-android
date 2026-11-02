package com.yahpz.domain

/*
 * Scan mark on the users list for מתנדב פעיל and חניכה ברכב פרטי (matches the web).
 * A vehicle counts only when it is not archived. A map address counts only with a
 * Google place_id and finite lat/lng, the pin the unit map draws.
 */

const val USER_GAP_NO_VEHICLE = "אין רכב"
const val USER_GAP_NO_MAP_ADDRESS = "אין כתובת על המפה"
const val USER_GAP_BOTH = "אין רכב ואין כתובת על המפה"

private val GAP_STATUSES = setOf(
    VolunteerStatus.ACTIVE_VOLUNTEER.raw,
    VolunteerStatus.PERSONAL_VEHICLE_TRAINING.raw,
)

data class UserListDataGap(val missingVehicle: Boolean, val missingMapAddress: Boolean, val tip: String)

fun isRecognizedMapAddress(placeId: String?, lat: Double?, lng: Double?): Boolean =
    !placeId.isNullOrBlank() && lat != null && lat.isFinite() && lng != null && lng.isFinite()

fun userListDataGap(volunteerStatus: String?, hasActiveVehicle: Boolean, hasMapAddress: Boolean): UserListDataGap? {
    if (volunteerStatus !in GAP_STATUSES) return null
    val missingVehicle = !hasActiveVehicle
    val missingMapAddress = !hasMapAddress
    val tip = when {
        missingVehicle && missingMapAddress -> USER_GAP_BOTH
        missingVehicle -> USER_GAP_NO_VEHICLE
        missingMapAddress -> USER_GAP_NO_MAP_ADDRESS
        else -> return null
    }
    return UserListDataGap(missingVehicle, missingMapAddress, tip)
}
