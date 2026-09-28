package com.yahpz.domain

/**
 * Manual events stay off the responder until או״ק (the number), כביש, and
 * מספר אירוע are all filled. Shift-born events stay visible — responders fill
 * those before a police id exists.
 */
fun eventReleasedToResponders(
    origin: String?,
    policeEventId: String?,
    patrolCallsignNumber: String? = null,
    hasRoad: Boolean = false,
    patrolCallsignLegacy: String? = null,
    patrolCallsignPrefix: String? = null,
): Boolean {
    if (origin == "shift") return true
    if (policeEventId.isNullOrBlank()) return false
    if (!hasRoad) return false
    val callsign = resolvePatrolCallsign(patrolCallsignPrefix, patrolCallsignNumber, patrolCallsignLegacy)
    return callsign.number.isNotBlank()
}
