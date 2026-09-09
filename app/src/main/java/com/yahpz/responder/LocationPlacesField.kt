package com.yahpz.responder

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.yahpz.domain.CombinedSearchResult
import com.yahpz.domain.HighwayJunction
import com.yahpz.domain.HighwayJunctionCatalogRow
import com.yahpz.domain.LOCATION_GOOGLE_GROUP
import com.yahpz.domain.LOCATION_JUNCTIONS_UNAVAILABLE
import com.yahpz.domain.LOCATION_JUNCTION_GROUP
import com.yahpz.domain.LOCATION_PLACEHOLDER
import com.yahpz.domain.LOCATION_SEARCHING
import com.yahpz.domain.LocationPinFields
import com.yahpz.domain.PlacePrediction
import com.yahpz.domain.PlacesSearchOutcome
import com.yahpz.domain.RankedLocationSuggestion
import com.yahpz.domain.applyLocationFieldChange
import com.yahpz.domain.combineLocationSearchResults
import com.yahpz.domain.eventGeocodeQuery
import com.yahpz.domain.junctionLocationLabel
import com.yahpz.domain.junctionPlaceId
import com.yahpz.domain.junctionRoadsCaption
import com.yahpz.domain.locationFreeTextLabel
import com.yahpz.domain.rankLocationSuggestions
import com.yahpz.domain.searchHighwayJunctionsCached
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val fieldShape = RoundedCornerShape(4.dp)

@Composable
fun LocationPlacesField(
    value: LocationPinFields,
    onChange: (LocationPinFields) -> Unit,
    onJunctionCommit: (HighwayJunction) -> Unit,
    roadName: String?,
    error: String?,
    mapsApiKey: String,
    onGoogleUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "מיקום",
    placeholder: String = LOCATION_PLACEHOLDER,
    allowFreeText: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf(value.location) }
    var open by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf<List<HighwayJunctionCatalogRow>>(emptyList()) }
    var catalogError by remember { mutableStateOf<Throwable?>(null) }
    var searching by remember { mutableStateOf(false) }
    var localSearchFailed by remember { mutableStateOf(false) }
    var junctions by remember { mutableStateOf<List<HighwayJunction>>(emptyList()) }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    var warnedGoogle by remember { mutableStateOf(false) }

    LaunchedEffect(value.location) {
        if (value.location != query) query = value.location
    }

    LaunchedEffect(Unit) {
        runCatching { YahpazAPI.fetchHighwayJunctionCatalog() }
            .onSuccess {
                catalog = it
                catalogError = null
            }
            .onFailure { catalogError = it }
    }

    fun notifyGoogleUnavailable() {
        if (warnedGoogle) return
        warnedGoogle = true
        onGoogleUnavailable()
    }

    LaunchedEffect(query, open, roadName, catalog, catalogError) {
        val trimmed = query.trim()
        if (!open || trimmed.isEmpty()) {
            junctions = emptyList()
            predictions = emptyList()
            searching = false
            localSearchFailed = false
            return@LaunchedEffect
        }
        val googleQuery = eventGeocodeQuery(roadName, trimmed) ?: return@LaunchedEffect
        searching = true
        delay(250)
        val result = searchLocationSuggestions(
            context = context,
            mapsApiKey = mapsApiKey,
            catalog = catalog,
            catalogError = catalogError,
            localQuery = trimmed,
            googleQuery = googleQuery,
            sessionToken = sessionToken,
        )
        searching = false
        localSearchFailed = result.localError != null
        junctions = result.junctions
        if (!result.places.ok) {
            notifyGoogleUnavailable()
            predictions = emptyList()
        } else {
            predictions = result.places.predictions
        }
    }

    val ranked = rankLocationSuggestions(junctions, predictions, query, allowFreeText)

    fun applyNext(next: LocationPinFields) {
        onChange(next)
        query = next.location
    }

    fun commitFreeText(text: String) {
        val trimmed = text.trim()
        sessionToken = AutocompleteSessionToken.newInstance()
        applyNext(
            applyLocationFieldChange(
                current = value,
                nextLocation = trimmed,
                nextPlaceId = null,
                nextLat = null,
                nextLng = null,
            ),
        )
        open = false
    }

    fun commitJunction(junction: HighwayJunction) {
        val location = junctionLocationLabel(junction.nameHe, query)
        applyNext(
            applyLocationFieldChange(
                current = value,
                nextLocation = location,
                nextPlaceId = junctionPlaceId(junction.id),
                nextLat = junction.lat,
                nextLng = junction.lng,
            ),
        )
        onJunctionCommit(junction)
        sessionToken = AutocompleteSessionToken.newInstance()
        open = false
    }

    fun commitGoogle(prediction: PlacePrediction) {
        scope.launch {
            val details = fetchPlaceDetails(context, mapsApiKey, prediction.placeId, sessionToken)
            sessionToken = AutocompleteSessionToken.newInstance()
            if (details == null) {
                if (allowFreeText) {
                    commitFreeText(
                        listOf(prediction.primaryText, prediction.secondaryText)
                            .filter { it.isNotBlank() }
                            .joinToString(", "),
                    )
                }
                notifyGoogleUnavailable()
                return@launch
            }
            applyNext(
                applyLocationFieldChange(
                    current = value,
                    nextLocation = details.label,
                    nextPlaceId = details.placeId,
                    nextLat = details.lat,
                    nextLng = details.lng,
                ),
            )
            open = false
        }
    }

    fun select(option: RankedLocationSuggestion) {
        when (option) {
            is RankedLocationSuggestion.Junction -> commitJunction(option.junction)
            is RankedLocationSuggestion.Google -> commitGoogle(option.prediction)
            is RankedLocationSuggestion.FreeText -> commitFreeText(option.text)
        }
        focusManager.clearFocus()
        keyboard?.hide()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        TextField(
            value = query,
            onValueChange = { next ->
                query = next
                open = true
                onChange(
                    applyLocationFieldChange(
                        current = value,
                        nextLocation = next,
                        nextPlaceId = null,
                        nextLat = null,
                        nextLng = null,
                    ),
                )
            },
            singleLine = true,
            textStyle = TypeScale.body,
            placeholder = {
                Text(placeholder, style = TypeScale.body, color = FieldTheme.textMuted)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (allowFreeText) commitFreeText(query)
                    focusManager.clearFocus()
                    keyboard?.hide()
                },
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldTheme.raised,
                unfocusedContainerColor = FieldTheme.raised,
                disabledContainerColor = FieldTheme.raised,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = FieldTheme.textPrimary,
                unfocusedTextColor = FieldTheme.textPrimary,
                disabledTextColor = FieldTheme.textMuted,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(FormControlHeight)
                .border(1.dp, if (error == null) FieldTheme.strong else FieldTheme.alert, fieldShape)
                .onFocusChanged { if (it.isFocused) open = true },
        )
        if (error != null) {
            Text(error, style = TypeScale.caption, color = FieldTheme.alert)
        }
        if (open && query.trim().isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.raised, fieldShape)
                    .border(1.dp, FieldTheme.hairline, fieldShape),
            ) {
                if (searching) {
                    SuggestionStatus(LOCATION_SEARCHING)
                }
                if (localSearchFailed) {
                    SuggestionStatus(LOCATION_JUNCTIONS_UNAVAILABLE)
                }
                ranked.forEachIndexed { index, option ->
                    val previous = ranked.getOrNull(index - 1)
                    val groupLabel = when {
                        option is RankedLocationSuggestion.Junction &&
                            previous !is RankedLocationSuggestion.Junction -> LOCATION_JUNCTION_GROUP
                        option is RankedLocationSuggestion.Google &&
                            previous !is RankedLocationSuggestion.Google -> LOCATION_GOOGLE_GROUP
                        else -> null
                    }
                    if (groupLabel != null) {
                        Text(
                            groupLabel,
                            style = TypeScale.caption,
                            color = FieldTheme.textMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    when (option) {
                        is RankedLocationSuggestion.Junction -> SuggestionRow(
                            primary = option.junction.nameHe,
                            secondary = junctionRoadsCaption(option.junction.roads),
                            onClick = { select(option) },
                        )
                        is RankedLocationSuggestion.Google -> SuggestionRow(
                            primary = option.prediction.primaryText,
                            secondary = option.prediction.secondaryText.takeIf { it.isNotBlank() },
                            onClick = { select(option) },
                        )
                        is RankedLocationSuggestion.FreeText -> SuggestionRow(
                            primary = locationFreeTextLabel(option.text),
                            secondary = null,
                            onClick = { select(option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionStatus(text: String) {
    Text(
        text,
        style = TypeScale.caption,
        color = FieldTheme.textSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SuggestionRow(
    primary: String,
    secondary: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(primary, style = TypeScale.body, color = FieldTheme.textPrimary)
        if (secondary != null) {
            Text(secondary, style = TypeScale.caption, color = FieldTheme.textMuted)
        }
    }
}

private data class ResolvedPlace(
    val placeId: String,
    val label: String,
    val lat: Double,
    val lng: Double,
)

private suspend fun searchLocationSuggestions(
    context: Context,
    mapsApiKey: String,
    catalog: List<HighwayJunctionCatalogRow>,
    catalogError: Throwable?,
    localQuery: String,
    googleQuery: String,
    sessionToken: AutocompleteSessionToken,
): CombinedSearchResult = coroutineScope {
    val junctions = async {
        if (catalogError != null && catalog.isEmpty()) {
            Result.failure(catalogError)
        } else {
            Result.success(searchHighwayJunctionsCached(catalog, localQuery))
        }
    }
    val places = async {
        searchGooglePlaces(context, mapsApiKey, googleQuery, sessionToken)
    }
    combineLocationSearchResults(junctions.await(), places.await())
}

private suspend fun searchGooglePlaces(
    context: Context,
    mapsApiKey: String,
    query: String,
    sessionToken: AutocompleteSessionToken,
): Result<PlacesSearchOutcome> {
    if (mapsApiKey.isBlank()) {
        return Result.success(PlacesSearchOutcome(ok = false, error = "missing_key"))
    }
    return withContext(Dispatchers.IO) {
        runCatching {
            ensurePlacesInitialized(context, mapsApiKey)
            val client = Places.createClient(context)
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries(listOf("IL"))
                .setSessionToken(sessionToken)
                .build()
            val predictions = client.findAutocompletePredictions(request).await()
                .autocompletePredictions
                .map {
                    PlacePrediction(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString(),
                    )
                }
            PlacesSearchOutcome(ok = true, predictions = predictions)
        }
    }
}

private suspend fun fetchPlaceDetails(
    context: Context,
    mapsApiKey: String,
    placeId: String,
    sessionToken: AutocompleteSessionToken,
): ResolvedPlace? {
    if (mapsApiKey.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            ensurePlacesInitialized(context, mapsApiKey)
            val client = Places.createClient(context)
            val request = FetchPlaceRequest.builder(
                placeId,
                listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.NAME),
            )
                .setSessionToken(sessionToken)
                .build()
            val place = client.fetchPlace(request).await().place
            val latLng = place.latLng ?: return@runCatching null
            val label = place.name?.takeIf { it.isNotBlank() }
                ?: place.address?.takeIf { it.isNotBlank() }
                ?: placeId
            ResolvedPlace(
                placeId = place.id ?: placeId,
                label = label,
                lat = latLng.latitude,
                lng = latLng.longitude,
            )
        }.getOrNull()
    }
}

private fun ensurePlacesInitialized(context: Context, apiKey: String) {
    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, apiKey)
    }
}
