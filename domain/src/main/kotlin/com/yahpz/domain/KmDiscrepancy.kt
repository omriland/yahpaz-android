package com.yahpz.domain

/**
 * אירועים עם פערי דיווח ק״מ. Mirrors the web `kmDiscrepancyReport.ts`: a completed
 * participation where the lead's `total_km` disagrees with the odometer the responder
 * entered. Admin can replace the lead figure with the odometer difference.
 */
data class KmDiscrepancyResponderInput(
    val assignmentId: String,
    val status: ParticipationStatus,
    val totalKm: Double? = null,
    val odometerStart: Double? = null,
    val odometerEnd: Double? = null,
    val name: String? = null,
    val callsign: String? = null,
)

data class KmDiscrepancyEventInput(
    val id: String,
    val eventDate: String,
    val isCancelled: Boolean = false,
    val policeEventId: String? = null,
    val location: String? = null,
    val roadName: String? = null,
    val leadName: String? = null,
    val leadCallsign: String? = null,
    val responders: List<KmDiscrepancyResponderInput> = emptyList(),
)

data class KmDiscrepancyRow(
    val assignmentId: String,
    val eventId: String,
    val eventDate: String,
    val isCancelled: Boolean,
    val policeEventId: String?,
    val location: String?,
    val roadName: String?,
    val responderName: String?,
    val responderCallsign: String?,
    val leadName: String?,
    val leadCallsign: String?,
    val leadKm: Double,
    val responderKm: Double,
) {
    val diff: Double get() = responderKm - leadKm
    val responderDisplay: String get() = personDisplay(responderName, responderCallsign, fallback = "מתנדב")
    val placeDisplay: String get() = placeDisplay(roadName, location)
}

/** Result of asking to overwrite the lead km with the responder's odometer difference. */
sealed interface LeadKmReplacement {
    data class Replace(val totalKm: Double) : LeadKmReplacement
    data object AlreadyAligned : LeadKmReplacement
    data object Invalid : LeadKmReplacement
}

fun responderOdometerKm(start: Double?, end: Double?): Double? {
    if (start == null || end == null) return null
    return end - start
}

fun resolveLeadKmReplacement(
    totalKm: Double?,
    odometerStart: Double?,
    odometerEnd: Double?,
): LeadKmReplacement {
    val next = responderOdometerKm(odometerStart, odometerEnd)
    if (totalKm == null || next == null) return LeadKmReplacement.Invalid
    if (next == totalKm) return LeadKmReplacement.AlreadyAligned
    return LeadKmReplacement.Replace(next)
}

/** Date desc, then the biggest absolute gap, then responder name. */
fun buildKmDiscrepancyRows(
    events: List<KmDiscrepancyEventInput>,
    from: String? = null,
    to: String? = null,
): List<KmDiscrepancyRow> {
    val rows = mutableListOf<KmDiscrepancyRow>()
    for (event in events) {
        if (from != null && event.eventDate < from) continue
        if (to != null && event.eventDate > to) continue
        for (responder in event.responders) {
            if (responder.status != ParticipationStatus.DONE) continue
            val leadKm = responder.totalKm ?: continue
            val responderKm = responderOdometerKm(responder.odometerStart, responder.odometerEnd)
                ?: continue
            if (responderKm == leadKm) continue
            rows += KmDiscrepancyRow(
                assignmentId = responder.assignmentId,
                eventId = event.id,
                eventDate = event.eventDate,
                isCancelled = event.isCancelled,
                policeEventId = event.policeEventId,
                location = event.location,
                roadName = event.roadName,
                responderName = responder.name,
                responderCallsign = responder.callsign,
                leadName = event.leadName,
                leadCallsign = event.leadCallsign,
                leadKm = leadKm,
                responderKm = responderKm,
            )
        }
    }
    return rows.sortedWith(
        compareByDescending<KmDiscrepancyRow> { it.eventDate }
            .thenByDescending { kotlin.math.abs(it.diff) }
            .thenBy { it.responderDisplay },
    )
}

fun kmDiscrepancyApplyTitle(responderKm: Double): String =
    "החלפה ל־${formatNumber(responderKm)} ק״מ"

fun kmDiscrepancyApplyConfirm(responderKm: Double): String =
    "הקילומטרים שהזין האחמ״ש יוחלפו ב־${formatNumber(responderKm)} ק״מ לפי מד האוץ של המתנדב."

const val KM_DISCREPANCY_APPLIED = "הקילומטרים עודכנו לפי מד האוץ."
const val KM_DISCREPANCY_ALIGNED = "הדיווחים כבר תואמים."
const val KM_DISCREPANCY_APPLY_FAILED = "עדכון הקילומטרים נכשל. נסו שוב."

fun kmDiscrepancyReportRows(rows: List<KmDiscrepancyRow>): List<ReportRow> = rows.map { row ->
    ReportRow(
        id = "${row.eventId}:${row.assignmentId}",
        eventId = row.eventId,
        title = row.responderDisplay,
        subtitle = listOfNotNull(
            formatDate(row.eventDate),
            policeEventLabel(row.policeEventId, row.isCancelled).takeIf { it != "—" },
            row.placeDisplay.takeIf { it.isNotEmpty() },
        ).joinToString(" · "),
        trailing = listOf(
            "אחמ״ש ${formatNumber(row.leadKm)}",
            "מתנדב ${formatNumber(row.responderKm)}",
            "פער ${formatNumber(row.diff)}",
        ).joinToString(" · "),
        detail = personDisplay(row.leadName, row.leadCallsign, fallback = "").takeIf { it.isNotEmpty() }
            ?.let { "אחמ״ש: $it" },
        actionId = row.assignmentId,
        actionTitle = kmDiscrepancyApplyTitle(row.responderKm),
        actionConfirm = kmDiscrepancyApplyConfirm(row.responderKm),
        searchText = listOf(
            row.responderDisplay,
            row.policeEventId.orEmpty(),
            row.placeDisplay,
        ).joinToString(" "),
    )
}
