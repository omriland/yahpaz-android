package com.yahpz.responder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.OPEN_DOC_DEFAULT_RANGE_DAYS
import com.yahpz.domain.OpenDocRow
import com.yahpz.domain.addCalendarDays
import com.yahpz.domain.isAdmin
import com.yahpz.domain.isResponder
import com.yahpz.domain.israelToday
import com.yahpz.domain.managesUnit
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

enum class AppTab { INBOX, SHIFTS, CONTACTS, UNIT_EVENTS, UNIT_SHIFTS, TOOLS, PROFILE }

enum class ToolsDestination { HUB, OPEN_DOC_REPORT, ADMIN_USERS }

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
    val contacts: List<UnitContact> = emptyList(),
    val contactsFailed: Boolean = false,
    val contactsLoading: Boolean = false,
    val unitEvents: List<EventListItem> = emptyList(),
    val unitEventsFailed: Boolean = false,
    val unitEventsLoading: Boolean = false,
    val unitShifts: List<ShiftListItem> = emptyList(),
    val unitShiftsFailed: Boolean = false,
    val unitShiftsLoading: Boolean = false,
    val adminUsers: List<AdminUserListItem> = emptyList(),
    val adminUsersFailed: Boolean = false,
    val adminUsersLoading: Boolean = false,
    val openDocRows: List<OpenDocRow> = emptyList(),
    val openDocFailed: Boolean = false,
    val openDocLoading: Boolean = false,
    val openDocFrom: String? = null,
    val openDocTo: String? = null,
    val tab: AppTab = AppTab.INBOX,
    val toolsDestination: ToolsDestination = ToolsDestination.HUB,
    val toast: String? = null,
    val toastTone: StampTone = StampTone.DONE,
    val trackToken: String? = null,
    val mustChangePassword: Boolean = false,
    val fillEventId: String? = null,
    val signingIn: Boolean = false,
    val signInError: String? = null,
) {
    val isSignedIn: Boolean get() = userId != null && profile != null
    val canManageUnit: Boolean get() = managesUnit(roles)
    val canAdmin: Boolean get() = isAdmin(roles)
    val canRespond: Boolean get() = isResponder(roles)
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
        _state.update {
            it.copy(
                tab = tab,
                toolsDestination = if (tab == AppTab.TOOLS) it.toolsDestination else ToolsDestination.HUB,
            )
        }
    }

    fun setToolsDestination(destination: ToolsDestination) {
        _state.update { it.copy(tab = AppTab.TOOLS, toolsDestination = destination) }
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

    /**
     * Shared load pattern for the list sections: first load shows a spinner and can fail into an
     * empty state, a refresh over existing rows keeps them and only toasts.
     */
    private suspend fun <T> loadSection(
        read: (AppUiState) -> List<T>,
        write: (AppUiState, List<T>?, Boolean, Boolean) -> AppUiState,
        failureMessage: String,
        fetch: suspend () -> List<T>,
    ) {
        if (_state.value.userId == null) return
        val hadRows = read(_state.value).isNotEmpty()
        _state.update { write(it, null, !hadRows, false) }
        try {
            val rows = fetch()
            _state.update { write(it, rows, false, false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadRows) {
                _state.update { write(it, null, false, false) }
                showToast(failureMessage, StampTone.PENDING)
            } else {
                _state.update { write(it, null, false, true) }
            }
        }
    }

    suspend fun reloadContacts() = loadSection(
        read = { it.contacts },
        write = { state, rows, loading, failed ->
            state.copy(
                contacts = rows ?: state.contacts,
                contactsLoading = loading,
                contactsFailed = failed,
            )
        },
        failureMessage = "טעינת אנשי הקשר נכשלה. בדקו את החיבור ונסו שוב.",
        fetch = { YahpazAPI.fetchUnitContacts() },
    )

    suspend fun reloadUnitEvents() = loadSection(
        read = { it.unitEvents },
        write = { state, rows, loading, failed ->
            state.copy(
                unitEvents = rows ?: state.unitEvents,
                unitEventsLoading = loading,
                unitEventsFailed = failed,
            )
        },
        failureMessage = "טעינת אירועי היחידה נכשלה. בדקו את החיבור ונסו שוב.",
        fetch = { YahpazAPI.fetchUnitEvents() },
    )

    suspend fun reloadUnitShifts() = loadSection(
        read = { it.unitShifts },
        write = { state, rows, loading, failed ->
            state.copy(
                unitShifts = rows ?: state.unitShifts,
                unitShiftsLoading = loading,
                unitShiftsFailed = failed,
            )
        },
        failureMessage = "טעינת משמרות היחידה נכשלה. בדקו את החיבור ונסו שוב.",
        fetch = { YahpazAPI.fetchUnitShifts() },
    )

    suspend fun reloadAdminUsers() = loadSection(
        read = { it.adminUsers },
        write = { state, rows, loading, failed ->
            state.copy(
                adminUsers = rows ?: state.adminUsers,
                adminUsersLoading = loading,
                adminUsersFailed = failed,
            )
        },
        failureMessage = "טעינת המשתמשים נכשלה. בדקו את החיבור ונסו שוב.",
        fetch = { YahpazAPI.fetchAdminUsers() },
    )

    suspend fun reloadOpenDocumentation(from: String, to: String) {
        _state.update { it.copy(openDocFrom = from, openDocTo = to) }
        loadSection(
            read = { it.openDocRows },
            write = { state, rows, loading, failed ->
                state.copy(
                    openDocRows = rows ?: state.openDocRows,
                    openDocLoading = loading,
                    openDocFailed = failed,
                )
            },
            failureMessage = "טעינת הדוח נכשלה. בדקו את החיבור ונסו שוב.",
            fetch = { YahpazAPI.fetchOpenDocumentation(from, to) },
        )
    }

    fun defaultOpenDocRange(): Pair<String, String> {
        val today = israelToday()
        return addCalendarDays(today, -OPEN_DOC_DEFAULT_RANGE_DAYS) to today
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
            // Secondary surfaces load in the background so boot is not blocked on them.
            viewModelScope.launch { reloadContacts() }
            if (managesUnit(roles)) {
                viewModelScope.launch { reloadUnitEvents() }
                viewModelScope.launch { reloadUnitShifts() }
            }
            if (isAdmin(roles)) viewModelScope.launch { reloadAdminUsers() }
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
