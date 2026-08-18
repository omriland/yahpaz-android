package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FuelQuarterMathTest {
    @Test
    fun constantsMatchMosheKeys() {
        assertEquals(6.0, KM_PER_LITER, 0.0)
        assertEquals(15.0, LITERS_PER_CARD, 0.0)
        assertEquals(90.0, KM_PER_CARD, 0.0)
    }

    @Test
    fun suggestedCardsFloorsAndNeverNegative() {
        assertEquals(0, suggestedCards(0.0))
        assertEquals(0, suggestedCards(89.0))
        assertEquals(1, suggestedCards(90.0))
        assertEquals(2, suggestedCards(180.0))
        assertEquals(0, suggestedCards(-50.0))
    }

    @Test
    fun unitFuelQuarterKpisSumAndSuggest() {
        assertEquals(UnitFuelQuarterKpis(0.0, 0, 0), unitFuelQuarterKpis(emptyList()))
        assertEquals(
            UnitFuelQuarterKpis(180.0, 2, 2),
            unitFuelQuarterKpis(listOf(90.0 to 1, 90.0 to 1)),
        )
        assertEquals(
            UnitFuelQuarterKpis(178.0, 1, 0),
            unitFuelQuarterKpis(listOf(89.0 to 0, 89.0 to 0)),
        )
        assertEquals(
            UnitFuelQuarterKpis(180.0, 2, 4),
            unitFuelQuarterKpis(listOf(180.0 to 1, 0.0 to 3)),
        )
    }

    @Test
    fun litersAndRemaining() {
        assertEquals(15.0, litersFromPayableKm(90.0), 0.0)
        assertEquals(0.0, remainingKm(90.0, 1), 0.0)
        assertEquals(10.0, remainingKm(100.0, 1), 0.0)
        assertEquals(50.0, remainingKm(50.0, 0), 0.0)
        assertEquals(-20.0, remainingKm(-20.0, 0), 0.0)
    }

    @Test
    fun quarterMonthIndexesMap() {
        assertEquals(listOf(1, 2, 3), quarterMonthIndexes(1))
        assertEquals(listOf(10, 11, 12), quarterMonthIndexes(4))
    }

    @Test
    fun monthKmBucketsByLocalCreatedAt() {
        val buckets = monthKmBuckets(
            year = 2026,
            quarter = 3,
            rows = listOf(
                FuelQuarterParticipationInput("a", "2026-07-05T12:00:00+03:00", 10.0),
                FuelQuarterParticipationInput("a", "2026-08-01T00:00:00+03:00", 20.0),
                FuelQuarterParticipationInput("a", "2026-09-15T23:00:00+03:00", 5.0),
                FuelQuarterParticipationInput("a", "2026-06-30T12:00:00+03:00", 99.0),
                FuelQuarterParticipationInput("a", "2026-07-01T12:00:00+03:00", null),
            ),
        )
        assertEquals(MonthKmBuckets(10.0, 20.0, 5.0), buckets)
    }

    @Test
    fun defaultFuelQuarterFromDate() {
        assertEquals(FuelQuarterSelection(2026, 3), defaultFuelQuarter(LocalDate.of(2026, 8, 18)))
        assertEquals(FuelQuarterSelection(2026, 1), defaultFuelQuarter(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun quarterLocalDateRangeInclusive() {
        assertEquals("2026-07-01" to "2026-09-30", quarterLocalDateRange(2026, 3))
        assertEquals("2026-01-01" to "2026-03-31", quarterLocalDateRange(2026, 1))
    }

    @Test
    fun buildFuelQuarterRowsFiltersAndDefaultsCards() {
        val profiles = listOf(
            FuelQuarterProfileInput("a", "אבי לוי", "A1", true),
            FuelQuarterProfileInput("b", "בני כהן", "B1", true),
            FuelQuarterProfileInput("c", "גיל ישן", "C1", false),
        )
        val rows = buildFuelQuarterRows(
            year = 2026,
            quarter = 1,
            profiles = profiles,
            participations = listOf(
                FuelQuarterParticipationInput("a", "2026-01-10T12:00:00+03:00", 90.0),
            ),
            openingByUser = mapOf("c" to -10.0),
            savedByUser = mapOf("b" to FuelQuarterSavedDistribution(1, "x")),
        )
        assertEquals(listOf("a", "b", "c"), rows.map { it.responderId }.sorted())
        val a = rows.first { it.responderId == "a" }
        assertEquals(90.0, a.quarterKm, 0.0)
        assertEquals(1, a.suggestedCards)
        assertEquals(1, a.cards)
        assertEquals(0.0, a.remainingKm, 0.0)
    }

    @Test
    fun buildFuelQuarterRowsUsesSavedOverride() {
        val rows = buildFuelQuarterRows(
            year = 2026,
            quarter = 1,
            profiles = listOf(FuelQuarterProfileInput("a", "אבי לוי", "A1", true)),
            participations = listOf(
                FuelQuarterParticipationInput("a", "2026-01-10T12:00:00+03:00", 90.0),
            ),
            openingByUser = emptyMap(),
            savedByUser = mapOf("a" to FuelQuarterSavedDistribution(0, "n/a")),
        )
        assertEquals(1, rows.size)
        assertEquals(0, rows[0].cards)
        assertEquals(90.0, rows[0].remainingKm, 0.0)
        assertEquals("n/a", rows[0].cardNumbers)
    }

    @Test
    fun filterFuelQuarterRowsByNameOrCallsign() {
        val rows = listOf(
            FuelQuarterRow(
                responderId = "a",
                fullName = "אבי לוי",
                callsign = "A1",
                active = true,
                openingBalanceKm = 0.0,
                kmMonth1 = 90.0,
                kmMonth2 = 0.0,
                kmMonth3 = 0.0,
                quarterKm = 90.0,
                payableKm = 90.0,
                liters = 15.0,
                suggestedCards = 1,
                cards = 1,
                remainingKm = 0.0,
                cardNumbers = "",
            ),
        )
        assertEquals(1, filterFuelQuarterRows(rows, "אבי").size)
        assertEquals(1, filterFuelQuarterRows(rows, "A1").size)
        assertTrue(filterFuelQuarterRows(rows, "zzz").isEmpty())
    }
}
