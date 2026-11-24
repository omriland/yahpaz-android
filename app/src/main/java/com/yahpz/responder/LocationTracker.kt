package com.yahpz.responder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.yahpz.domain.LOCATION_PERMISSION_REQUIRED_TEXT
import com.yahpz.domain.LatLngAt
import com.yahpz.domain.errorAfterPermissionResult
import com.yahpz.domain.pingBaselineAfterAttempt
import com.yahpz.domain.sharingStatusAfterPing
import com.yahpz.domain.shouldEmitPing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TrackUiState(
    val statusText: String = "ממתינים לאישור מיקום…",
    val ended: Boolean = false,
    val failed: String? = null,
    val sharing: Boolean = false,
)

class LocationTracker(private val context: Context) {
    private val _state = MutableStateFlow(TrackUiState())
    val state: StateFlow<TrackUiState> = _state
    private var scope = newScope()
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var token: String? = null
    private var last: LatLngAt? = null
    private var started = false
    private var pingJob: Job? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handle(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    private fun newScope() = CoroutineScope(Dispatchers.Main.immediate + Job())

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun start(token: String) {
        this.token = token
        started = true
        scope.cancel()
        scope = newScope()
        last = null
        _state.value = TrackUiState()
        if (!hasLocationPermission()) {
            _state.value = TrackUiState(
                statusText = LOCATION_PERMISSION_REQUIRED_TEXT,
                failed = LOCATION_PERMISSION_REQUIRED_TEXT,
            )
            return
        }
        startUpdates()
        scope.launch { load() }
    }

    fun onPermissionResult(granted: Boolean) {
        if (!started) return
        if (!granted) {
            _state.value = TrackUiState(
                statusText = LOCATION_PERMISSION_REQUIRED_TEXT,
                failed = LOCATION_PERMISSION_REQUIRED_TEXT,
            )
            return
        }
        clearStalePermissionError()
        startUpdates()
        scope.launch { load() }
    }

    private fun clearStalePermissionError() {
        val current = _state.value
        val error = errorAfterPermissionResult(current.failed, granted = true)
        if (error == current.failed) return
        _state.value = current.copy(failed = error, statusText = TrackUiState().statusText)
    }

    fun stop() {
        started = false
        runCatching { manager.removeUpdates(listener) }
        _state.value = _state.value.copy(sharing = false)
        scope.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            _state.value = _state.value.copy(
                failed = "לא הצלחנו לקבל מיקום. בדקו את ההרשאות ונסו שוב.",
                statusText = "לא הצלחנו לקבל מיקום. בדקו את ההרשאות ונסו שוב.",
            )
            return
        }
        runCatching {
            manager.requestLocationUpdates(provider, LOCATION_UPDATE_INTERVAL_MS, 0f, listener, Looper.getMainLooper())
        }.onFailure {
            _state.value = _state.value.copy(
                failed = "לא הצלחנו לקבל מיקום. בדקו את ההרשאות ונסו שוב.",
                statusText = "לא הצלחנו לקבל מיקום. בדקו את ההרשאות ונסו שוב.",
            )
        }
    }

    private suspend fun load() {
        val token = token ?: return
        val result = YahpazAPI.loadTrack(token)
        if (result.ended == true || result.error == "ended") {
            _state.value = _state.value.copy(ended = true, statusText = "המעקב הסתיים.", sharing = false)
            stop()
            return
        }
        if (result.ok == false && result.error != null) {
            _state.value = _state.value.copy(failed = result.error, statusText = result.error)
        }
    }

    private fun handle(location: Location) {
        if (!started || token == null || _state.value.ended) return
        val next = LatLngAt(location.latitude, location.longitude, location.time)
        if (pingJob?.isActive == true) return
        if (!shouldEmitPing(last, next)) return
        val token = token ?: return
        pingJob = scope.launch {
            val result = YahpazAPI.pingTrack(token, next.lat, next.lng, location.accuracy.toDouble())
            if (result.ended == true || result.error == "ended") {
                _state.value = _state.value.copy(ended = true, statusText = "המעקב הסתיים.", sharing = false)
                stop()
                return@launch
            }
            if (result.error == "invalid" || result.error == "expired") {
                _state.value = _state.value.copy(
                    failed = "קישור המעקב אינו תקף.",
                    statusText = "קישור המעקב אינו תקף.",
                )
                stop()
                return@launch
            }
            val delivered = result.ok != false
            last = pingBaselineAfterAttempt(last, next, delivered)
            val status = sharingStatusAfterPing(delivered)
            _state.value = _state.value.copy(
                sharing = status.sharing,
                failed = status.failed,
                statusText = status.statusText,
            )
        }
    }
}

// No minimum distance: a responder standing still still gets updates, so a failed ping is retried.
private const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
