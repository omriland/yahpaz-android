package com.yahpz.domain

data class EventsByResponderResponderInput(
    val responderId: String,
    val totalKm: Double? = null,
    val name: String? = null,
    val callsign: String? = null,
)

data class EventsByResponderEventInput(
    val id: String,
    val eventDate: String,
    val isCancelled: Boolean = false,
    val policeEventId: String? = null,
    val location: String? = null,
    val eventTypeName: String? = null,
    val districtName: String? = null,
    val roadName: String? = null,
    val leadName: String? = null,
    val leadCallsign: String? = null,
    val responders: List<EventsByResponderResponderInput> = emptyList(),
)

data class EventsByResponderRow(
    val id: String,
    val eventId: String,
    val eventDate: String,
    val isCancelled: Boolean,
    val policeEventId: String?,
    val eventTypeName: String?,
    val districtName: String?,
    val roadName: String?,
    val location: String?,
    val leadName: String?,
    val leadCallsign: String?,
    val totalKm: Double?,
    val responderId: String,
    val responderName: String?,
    val responderCallsign: String?,
) {
    val responderDisplay: String get() = personDisplay(responderName, responderCallsign)
    val leadDisplay: String get() = personDisplay(leadCallsign, leadName, fallback = "")
    val placeDisplay: String get() = placeDisplay(roadName, location)
}

private fun responderSortKey(row: EventsByResponderRow): String =
    listOf(row.responderName.orEmpty(), row.responderCallsign.orEmpty(), row.responderId).joinToString(" ")

/** Flatten events → one row per volunteer; sort name asc, then date desc. */
fun buildEventsByResponderRows(
    events: List<EventsByResponderEventInput>,
    from: String,
    to: String,
): List<EventsByResponderRow> {
    val rows = mutableListOf<EventsByResponderRow>()
    for (event in events) {
        if (event.eventDate < from || event.eventDate > to) continue
        for (responder in event.responders) {
            rows += EventsByResponderRow(
                id = "${event.id}:${responder.responderId}",
                eventId = event.id,
                eventDate = event.eventDate,
                isCancelled = event.isCancelled,
                policeEventId = event.policeEventId,
                eventTypeName = event.eventTypeName,
                districtName = event.districtName,
                roadName = event.roadName,
                location = event.location,
                leadName = event.leadName,
                leadCallsign = event.leadCallsign,
                totalKm = responder.totalKm,
                responderId = responder.responderId,
                responderName = responder.name,
                responderCallsign = responder.callsign,
            )
        }
    }
    return rows.sortedWith(
        compareBy<EventsByResponderRow> { responderSortKey(it) }
            .thenByDescending { it.eventDate },
    )
}

fun eventsByResponderReportRows(rows: List<EventsByResponderRow>): List<ReportRow> = rows.map { row ->
    ReportRow(
        id = row.id,
        eventId = row.eventId,
        title = row.responderDisplay,
        subtitle = listOfNotNull(
            formatDate(row.eventDate),
            policeEventLabel(row.policeEventId, row.isCancelled).takeIf { it != "—" },
            row.eventTypeName?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · "),
        detail = listOfNotNull(
            row.placeDisplay.takeIf { it.isNotEmpty() },
            row.districtName?.trim()?.takeIf { it.isNotEmpty() },
            row.leadDisplay.takeIf { it.isNotEmpty() }?.let { "אחמ״ש: $it" },
        ).joinToString(" · ").takeIf { it.isNotEmpty() },
        trailing = row.totalKm?.let { "${formatNumber(it)} ק״מ" },
        searchText = listOf(
            row.responderDisplay,
            row.policeEventId.orEmpty(),
            row.placeDisplay,
            row.districtName.orEmpty(),
        ).joinToString(" "),
        stampLabel = if (row.isCancelled) "בוטל" else null,
        stampTone = StampTone.DRAFT,
    )
}
