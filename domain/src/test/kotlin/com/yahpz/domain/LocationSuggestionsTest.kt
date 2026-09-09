package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSuggestionsTest {
    private val junction = HighwayJunction(
        id = "junction-1",
        nameHe = "מחלף השלום",
        roads = "20",
        lat = 32.073,
        lng = 34.793,
    )
    private val google = PlacePrediction(
        placeId = "google-1",
        primaryText = "מחלף השלום",
        secondaryText = "תל אביב",
    )

    @Test
    fun `combine keeps Google results when the closed-list lookup fails`() {
        val error = IllegalStateException("local unavailable")
        val result = combineLocationSearchResults(
            junctionResult = Result.failure(error),
            placesResult = Result.success(PlacesSearchOutcome(ok = true)),
        )
        assertEquals(emptyList<HighwayJunction>(), result.junctions)
        assertTrue(result.places.ok)
        assertEquals(error, result.localError)
    }

    @Test
    fun `combine records a network failure without dropping junctions`() {
        val result = combineLocationSearchResults(
            junctionResult = Result.success(listOf(junction)),
            placesResult = Result.failure(IllegalStateException("offline")),
        )
        assertEquals(listOf(junction), result.junctions)
        assertEquals(PlacesSearchOutcome(ok = false, error = "network"), result.places)
        assertNull(result.localError)
    }

    @Test
    fun `ranks junctions above Google and free text last`() {
        assertEquals(
            listOf(
                RankedLocationSuggestion.Junction(junction),
                RankedLocationSuggestion.Google(google),
                RankedLocationSuggestion.FreeText("השלום"),
            ),
            rankLocationSuggestions(listOf(junction), listOf(google), "השלום", true),
        )
    }

    @Test
    fun `keeps free text as the fallback when neither source matches`() {
        assertEquals(
            listOf(RankedLocationSuggestion.FreeText("מקום שלא נמצא")),
            rankLocationSuggestions(emptyList(), emptyList(), " מקום שלא נמצא ", true),
        )
    }

    @Test
    fun `omits free text for places-only fields`() {
        assertEquals(
            listOf(RankedLocationSuggestion.Google(google)),
            rankLocationSuggestions(emptyList(), listOf(google), "השלום", false),
        )
    }

    @Test
    fun `google query prefixes the road number`() {
        assertEquals("כביש 4 אהרונסון", eventGeocodeQuery("כביש 4", "אהרונסון"))
        assertEquals("אהרונסון", eventGeocodeQuery(null, "אהרונסון"))
        assertNull(eventGeocodeQuery(null, "  "))
    }
}
