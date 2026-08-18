package com.yahpz.domain

data class FuelRefundProfileInput(
    val id: String,
    val fullName: String,
    val callsign: String,
)

/** Participation km for refunds — only lead-entered `total_km` counts. */
data class FuelRefundParticipationInput(
    val responderId: String,
    val eventId: String,
    val totalKm: Double? = null,
)

/** Extra km that is not an event participation (private-vehicle shift). */
data class FuelRefundCreditInput(
    val responderId: String,
    val totalKm: Double,
)

data class FuelRefundRow(
    val id: String,
    val fullName: String,
    val callsign: String,
    val totalKm: Double,
    val eventCount: Int,
)

fun buildFuelRefundRows(
    profiles: List<FuelRefundProfileInput>,
    participations: List<FuelRefundParticipationInput>,
    credits: List<FuelRefundCreditInput> = emptyList(),
): List<FuelRefundRow> {
    val withKm = participations.filter { it.totalKm != null }
    val byUser = withKm.groupBy { it.responderId }
    val extraByUser = credits.groupBy { it.responderId }
        .mapValues { entry -> entry.value.sumOf { it.totalKm } }

    return profiles.map { profile ->
        val parts = byUser[profile.id].orEmpty()
        FuelRefundRow(
            id = profile.id,
            fullName = profile.fullName,
            callsign = profile.callsign,
            totalKm = parts.sumOf { it.totalKm ?: 0.0 } + (extraByUser[profile.id] ?: 0.0),
            eventCount = parts.size,
        )
    }.sortedBy { it.fullName }
}

fun fuelRefundReportRows(rows: List<FuelRefundRow>): List<ReportRow> = rows.map { row ->
    ReportRow(
        id = row.id,
        title = personDisplay(row.fullName, row.callsign),
        subtitle = if (row.eventCount == 1) "אירוע אחד" else "${row.eventCount} אירועים",
        trailing = "${formatNumber(row.totalKm)} ק״מ",
        searchText = "${row.fullName} ${row.callsign}",
    )
}
