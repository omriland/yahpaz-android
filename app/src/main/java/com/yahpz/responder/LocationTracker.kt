package com.yahpz.responder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.yahpz.domain.LatLngAt
import com.yahpz.domain.shouldEmitPing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var token: String? = null
    private var last: LatLngAt? = null
    private var started = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handle(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun start(token: String) {
        this.token = token
        started = true
        last = null
        _state.value = TrackUiState()
        if (!hasLocationPermission()) {
            _state.value = TrackUiState(
                statusText = "יש לאשר מיקום בהגדרות כדי לשתף מיקום בזמן אירוע.",
                failed = "יש לאשר מיקום בהגדרות כדי לשתף מיקום בזמן אירוע.",
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
                statusText = "יש לאשר מיקום בהגדרות כדי לשתף מיקום בזמן אירוע.",
                failed = "יש לאשר מיקום בהגדרות כדי לשתף מיקום בזמן אירוע.",
            )
            return
        }
        startUpdates()
        scope.launch { load() }
    }

    fun stop() {
        started = false
        runCatching { manager.removeUpdates(listener) }
        _state.value = _state.value.copy(sharing = false)
    }

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
            manager.requestLocationUpdates(provider, 10_000L, 25f, listener, Looper.getMainLooper())
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
        if (!shouldEmitPing(last, next)) return
        last = next
        val token = token ?: return
        scope.launch {
            val result = YahpazAPI.pingTrack(token, next.lat, next.lng, location.accuracy.toDouble())
            if (result.ended == true || result.error == "ended") {
                _state.value = _state.value.copy(ended = true, statusText = "המעקב הסתיים.", sharing = false)
                stop()
                return@launch
            }
            if (result.ok == false) {
                val error = result.error
                if (error == "invalid" || error == "expired") {
                    _state.value = _state.value.copy(
                        failed = "קישור המעקב אינו תקף.",
                        statusText = "קישור המעקב אינו תקף.",
                    )
                    stop()
                    return@launch
                }
            }
            _state.value = _state.value.copy(sharing = true, statusText = "המיקום משותף עם האחמ״ש.")
        }
    }
}
