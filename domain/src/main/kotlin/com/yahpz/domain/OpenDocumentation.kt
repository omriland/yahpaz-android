package com.yahpz.domain

const val OPEN_DOC_TITLE = "דוח תיעוד פתוח"
const val OPEN_DOC_EMPTY_TITLE = "אין דיווחים פתוחים בתקופה זו"
const val OPEN_DOC_FAILED_TITLE = "טעינת הדוח נכשלה. בדקו את החיבור ונסו שוב."
const val OPEN_DOC_RANGE_ERROR = "יש להזין תאריך התחלה וסיום תקינים"
const val OPEN_DOC_DEFAULT_RANGE_DAYS = 30

enum class OpenDocFillStatus { PENDING, IN_PROGRESS }

fun openDocFillLabel(status: OpenDocFillStatus): String =
    if (status == OpenDocFillStatus.IN_PROGRESS) "נשמרה טיוטה" else "טרם הוזן"

/** Only events still awaiting documentation appear in the report. */
private val OPEN_EVENT_STATUSES = setOf(EventStatus.IN_PROGRESS, EventStatus.PARTIAL)
private val OPEN_PARTICIPATION_STATUSES = setOf(ParticipationStatus.PENDING, ParticipationStatus.IN_PROGRESS)

data class OpenDocResponderInput(
    val responderId: String,
    val status: ParticipationStatus,
    val name: String? = null,
    val callsign: String? = null,
)

data class OpenDocEventInput(
    val id: String,
    val eventDate: String,
    val status: EventStatus,
    val isCancelled: Boolean = false,
    val policeEventId: String? = null,
    val location: String? = null,
    val roadName: String? = null,
    val shiftLeadId: String? = null,
    val leadName: String? = null,
    val leadCallsign: String? = null,
    val responders: List<OpenDocResponderInput> = emptyList(),
)

data class OpenDocRow(
    val id: String,
    val eventId: String,
    val eventDate: String,
    val policeEventId: String? = null,
    val location: String? = null,
    val roadName: String? = null,
    val responderName: String? = null,
    val responderCallsign: String? = null,
    val leadName: String? = null,
    val leadCallsign: String? = null,
    val fillStatus: OpenDocFillStatus,
) {
    val fillStatusLabel: String get() = openDocFillLabel(fillStatus)

    val responderDisplay: String
        get() = listOfNotNull(
            responderName?.trim()?.takeIf { it.isNotEmpty() },
            responderCallsign?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ").ifEmpty { "כונן" }

    val leadDisplay: String
        get() = listOfNotNull(
            leadName?.trim()?.takeIf { it.isNotEmpty() },
            leadCallsign?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ")
}

fun buildOpenDocRows(
    events: List<OpenDocEventInput>,
    from: String,
    to: String,
    viewerId: String?,
    viewerIsAdmin: Boolean,
): List<OpenDocRow> {
    val rows = mutableListOf<OpenDocRow>()
    for (event in events) {
        if (event.status !in OPEN_EVENT_STATUSES) continue
        if (event.isCancelled) continue
        if (event.eventDate < from || event.eventDate > to) continue
        if (!viewerIsAdmin && event.shiftLeadId != viewerId) continue
        for (responder in event.responders) {
            if (responder.status !in OPEN_PARTICIPATION_STATUSES) continue
            rows += OpenDocRow(
                id = "${event.id}:${responder.responderId}",
                eventId = event.id,
                eventDate = event.eventDate,
                policeEventId = event.policeEventId,
                location = event.location,
                roadName = event.roadName,
                responderName = responder.name,
                responderCallsign = responder.callsign,
                leadName = event.leadName,
                leadCallsign = event.leadCallsign,
                fillStatus = if (responder.status == ParticipationStatus.IN_PROGRESS) {
                    OpenDocFillStatus.IN_PROGRESS
                } else {
                    OpenDocFillStatus.PENDING
                },
            )
        }
    }
    return rows.sortedWith(
        compareByDescending<OpenDocRow> { it.eventDate }
            .thenBy { "${it.responderName.orEmpty()} ${it.responderCallsign.orEmpty()}" },
    )
}

fun openDocSummary(count: Int): String = when (count) {
    0 -> OPEN_DOC_EMPTY_TITLE
    1 -> "דיווח אחד ממתין לתיעוד"
    else -> "$count דיווחים ממתינים לתיעוד"
}
