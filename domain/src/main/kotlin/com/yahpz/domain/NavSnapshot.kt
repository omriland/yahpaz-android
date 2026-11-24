package com.yahpz.domain

const val NAV_TAB_KEY = "nav_tab"
const val NAV_FILL_EVENT_ID_KEY = "nav_fill_event_id"
const val NAV_FILL_SHIFT_BORN_KEY = "nav_fill_shift_born"
const val NAV_LIVE_SHIFT_ID_KEY = "nav_live_shift_id"
const val NAV_TOOLS_DESTINATION_KEY = "nav_tools_destination"
const val NAV_UNIT_EVENTS_QUERY_KEY = "nav_unit_events_query"

private const val NAV_FALLBACK_TAB = "INBOX"
private const val NAV_FALLBACK_TOOLS_DESTINATION = "HUB"

/** Where the user is in the app, as plain strings that fit a saved-state bundle. */
data class NavSnapshot(
    val tab: String,
    val fillEventId: String?,
    val fillShiftBorn: Boolean,
    val liveShiftScreenId: String?,
    val toolsDestination: String,
    val unitEventsQuery: String,
)

fun NavSnapshot.toBundleMap(): Map<String, String> = buildMap {
    put(NAV_TAB_KEY, tab)
    put(NAV_TOOLS_DESTINATION_KEY, toolsDestination)
    put(NAV_UNIT_EVENTS_QUERY_KEY, unitEventsQuery)
    if (fillEventId != null) {
        put(NAV_FILL_EVENT_ID_KEY, fillEventId)
        put(NAV_FILL_SHIFT_BORN_KEY, fillShiftBorn.toString())
    }
    if (liveShiftScreenId != null) put(NAV_LIVE_SHIFT_ID_KEY, liveShiftScreenId)
}

/**
 * Reads a saved map back. A fill needs both its id and its kind, or it is dropped.
 * The live shift is kept only while it is still the user's current shift.
 * Returns null when nothing usable was saved.
 */
fun restoreNavSnapshot(
    map: Map<String, String>,
    validTabs: Set<String>,
    validToolsDestinations: Set<String>,
    currentShiftId: String?,
): NavSnapshot? {
    val savedTab = map[NAV_TAB_KEY] ?: return null
    val fillEventId = map[NAV_FILL_EVENT_ID_KEY]
    val fillKind = map[NAV_FILL_SHIFT_BORN_KEY]
    val hasWholeFill = fillEventId != null && fillKind != null
    val savedLiveShiftId = map[NAV_LIVE_SHIFT_ID_KEY]
    val savedDestination = map[NAV_TOOLS_DESTINATION_KEY]
    return NavSnapshot(
        tab = if (savedTab in validTabs) savedTab else NAV_FALLBACK_TAB,
        fillEventId = if (hasWholeFill) fillEventId else null,
        fillShiftBorn = hasWholeFill && fillKind == true.toString(),
        liveShiftScreenId = savedLiveShiftId?.takeIf { it == currentShiftId },
        toolsDestination = if (savedDestination in validToolsDestinations) {
            savedDestination.orEmpty()
        } else {
            NAV_FALLBACK_TOOLS_DESTINATION
        },
        unitEventsQuery = map[NAV_UNIT_EVENTS_QUERY_KEY].orEmpty(),
    )
}
