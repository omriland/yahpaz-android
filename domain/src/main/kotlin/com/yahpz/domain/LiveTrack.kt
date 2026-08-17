package com.yahpz.domain

import java.net.URI

data class LatLngAt(val lat: Double, val lng: Double, val atMs: Long)

const val LIVE_PING_MIN_INTERVAL_MS = 10_000L
const val LIVE_PING_MIN_MOVE_M = 50.0

fun shouldEmitPing(last: LatLngAt?, next: LatLngAt): Boolean {
    if (last == null) return true
    if (next.atMs - last.atMs >= LIVE_PING_MIN_INTERVAL_MS) return true
    return metersBetween(last, next) >= LIVE_PING_MIN_MOVE_M
}

fun parseTrackToken(raw: String): String? {
    val uri = runCatching { URI(raw) }.getOrNull() ?: return null
    val query = uri.query ?: return null
    val items = query.split("&").mapNotNull { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size != 2) null else parts[0] to parts[1]
    }.toMap()
    val token = (items["track_token"] ?: items["token"])?.trim()
    return token?.takeIf { it.isNotEmpty() }
}

private fun metersBetween(a: LatLngAt, b: LatLngAt): Double {
    fun toRad(deg: Double) = deg * Math.PI / 180
    val earthM = 6_371_000.0
    val dLat = toRad(b.lat - a.lat)
    val dLng = toRad(b.lng - a.lng)
    val lat1 = toRad(a.lat)
    val lat2 = toRad(b.lat)
    val h = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return 2 * earthM * Math.asin(Math.min(1.0, Math.sqrt(h)))
}
