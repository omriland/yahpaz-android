package com.yahpz.responder

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.yahpz.domain.ISRAEL_MAP_LAT
import com.yahpz.domain.ISRAEL_MAP_LNG
import com.yahpz.domain.ISRAEL_MAP_ZOOM
import com.yahpz.domain.ISRAEL_VIEW_BBOX
import com.yahpz.domain.LIVE_MAP_POLL_MS
import com.yahpz.domain.LiveMapPin
import com.yahpz.domain.MAP_CAPTION
import com.yahpz.domain.MAP_LAYER_MILE_POSTS
import com.yahpz.domain.MAP_LAYER_POLICE
import com.yahpz.domain.MAP_LAYERS_TITLE
import com.yahpz.domain.MAP_LEGEND_ACTIVE
import com.yahpz.domain.MAP_LEGEND_PHONE
import com.yahpz.domain.MAP_LEGEND_UNAVAILABLE
import com.yahpz.domain.MAP_LOAD_FAILED
import com.yahpz.domain.MAP_NEARBY_TITLE
import com.yahpz.domain.MAP_PLACES_ONLY_ERROR
import com.yahpz.domain.MAP_SEARCH_PLACEHOLDER
import com.yahpz.domain.MAP_TITLE
import com.yahpz.domain.MAP_UNAVAILABLE_TITLE
import com.yahpz.domain.MapPin
import com.yahpz.domain.MapUserPinTone
import com.yahpz.domain.MilePost
import com.yahpz.domain.NearbyResponder
import com.yahpz.domain.OpsMapLayers
import com.yahpz.domain.SEARCH_VIEW_RADIUS_KM
import com.yahpz.domain.catalogViewForViewport
import com.yahpz.domain.defaultOpsMapLayers
import com.yahpz.domain.formatMapDistanceKm
import com.yahpz.domain.freshLivePins
import com.yahpz.domain.managesUnit
import com.yahpz.domain.mapBoundsForRadiusKm
import com.yahpz.domain.mapUserPinChrome
import com.yahpz.domain.milePostTooltip
import com.yahpz.domain.milePostsInView
import com.yahpz.domain.nearbyResponders
import com.yahpz.domain.padBbox
import com.yahpz.domain.pointInBbox
import com.yahpz.domain.shouldShowMilePosts
import com.yahpz.domain.zoomAfterCatalogClusterClick
import com.yahpz.domain.LatLngBbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Composable
fun MapScreen(app: AppModel, ui: AppUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapsKey = BuildConfig.MAPS_API_KEY
    val manages = managesUnit(ui.roles)

    var pins by remember { mutableStateOf<List<MapPin>>(emptyList()) }
    var livePins by remember { mutableStateOf<List<LiveMapPin>>(emptyList()) }
    var milePosts by remember { mutableStateOf<List<MilePost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var layers by remember { mutableStateOf(defaultOpsMapLayers()) }
    var layersOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePredictionUi>>(emptyList()) }
    var searchOrigin by remember { mutableStateOf<LatLng?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyResponder>>(emptyList()) }
    var placesError by remember { mutableStateOf<String?>(null) }
    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    var zoom by remember { mutableFloatStateOf(ISRAEL_MAP_ZOOM) }
    var bbox by remember { mutableStateOf(ISRAEL_VIEW_BBOX) }
    var focusedPinKey by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(ISRAEL_MAP_LAT, ISRAEL_MAP_LNG),
            ISRAEL_MAP_ZOOM,
        )
    }

    LaunchedEffect(Unit) {
        if (mapsKey.isBlank()) {
            loading = false
            return@LaunchedEffect
        }
        ensurePlacesInitialized(context, mapsKey)
        loading = true
        loadFailed = false
        runCatching {
            pins = YahpazAPI.fetchUnitMapPins()
            milePosts = loadMilePostsAsset(context)
        }.onFailure {
            loadFailed = true
        }
        loading = false
    }

    LaunchedEffect(manages, ui.userId) {
        if (!manages || mapsKey.isBlank()) {
            livePins = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            runCatching {
                livePins = YahpazAPI.fetchLiveMapPins()
            }
            delay(LIVE_MAP_POLL_MS)
        }
    }

    LaunchedEffect(searchQuery) {
        placesError = null
        if (searchQuery.isBlank()) {
            predictions = emptyList()
            searchOrigin = null
            nearby = emptyList()
            return@LaunchedEffect
        }
        delay(280)
        if (mapsKey.isBlank()) return@LaunchedEffect
        ensurePlacesInitialized(context, mapsKey)
        val client = Places.createClient(context)
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(searchQuery)
            .setCountries(listOf("IL"))
            .setSessionToken(sessionToken)
            .build()
        runCatching {
            val response = client.findAutocompletePredictions(request).await()
            predictions = response.autocompletePredictions.map {
                PlacePredictionUi(it.placeId, it.getPrimaryText(null).toString(), it.getFullText(null).toString())
            }
        }.onFailure {
            predictions = emptyList()
        }
    }

    if (mapsKey.isBlank()) {
        EmptyState(title = MAP_UNAVAILABLE_TITLE)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(MAP_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(MAP_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)
            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = MAP_SEARCH_PLACEHOLDER,
            )
            placesError?.let {
                Text(it, style = TypeScale.caption, color = FieldTheme.alert)
            }
            if (predictions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FieldTheme.raised,
                    tonalElevation = 2.dp,
                ) {
                    Column {
                        predictions.take(6).forEach { prediction ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val place = fetchPlaceLatLng(context, prediction.placeId)
                                            if (place == null) {
                                                placesError = MAP_PLACES_ONLY_ERROR
                                                return@launch
                                            }
                                            searchQuery = prediction.primary
                                            predictions = emptyList()
                                            searchOrigin = place
                                            sessionToken = AutocompleteSessionToken.newInstance()
                                            nearby = nearbyResponders(pins, place.latitude, place.longitude)
                                            val bounds = mapBoundsForRadiusKm(
                                                place.latitude,
                                                place.longitude,
                                                SEARCH_VIEW_RADIUS_KM,
                                            )
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngBounds(
                                                    LatLngBounds(
                                                        LatLng(bounds.south, bounds.west),
                                                        LatLng(bounds.north, bounds.east),
                                                    ),
                                                    80,
                                                ),
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(prediction.primary, style = TypeScale.body, color = FieldTheme.textPrimary)
                                Text(prediction.full, style = TypeScale.caption, color = FieldTheme.textMuted)
                            }
                            HorizontalDivider(color = FieldTheme.hairline)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FieldTheme.accent)
                }
                loadFailed && pins.isEmpty() -> EmptyState(
                    title = MAP_LOAD_FAILED,
                    actionTitle = "רענון",
                    onAction = {
                        scope.launch {
                            loading = true
                            loadFailed = false
                            runCatching { pins = YahpazAPI.fetchUnitMapPins() }
                                .onFailure { loadFailed = true }
                            loading = false
                        }
                    },
                )
                else -> {
                    val nowMs = System.currentTimeMillis()
                    val liveFresh = freshLivePins(livePins, nowMs)
                        .filter { pointInBbox(it.lat, it.lng, padBbox(bbox)) }
                    val (clusters, catalogPoints) = catalogViewForViewport(pins, bbox, zoom)
                    val mileInView = milePostsInView(milePosts, bbox)
                    val showMiles = shouldShowMilePosts(layers.milePosts, zoom, mileInView.size)

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            mapToolbarEnabled = false,
                            compassEnabled = true,
                        ),
                        onMapLoaded = {
                            val position = cameraPositionState.position
                            zoom = position.zoom
                            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                                bbox = LatLngBbox(
                                    south = bounds.southwest.latitude,
                                    west = bounds.southwest.longitude,
                                    north = bounds.northeast.latitude,
                                    east = bounds.northeast.longitude,
                                )
                            }
                        },
                    ) {
                        LaunchedEffect(cameraPositionState.isMoving) {
                            if (!cameraPositionState.isMoving) {
                                zoom = cameraPositionState.position.zoom
                                cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                                    bbox = LatLngBbox(
                                        south = bounds.southwest.latitude,
                                        west = bounds.southwest.longitude,
                                        north = bounds.northeast.latitude,
                                        east = bounds.northeast.longitude,
                                    )
                                }
                            }
                        }

                        PoliceGeoJsonEffect(enabled = layers.policeStations)

                        catalogPoints.forEach { pin ->
                            val chrome = mapUserPinChrome(pin)
                            val hue = when {
                                chrome.unavailable -> BitmapDescriptorFactory.HUE_AZURE
                                chrome.tone == MapUserPinTone.PHONE -> BitmapDescriptorFactory.HUE_GREEN
                                else -> BitmapDescriptorFactory.HUE_BLUE
                            }
                            val key = "${pin.userId}:${pin.kind}:${pin.lat}:${pin.lng}"
                            Marker(
                                state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                                title = pin.label,
                                snippet = chrome.tooltip,
                                icon = BitmapDescriptorFactory.defaultMarker(hue),
                                zIndex = if (focusedPinKey == key) 2f else 1f,
                            )
                        }
                        clusters.forEach { cluster ->
                            Marker(
                                state = MarkerState(position = LatLng(cluster.lat, cluster.lng)),
                                title = cluster.count.toString(),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(cluster.lat, cluster.lng),
                                                zoomAfterCatalogClusterClick(zoom),
                                            ),
                                        )
                                    }
                                    true
                                },
                            )
                        }
                        searchOrigin?.let { origin ->
                            Marker(
                                state = MarkerState(position = origin),
                                title = "חיפוש",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                                zIndex = 3f,
                            )
                        }
                        if (showMiles) {
                            mileInView.forEach { post ->
                                Marker(
                                    state = MarkerState(position = LatLng(post.lat, post.lng)),
                                    title = milePostTooltip(post),
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
                                    zIndex = 0.5f,
                                )
                            }
                        }
                        liveFresh.forEach { pin ->
                            Marker(
                                state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                                title = pin.label,
                                snippet = pin.tooltip,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                zIndex = 4f,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .zIndex(2f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = FieldTheme.raised) {
                            IconButton(onClick = { layersOpen = true }) {
                                Icon(Icons.Outlined.Layers, contentDescription = MAP_LAYERS_TITLE, tint = FieldTheme.accent)
                            }
                        }
                        MapLegendCard()
                    }

                    if (layersOpen) {
                        LayersSheet(
                            layers = layers,
                            onChange = { layers = it },
                            onClose = { layersOpen = false },
                        )
                    }
                }
            }
        }

        if (searchOrigin != null) {
            NearbySheet(
                nearby = nearby,
                onFocus = { row ->
                    focusedPinKey = "${row.userId}:${row.kind}:${row.lat}:${row.lng}"
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(row.lat, row.lng), 14f),
                        )
                    }
                },
                onClear = {
                    searchQuery = ""
                    searchOrigin = null
                    nearby = emptyList()
                    focusedPinKey = null
                },
            )
        }
    }
}

@Composable
private fun PoliceGeoJsonEffect(enabled: Boolean) {
    val context = LocalContext.current
    var layer by remember { mutableStateOf<GeoJsonLayer?>(null) }
    MapEffect(enabled) { map ->
        layer?.removeLayerFromMap()
        layer = null
        if (!enabled) return@MapEffect
        runCatching {
            val json = context.assets.open("map/police-station-boundaries.geojson").bufferedReader().use { it.readText() }
            val geo = GeoJsonLayer(map, JSONObject(json))
            geo.defaultPolygonStyle.apply {
                strokeColor = 0xFF1D4E89.toInt()
                strokeWidth = 2f
                fillColor = 0x291D4E89
            }
            geo.addLayerToMap()
            layer = geo
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            layer?.removeLayerFromMap()
            layer = null
        }
    }
}

@Composable
private fun MapLegendCard() {
    Surface(shape = RoundedCornerShape(12.dp), color = FieldTheme.raised.copy(alpha = 0.95f)) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendRow(FieldTheme.accent, MAP_LEGEND_ACTIVE)
            LegendRow(FieldTheme.done, MAP_LEGEND_PHONE)
            LegendRow(FieldTheme.draft, MAP_LEGEND_UNAVAILABLE)
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = TypeScale.caption, color = FieldTheme.textSecondary)
    }
}

@Composable
private fun LayersSheet(
    layers: OpsMapLayers,
    onChange: (OpsMapLayers) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(onClick = onClose),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = FieldTheme.raised,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(MAP_LAYERS_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "סגירה")
                    }
                }
                LayerToggle(MAP_LAYER_POLICE, layers.policeStations) {
                    onChange(layers.copy(policeStations = it))
                }
                LayerToggle(MAP_LAYER_MILE_POSTS, layers.milePosts) {
                    onChange(layers.copy(milePosts = it))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LayerToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TypeScale.body, color = FieldTheme.textPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NearbySheet(
    nearby: List<NearbyResponder>,
    onFocus: (NearbyResponder) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        color = FieldTheme.raised,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(MAP_NEARBY_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
                TextButton(onClick = onClear) {
                    Text("נקה", color = FieldTheme.accent)
                }
            }
            if (nearby.isEmpty()) {
                Text("אין מתנדבים בטווח 30 ק״מ", style = TypeScale.caption, color = FieldTheme.textMuted)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(nearby, key = { it.userId }) { row ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFocus(row) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                "${row.callsign} · ${row.name} · ${formatMapDistanceKm(row.km)}",
                                style = TypeScale.body,
                                color = FieldTheme.textPrimary,
                            )
                            Text(row.formattedAddress, style = TypeScale.caption, color = FieldTheme.textMuted)
                        }
                    }
                }
            }
        }
    }
}

private data class PlacePredictionUi(
    val placeId: String,
    val primary: String,
    val full: String,
)

private fun ensurePlacesInitialized(context: Context, apiKey: String) {
    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, apiKey)
    }
}

private suspend fun fetchPlaceLatLng(context: Context, placeId: String): LatLng? =
    withContext(Dispatchers.IO) {
        runCatching {
            val client = Places.createClient(context)
            val request = FetchPlaceRequest.builder(
                placeId,
                listOf(Place.Field.LAT_LNG),
            ).build()
            val place = client.fetchPlace(request).await().place
            place.latLng
        }.getOrNull()
    }

@Serializable
private data class MilePostJson(
    val road: String,
    val km: Int,
    val lat: Double,
    val lng: Double,
)

private suspend fun loadMilePostsAsset(context: Context): List<MilePost> =
    withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.assets.open("map/mile-posts.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }
                .decodeFromString<List<MilePostJson>>(raw)
                .map { MilePost(road = it.road.trim(), km = it.km, lat = it.lat, lng = it.lng) }
                .filter { it.road.isNotEmpty() && it.km >= 1 }
        }.getOrElse { emptyList() }
    }
