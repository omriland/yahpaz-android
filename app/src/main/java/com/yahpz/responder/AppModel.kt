package com.yahpz.responder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.StampTone
import com.yahpz.domain.parseTrackToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab { INBOX, SHIFTS, PROFILE }

data class AppUiState(
    val booting: Boolean = true,
    val forceUpdate: ForceUpdateRequired? = null,
    val userId: String? = null,
    val profile: ProfileRecord? = null,
    val roles: List<String> = emptyList(),
    val events: List<EventListItem> = emptyList(),
    val eventsFailed: Boolean = false,
    val eventsLoading: Boolean = false,
    val shifts: List<ShiftListItem> = emptyList(),
    val shiftsFailed: Boolean = false,
    val shiftsLoading: Boolean = false,
    val vehicles: List<ProfileVehicle> = emptyList(),
    val vehiclesFailed: Boolean = false,
    val vehiclesLoading: Boolean = false,
    val tab: AppTab = AppTab.INBOX,
    val toast: String? = null,
    val toastTone: StampTone = StampTone.DONE,
    val trackToken: String? = null,
    val mustChangePassword: Boolean = false,
    val fillEventId: String? = null,
    val signingIn: Boolean = false,
    val signInError: String? = null,
) {
    val isSignedIn: Boolean get() = userId != null && profile != null
}

class AppModel : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state
    private var toastJob: Job? = null

    fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(booting = true) }
            val forceUpdate = checkForceUpdate(BuildConfig.VERSION_CODE)
            if (forceUpdate != null) {
                _state.update { it.copy(forceUpdate = forceUpdate, booting = false) }
                return@launch
            }
            val id = YahpazAPI.sessionUserId()
            if (id != null) {
                runCatching { applySession(id) }
            } else {
                _state.update { it.copy(userId = null, profile = null) }
            }
            _state.update { it.copy(forceUpdate = null, booting = false) }
        }
    }

    fun applyIncomingUrl(url: String) {
        parseTrackToken(url)?.let { token ->
            _state.update { it.copy(trackToken = token) }
        }
    }

    fun setTab(tab: AppTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun openFill(eventId: String) {
        _state.update { it.copy(fillEventId = eventId) }
    }

    fun closeFill() {
        _state.update { it.copy(fillEventId = null) }
    }

    fun closeTrack() {
        _state.update { it.copy(trackToken = null) }
    }

    fun submitSignIn(email: String, password: String) {
        if (_state.value.signingIn) return
        viewModelScope.launch {
            _state.update { it.copy(signingIn = true, signInError = null) }
            val error = signIn(email, password)
            _state.update { it.copy(signingIn = false, signInError = error) }
        }
    }

    fun clearSignInError() {
        _state.update { it.copy(signInError = null) }
    }

    private suspend fun signIn(email: String, password: String): String? {
        YahpazAPI.signIn(email, password)?.let { return it }
        val id = YahpazAPI.sessionUserId() ?: return "הכניסה נכשלה. בדקו את החיבור ונסו שוב."
        return try {
            applySession(id)
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            error.message ?: "הכניסה נכשלה. בדקו את החיבור ונסו שוב."
        }
    }

    fun signOut() {
        viewModelScope.launch {
            YahpazAPI.signOut()
            _state.value = AppUiState(booting = false)
        }
    }

    suspend fun reloadEvents() {
        if (_state.value.userId == null) return
        val hadEvents = _state.value.events.isNotEmpty()
        _state.update {
            it.copy(
                eventsLoading = if (hadEvents) it.eventsLoading else true,
                eventsFailed = false,
            )
        }
        try {
            val events = YahpazAPI.fetchMyEvents()
            _state.update { it.copy(events = events, eventsFailed = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadEvents) {
                showToast("טעינת האירועים נכשלה. בדקו את החיבור ונסו שוב.", StampTone.PENDING)
            } else {
                _state.update { it.copy(eventsFailed = true) }
            }
        } finally {
            _state.update { it.copy(eventsLoading = false) }
        }
    }

    suspend fun reloadShifts() {
        if (_state.value.userId == null) return
        val hadShifts = _state.value.shifts.isNotEmpty()
        _state.update {
            it.copy(
                shiftsLoading = if (hadShifts) it.shiftsLoading else true,
                shiftsFailed = false,
            )
        }
        try {
            val shifts = YahpazAPI.fetchMyShifts()
            _state.update { it.copy(shifts = shifts, shiftsFailed = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadShifts) {
                showToast("טעינת המשמרות נכשלה. בדקו את החיבור ונסו שוב.", StampTone.PENDING)
            } else {
                _state.update { it.copy(shiftsFailed = true) }
            }
        } finally {
            _state.update { it.copy(shiftsLoading = false) }
        }
    }

    suspend fun reloadVehicles() {
        if (_state.value.userId == null) return
        val hadVehicles = _state.value.vehicles.isNotEmpty()
        _state.update {
            it.copy(
                vehiclesLoading = if (hadVehicles) it.vehiclesLoading else true,
                vehiclesFailed = false,
            )
        }
        try {
            val vehicles = YahpazAPI.fetchMyVehicles()
            _state.update { it.copy(vehicles = vehicles, vehiclesFailed = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadVehicles) {
                showToast("טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.", StampTone.PENDING)
            } else {
                _state.update { it.copy(vehiclesFailed = true) }
            }
        } finally {
            _state.update { it.copy(vehiclesLoading = false) }
        }
    }

    fun showToast(text: String, tone: StampTone = StampTone.DONE) {
        _state.update { it.copy(toast = text, toastTone = tone) }
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2_400)
            _state.update { it.copy(toast = null) }
        }
    }

    suspend fun saveAvailability(status: AvailabilityStatus, availableFrom: String?): String? {
        val userId = _state.value.userId ?: return "יש להתחבר מחדש."
        YahpazAPI.saveAvailability(userId, status, availableFrom)?.let { return it }
        _state.update { current ->
            current.copy(
                profile = current.profile?.copy(
                    availability = status,
                    availableFrom = if (status == AvailabilityStatus.AVAILABLE) {
                        null
                    } else {
                        availableFrom?.let { normalizeReturnDate(it) ?: it }
                    },
                ),
            )
        }
        showToast("הזמינות עודכנה.", StampTone.DONE)
        return null
    }

    suspend fun completePasswordChange(password: String): String? {
        YahpazAPI.updatePassword(password)?.let { return it }
        _state.update {
            it.copy(
                mustChangePassword = false,
                profile = it.profile?.copy(mustChangePassword = false),
            )
        }
        showToast("הסיסמה עודכנה.", StampTone.DONE)
        return null
    }

    private suspend fun applySession(userId: String) {
        try {
            val (profile, roles) = YahpazAPI.loadProfile()
            _state.update {
                it.copy(
                    userId = userId,
                    profile = profile,
                    roles = roles,
                    mustChangePassword = profile.mustChangePassword,
                    vehiclesLoading = true,
                    vehiclesFailed = false,
                )
            }
            reloadEvents()
            reloadShifts()
            reloadVehicles()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    userId = null,
                    profile = null,
                    vehicles = emptyList(),
                    vehiclesFailed = false,
                    vehiclesLoading = false,
                )
            }
            throw error
        }
    }
}
