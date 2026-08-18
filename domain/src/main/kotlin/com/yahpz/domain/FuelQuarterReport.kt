package com.yahpz.domain

data class FuelQuarterProfileInput(
    val id: String,
    val fullName: String,
    val callsign: String,
    val active: Boolean,
)

data class FuelQuarterSavedDistribution(
    val cards: Int,
    val cardNumbers: String = "",
)

data class FuelQuarterRow(
    val responderId: String,
    val fullName: String,
    val callsign: String,
    val active: Boolean,
    val openingBalanceKm: Double,
    val kmMonth1: Double,
    val kmMonth2: Double,
    val kmMonth3: Double,
    val quarterKm: Double,
    val payableKm: Double,
    val liters: Double,
    val suggestedCards: Int,
    val cards: Int,
    val remainingKm: Double,
    val cardNumbers: String,
) {
    val display: String get() = personDisplay(fullName, callsign)
    val searchFields: List<String?> get() = listOf(fullName, callsign)
}

fun buildFuelQuarterRows(
    year: Int,
    quarter: Int,
    profiles: List<FuelQuarterProfileInput>,
    participations: List<FuelQuarterParticipationInput>,
    openingByUser: Map<String, Double>,
    savedByUser: Map<String, FuelQuarterSavedDistribution>,
): List<FuelQuarterRow> {
    val byUser = participations.groupBy { it.responderId }
    val profileById = profiles.associateBy { it.id }
    val ids = linkedSetOf<String>()
    ids.addAll(byUser.keys)
    openingByUser.forEach { (id, opening) -> if (opening != 0.0) ids.add(id) }
    ids.addAll(savedByUser.keys)

    val rows = mutableListOf<FuelQuarterRow>()
    for (id in ids) {
        val profile = profileById[id] ?: continue
        val buckets = monthKmBuckets(year, quarter, byUser[id].orEmpty())
        val opening = openingByUser[id] ?: 0.0
        val quarterKm = buckets.kmMonth1 + buckets.kmMonth2 + buckets.kmMonth3
        val saved = savedByUser[id]
        if (opening == 0.0 && quarterKm == 0.0 && saved == null) continue

        val payable = payableKm(opening, buckets)
        val suggested = suggestedCards(payable)
        val cards = saved?.cards ?: suggested
        val cardNumbers = saved?.cardNumbers.orEmpty()
        rows += FuelQuarterRow(
            responderId = id,
            fullName = profile.fullName,
            callsign = profile.callsign,
            active = profile.active,
            openingBalanceKm = opening,
            kmMonth1 = buckets.kmMonth1,
            kmMonth2 = buckets.kmMonth2,
            kmMonth3 = buckets.kmMonth3,
            quarterKm = quarterKm,
            payableKm = payable,
            liters = litersFromPayableKm(payable),
            suggestedCards = suggested,
            cards = cards,
            remainingKm = remainingKm(payable, cards),
            cardNumbers = cardNumbers,
        )
    }
    return rows.sortedBy { it.fullName }
}

fun filterFuelQuarterRows(rows: List<FuelQuarterRow>, query: String): List<FuelQuarterRow> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return rows
    return rows.filter { fieldsMatchQuery(it.searchFields, trimmed) }
}

fun fuelQuarterLabel(quarter: Int): String = "רבעון $quarter"

fun sumRemainingKm(rows: List<FuelQuarterRow>): Double = rows.sumOf { it.remainingKm }
