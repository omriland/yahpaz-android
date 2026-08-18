package com.yahpz.domain

const val KM_EXCEPTION_THRESHOLD = 60.0

data class KmExceptionResponderInput(
    val totalKm: Double? = null,
    val name: String? = null,
    val callsign: String? = null,
)

data class KmExceptionEventInput(
    val id: String,
    val eventDate: String,
    val isCancelled: Boolean = false,
    val policeEventId: String? = null,
    val location: String? = null,
    val eventTypeName: String? = null,
    val roadName: String? = null,
    val leadName: String? = null,
    val leadCallsign: String? = null,
    val responders: List<KmExceptionResponderInput> = emptyList(),
)

data class KmExceptionRow(
    val eventId: String,
    val eventDate: String,
    val isCancelled: Boolean,
    val policeEventId: String?,
    val location: String?,
    val eventTypeName: String?,
    val roadName: String?,
    val leadName: String?,
    val leadCallsign: String?,
    val responderName: String?,
    val responderCallsign: String?,
    val totalKm: Double,
) {
    val responderDisplay: String get() = personDisplay(responderName, responderCallsign)
    val placeDisplay: String get() = placeDisplay(roadName, location)
}

/**
 * Flatten events → exceptional responder rows; sort date desc, then km desc.
 * Only lead-entered `total_km` counts — participation status is irrelevant.
 */
fun buildKmExceptionRows(
    events: List<KmExceptionEventInput>,
    from: String? = null,
    to: String? = null,
): List<KmExceptionRow> {
    val rows = mutableListOf<KmExceptionRow>()
    for (event in events) {
        if (from != null && event.eventDate < from) continue
        if (to != null && event.eventDate > to) continue
        for (responder in event.responders) {
            val km = responder.totalKm ?: continue
            if (km < KM_EXCEPTION_THRESHOLD) continue
            rows += KmExceptionRow(
                eventId = event.id,
                eventDate = event.eventDate,
                isCancelled = event.isCancelled,
                policeEventId = event.policeEventId,
                location = event.location,
                eventTypeName = event.eventTypeName,
                roadName = event.roadName,
                leadName = event.leadName,
                leadCallsign = event.leadCallsign,
                responderName = responder.name,
                responderCallsign = responder.callsign,
                totalKm = km,
            )
        }
    }
    return rows.sortedWith(
        compareByDescending<KmExceptionRow> { it.eventDate }.thenByDescending { it.totalKm },
    )
}

fun kmExceptionReportRows(rows: List<KmExceptionRow>): List<ReportRow> = rows.mapIndexed { index, row ->
    ReportRow(
        id = "${row.eventId}:${row.responderCallsign.orEmpty()}:$index",
        eventId = row.eventId,
        title = row.responderDisplay,
        subtitle = listOfNotNull(
            formatDate(row.eventDate),
            policeEventLabel(row.policeEventId, row.isCancelled).takeIf { it != "—" },
            row.eventTypeName?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · "),
        detail = listOfNotNull(
            row.placeDisplay.takeIf { it.isNotEmpty() },
            personDisplay(row.leadName, row.leadCallsign, fallback = "").takeIf { it.isNotEmpty() }
                ?.let { "אחמ״ש: $it" },
        ).joinToString(" · ").takeIf { it.isNotEmpty() },
        trailing = "${formatNumber(row.totalKm)} ק״מ",
        searchText = listOf(
            row.responderDisplay,
            row.policeEventId.orEmpty(),
            row.placeDisplay,
        ).joinToString(" "),
    )
}
