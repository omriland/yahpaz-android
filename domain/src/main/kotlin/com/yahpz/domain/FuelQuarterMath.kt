package com.yahpz.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Moshe fuel-card keys — fixed in v1. */
const val KM_PER_LITER = 6.0
const val LITERS_PER_CARD = 15.0
val KM_PER_CARD: Double = LITERS_PER_CARD * KM_PER_LITER

fun litersFromPayableKm(payableKm: Double): Double = payableKm / KM_PER_LITER

/** floor(liters/15); never suggest negative cards. */
fun suggestedCards(payableKm: Double): Int {
    val liters = litersFromPayableKm(payableKm)
    if (liters <= 0) return 0
    return kotlin.math.floor(liters / LITERS_PER_CARD).toInt()
}

fun remainingKm(payableKm: Double, cards: Int): Double = payableKm - cards * KM_PER_CARD

data class UnitFuelQuarterKpis(
    val totalKm: Double,
    val suggestedCards: Int,
    val issuedCards: Int,
)

/** Unit KPIs for a quarter: driven km + suggested cards from that same total. */
fun unitFuelQuarterKpis(rows: List<Pair<Double, Int>>): UnitFuelQuarterKpis {
    val totalKm = rows.sumOf { it.first }
    val issuedCards = rows.sumOf { it.second }
    return UnitFuelQuarterKpis(
        totalKm = totalKm,
        suggestedCards = suggestedCards(totalKm),
        issuedCards = issuedCards,
    )
}

fun unitFuelQuarterKpisFromRows(rows: List<FuelQuarterRow>): UnitFuelQuarterKpis =
    unitFuelQuarterKpis(rows.map { it.quarterKm to it.cards })

/** 1-based calendar months in the quarter. */
fun quarterMonthIndexes(quarter: Int): List<Int> {
    require(quarter in 1..4)
    val start = (quarter - 1) * 3 + 1
    return listOf(start, start + 1, start + 2)
}

data class MonthKmBuckets(
    val kmMonth1: Double,
    val kmMonth2: Double,
    val kmMonth3: Double,
)

data class FuelQuarterParticipationInput(
    val responderId: String,
    val createdAt: String,
    val totalKm: Double?,
)

/** Sum lead `total_km` into the three months of `year`/`quarter` by local `created_at`. */
fun monthKmBuckets(
    year: Int,
    quarter: Int,
    rows: List<FuelQuarterParticipationInput>,
    zone: ZoneId = ZoneId.of("Asia/Jerusalem"),
): MonthKmBuckets {
    val months = quarterMonthIndexes(quarter)
    val sums = doubleArrayOf(0.0, 0.0, 0.0)
    for (row in rows) {
        val km = row.totalKm ?: continue
        val local = parseLocalCreatedAt(row.createdAt, zone) ?: continue
        if (local.year != year) continue
        val month = local.monthValue
        val slot = months.indexOf(month)
        if (slot == -1) continue
        sums[slot] += km
    }
    return MonthKmBuckets(sums[0], sums[1], sums[2])
}

private fun parseLocalCreatedAt(raw: String, zone: ZoneId): ZonedDateTime? {
    runCatching { Instant.parse(raw) }.getOrNull()?.let { return it.atZone(zone) }
    runCatching { java.time.OffsetDateTime.parse(raw) }.getOrNull()?.let {
        return it.atZoneSameInstant(zone)
    }
    return runCatching { ZonedDateTime.parse(raw) }.getOrNull()?.withZoneSameInstant(zone)
}

fun payableKm(opening: Double, buckets: MonthKmBuckets): Double =
    opening + buckets.kmMonth1 + buckets.kmMonth2 + buckets.kmMonth3

private val HE_MONTHS = listOf(
    "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
    "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר",
)

fun quarterMonthLabels(quarter: Int): List<String> =
    quarterMonthIndexes(quarter).map { HE_MONTHS[it - 1] }

data class FuelQuarterSelection(val year: Int, val quarter: Int)

fun defaultFuelQuarter(now: LocalDate = israelTodayDate()): FuelQuarterSelection {
    val quarter = (now.monthValue - 1) / 3 + 1
    return FuelQuarterSelection(year = now.year, quarter = quarter)
}

/** Inclusive local YYYY-MM-DD bounds for a calendar quarter. */
fun quarterLocalDateRange(year: Int, quarter: Int): Pair<String, String> {
    val months = quarterMonthIndexes(quarter)
    val m1 = months[0]
    val m3 = months[2]
    val from = "%04d-%02d-01".format(year, m1)
    val lastDay = LocalDate.of(year, m3, 1).lengthOfMonth()
    val to = "%04d-%02d-%02d".format(year, m3, lastDay)
    return from to to
}

fun israelTodayDate(): LocalDate = LocalDate.now(ZoneId.of("Asia/Jerusalem"))

const val FUEL_QUARTER_TITLE = "ניהול דלק"
const val FUEL_QUARTER_CAPTION = "ק״מ, כרטיסים ויתרות לפי רבעון — נספרים רק אירועים שתועדו במלואם"
const val FUEL_QUARTER_LOAD_FAILED = "לא הצלחנו לטעון את כרטיסי הדלק."
const val FUEL_QUARTER_EMPTY = "אין מתנדבים עם ק״מ או יתרה ברבעון זה."
const val FUEL_QUARTER_SEARCH_EMPTY = "לא נמצאו מתנדבים תואמים"
const val FUEL_QUARTER_SEARCH_PLACEHOLDER = "חיפוש לפי שם או או״ק"
