package com.yahpz.domain

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String = "",
)

data class PlacesSearchOutcome(
    val ok: Boolean,
    val predictions: List<PlacePrediction> = emptyList(),
    val error: String? = null,
)

data class CombinedSearchResult(
    val junctions: List<HighwayJunction>,
    val places: PlacesSearchOutcome,
    val localError: Throwable? = null,
)

sealed class RankedLocationSuggestion {
    data class Junction(val junction: HighwayJunction) : RankedLocationSuggestion()
    data class Google(val prediction: PlacePrediction) : RankedLocationSuggestion()
    data class FreeText(val text: String) : RankedLocationSuggestion()
}

/** Merge settled search results: junctions stay empty if the catalog failed. */
fun combineLocationSearchResults(
    junctionResult: Result<List<HighwayJunction>>,
    placesResult: Result<PlacesSearchOutcome>,
): CombinedSearchResult = CombinedSearchResult(
    junctions = junctionResult.getOrDefault(emptyList()),
    places = placesResult.getOrElse { PlacesSearchOutcome(ok = false, error = "network") },
    localError = junctionResult.exceptionOrNull(),
)

/** Junctions first, Google second, and free text last when allowed. */
fun rankLocationSuggestions(
    junctions: List<HighwayJunction>,
    predictions: List<PlacePrediction>,
    freeText: String,
    allowFreeText: Boolean,
): List<RankedLocationSuggestion> = buildList {
    addAll(junctions.map { RankedLocationSuggestion.Junction(it) })
    addAll(predictions.map { RankedLocationSuggestion.Google(it) })
    val trimmed = freeText.trim()
    if (allowFreeText && trimmed.isNotEmpty()) {
        add(RankedLocationSuggestion.FreeText(trimmed))
    }
}

