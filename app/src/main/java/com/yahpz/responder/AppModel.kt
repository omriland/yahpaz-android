package com.yahpz.responder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yahpz.domain.IMPERSONATION_STARTED
import com.yahpz.domain.IMPERSONATION_STOPPED
import com.yahpz.domain.ROLE_PREVIEW_STARTED
import com.yahpz.domain.ROLE_PREVIEW_STOPPED
import com.yahpz.domain.AppRole
import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.effectiveRoles
import com.yahpz.domain.parseRolePreviewRole
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.BROADCAST_LOAD_FAILED
import com.yahpz.domain.BroadcastCandidate
import com.yahpz.domain.BroadcastDraft
import com.yahpz.domain.BroadcastLogEntry
import com.yahpz.domain.EVENT_DELETED
import com.yahpz.domain.MY_ACTIVE_EVENT_DISMISSED
import com.yahpz.domain.MY_ACTIVE_EVENT_PINNED
import com.yahpz.domain.MY_ACTIVE_REMOVE_LOCKED
import com.yahpz.domain.canRemoveFromMyActive
import com.yahpz.domain.EVENT_DRAFT_PARTIAL_SAVED
import com.yahpz.domain.EVENT_DRAFT_SAVED
import com.yahpz.domain.FEEDBACK_SENT
import com.yahpz.domain.EventDraft
import com.yahpz.domain.InviteDraft
import com.yahpz.domain.USER_DELETED
import com.yahpz.domain.USER_SAVED
import com.yahpz.domain.otpFlagToast
import com.yahpz.domain.KM_DISCREPANCY_APPLIED
import com.yahpz.domain.REPORT_FAILED_TITLE
import com.yahpz.domain.ReportKindId
import com.yahpz.domain.ReportRow
import com.yahpz.domain.SHIFT_DRAFT_SAVED
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.broadcastResultCopy
import com.yahpz.domain.canToggleEventCancelled
import com.yahpz.domain.defaultMobileView
import com.yahpz.domain.defaultReportRange
import com.yahpz.domain.eventCancelToast
import com.yahpz.domain.isAdmin
import com.yahpz.domain.reportSpec
import com.yahpz.domain.setActiveToast
import com.yahpz.domain.isResponder
import com.yahpz.domain.israelToday
import com.yahpz.domain.managesUnit
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.StampTone
import com.yahpz.domain.androidSessionRpcParams
import com.yahpz.domain.parseTrackToken
import com.yahpz.domain.shouldReportAndroidSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab { INBOX, SHIFTS, CONTACTS, UNIT_EVENTS, UNIT_SHIFTS, TOOLS, REPORTS, PROFILE }

enum class ToolsDestination {
    HUB, REPORT_CATALOG, REPORT, ADMIN_USERS, NEW_EVENT, EDIT_EVENT, NEW_SHIFT, EDIT_SHIFT, BROADCAST, FUEL_QUARTER, CLOSED_LISTS, COCKPIT
}

fun appTabForMobileView(view: String): AppTab = when (view) {
    "mine" -> AppTab.INBOX
    "my_shifts" -> AppTab.SHIFTS
    "contacts" -> AppTab.CONTACTS
    "events" -> AppTab.UNIT_EVENTS
    "shifts" -> AppTab.UNIT_SHIFTS
    "users" -> AppTab.TOOLS
    "reports" -> AppTab.REPORTS
    "profile" -> AppTab.PROFILE
    else -> AppTab.INBOX
}

data class AppUiState(
    val booting: Boolean = true,
    val forceUpdate: ForceUpdateRequired? = null,
    val userId: String? = null,
    val profile: ProfileRecord? = null,
    val actualRoles: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val impersonating: Boolean = false,
    val impersonationName: String? = null,
    val impersonationCallsign: String? = null,
    val previewRole: String? = null,
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
    val myActiveUnitEvents: List<EventListItem> = emptyList(),
    val myActiveEventPrefs: List<MyActiveEventPrefRow> = emptyList(),
    val myActivePinnedEvents: List<EventListItem> = emptyList(),
    val unitEventsFailed: Boolean = false,
    val unitEventsLoading: Boolean = false,
    val unitShifts: List<ShiftListItem> = emptyList(),
    val unitShiftsFailed: Boolean = false,
    val unitShiftsLoading: Boolean = false,
    val adminUsers: List<AdminUserListItem> = emptyList(),
    val adminUsersFailed: Boolean = false,
    val adminUsersLoading: Boolean = false,
    val broadcastCandidates: List<BroadcastCandidate> = emptyList(),
    val broadcastLog: List<BroadcastLogEntry> = emptyList(),
    val broadcastFailed: Boolean = false,
    val broadcastLoading: Boolean = false,
    val reportKind: ReportKindId = ReportKindId.OPEN_DOCUMENTATION,
    val reportRows: List<ReportRow> = emptyList(),
    val reportFailed: Boolean = false,
    val reportLoading: Boolean = false,
    val reportFrom: String? = null,
    val reportTo: String? = null,
    val lookups: EventLookups = EventLookups(),
    val assignableProfiles: List<AssignableProfile> = emptyList(),
    val lookupsLoading: Boolean = false,
    val lookupsFailed: Boolean = false,
    val editingEventId: String? = null,
    val editingShiftId: String? = null,
    val fuelQuarter: FuelQuarterWorkbook? = null,
    val fuelQuarterLoading: Boolean = false,
    val fuelQuarterFailed: Boolean = false,
    val fuelQuarterYear: Int? = null,
    val fuelQuarterQuarter: Int? = null,
    val tab: AppTab = AppTab.INBOX,
    val toolsDestination: ToolsDestination = ToolsDestination.HUB,
    val toast: String? = null,
    val toastTone: StampTone = StampTone.DONE,
    val trackToken: String? = null,
    val mustChangePassword: Boolean = false,
    val fillEventId: String? = null,
    val signingIn: Boolean = false,
    val signInError: String? = null,
    val privacyOpen: Boolean = false,
    val feedbackHiddenUntilRefresh: Boolean = false,
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
    private var lastAndroidReportAtMs: Long? = null

    fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(booting = true) }
            val forceUpdate = checkForceUpdate(BuildConfig.VERSION_CODE)
            if (forceUpdate != null) {
                _state.update { it.copy(forceUpdate = forceUpdate, booting = false) }
                reportAndroidSessionIfDue()
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

    fun onForeground() {
        viewModelScope.launch { reportAndroidSessionIfDue() }
    }

    fun applyIncomingUrl(url: String) {
        parseTrackToken(url)?.let { token ->
            _state.update { it.copy(trackToken = token) }
            return
        }
        if (isPrivacyPolicyUrl(url)) {
            _state.update { it.copy(privacyOpen = true) }
        }
    }

    fun openPrivacy() {
        _state.update { it.copy(privacyOpen = true) }
    }

    fun closePrivacy() {
        _state.update { it.copy(privacyOpen = false) }
    }

    fun hideFeedbackUntilRefresh() {
        _state.update { it.copy(feedbackHiddenUntilRefresh = true) }
    }

    suspend fun submitUserFeedback(
        kind: String,
        body: String,
        pagePath: String?,
        audioBytes: ByteArray?,
        audioMime: String?,
    ): String? {
        val error = YahpazAPI.submitUserFeedback(kind, body, pagePath, audioBytes, audioMime)
        if (error == null) showToast(FEEDBACK_SENT, StampTone.DONE)
        return error
    }

    fun setTab(tab: AppTab) {
        _state.update {
            it.copy(
                tab = tab,
                toolsDestination = if (tab == AppTab.TOOLS) {
                    ToolsDestination.ADMIN_USERS
                } else {
                    ToolsDestination.HUB
                },
            )
        }
    }

    fun setToolsDestination(destination: ToolsDestination) {
        _state.update {
            it.copy(
                toolsDestination = destination,
                editingEventId = if (destination == ToolsDestination.EDIT_EVENT) it.editingEventId else null,
                editingShiftId = if (destination == ToolsDestination.EDIT_SHIFT) it.editingShiftId else null,
            )
        }
    }

    fun openEditEvent(eventId: String) {
        _state.update {
            it.copy(
                tab = AppTab.UNIT_EVENTS,
                toolsDestination = ToolsDestination.EDIT_EVENT,
                editingEventId = eventId,
            )
        }
    }

    fun openEditShift(shiftId: String) {
        _state.update {
            it.copy(
                tab = AppTab.UNIT_SHIFTS,
                toolsDestination = ToolsDestination.EDIT_SHIFT,
                editingShiftId = shiftId,
            )
        }
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
            lastAndroidReportAtMs = null
            _state.value = AppUiState(booting = false)
        }
    }

    fun startRolePreview(role: AppRole) {
        viewModelScope.launch {
            ViewAsStore.clearImpersonation()
            ViewAsStore.writeRolePreview(role.raw)
            val id = YahpazAPI.sessionUserId() ?: return@launch
            runCatching { applySession(id) }
            showToast(ROLE_PREVIEW_STARTED, StampTone.DONE)
        }
    }

    fun stopRolePreview() {
        viewModelScope.launch {
            ViewAsStore.clearRolePreview()
            val id = YahpazAPI.sessionUserId() ?: return@launch
            runCatching { applySession(id) }
            showToast(ROLE_PREVIEW_STOPPED, StampTone.DONE)
        }
    }

    suspend fun startImpersonation(targetUserId: String): String? {
        YahpazAPI.startImpersonation(targetUserId)?.let { return it }
        val id = YahpazAPI.sessionUserId() ?: return "פתיחת הצפייה נכשלה. נסו שוב."
        return try {
            applySession(id)
            showToast(IMPERSONATION_STARTED, StampTone.DONE)
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            error.message ?: "פתיחת הצפייה נכשלה. נסו שוב."
        }
    }

    fun stopImpersonation() {
        viewModelScope.launch {
            val error = YahpazAPI.stopImpersonation()
            if (error != null) {
                showToast(error, StampTone.PENDING)
                if (error.contains("התחברו")) {
                    _state.value = AppUiState(booting = false)
                }
                return@launch
            }
            val id = YahpazAPI.sessionUserId()
            if (id == null) {
                _state.value = AppUiState(booting = false)
                showToast(IMPERSONATION_STOPPED, StampTone.DONE)
                return@launch
            }
            runCatching { applySession(id) }
            showToast(IMPERSONATION_STOPPED, StampTone.DONE)
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

    suspend fun reloadUnitEvents() {
        if (_state.value.userId == null) return
        val hadEvents = _state.value.unitEvents.isNotEmpty()
        _state.update { it.copy(unitEventsLoading = !hadEvents, unitEventsFailed = false) }
        try {
            val events = YahpazAPI.fetchUnitEvents()
            val active = YahpazAPI.fetchMyActiveUnitEvents()
            val prefs = YahpazAPI.fetchMyActiveEventPrefs()
            val knownIds = (events.map { it.id } + active.map { it.id }).toSet()
            val pinnedIds = prefs.filter { it.kind == "pin" }.map { it.eventId }.toSet()
            val extra = YahpazAPI.fetchUnitEventsByIds((pinnedIds - knownIds).toList())
            _state.update {
                it.copy(
                    unitEvents = events,
                    myActiveUnitEvents = active,
                    myActiveEventPrefs = prefs,
                    myActivePinnedEvents = extra,
                    unitEventsFailed = false,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _state.update { it.copy(unitEventsFailed = true) }
        } finally {
            _state.update { it.copy(unitEventsLoading = false) }
        }
    }

    suspend fun reloadUnitShifts() = loadSection(
        read = { it.unitShifts },
        write = { state, rows, loading, failed ->
            state.copy(
                unitShifts = rows ?: state.unitShifts,
                unitShiftsLoading = loading,
                unitShiftsFailed = failed,
            )
        },
        failureMessage = "טעינת המשמרות נכשלה. בדקו את החיבור ונסו שוב.",
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

    /** The recipient pool and the sent log are always shown together, so they load as one unit. */
    suspend fun reloadBroadcast() {
        if (_state.value.userId == null) return
        val hadCandidates = _state.value.broadcastCandidates.isNotEmpty()
        _state.update {
            it.copy(broadcastLoading = !hadCandidates, broadcastFailed = false)
        }
        try {
            val candidates = YahpazAPI.fetchBroadcastCandidates()
            val log = YahpazAPI.fetchBroadcastLog()
            _state.update {
                it.copy(broadcastCandidates = candidates, broadcastLog = log, broadcastFailed = false)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (hadCandidates) {
                showToast(BROADCAST_LOAD_FAILED, StampTone.PENDING)
            } else {
                _state.update { it.copy(broadcastFailed = true) }
            }
        } finally {
            _state.update { it.copy(broadcastLoading = false) }
        }
    }

    /** Returns an error to show inline, or null after toasting the send summary. */
    suspend fun sendBroadcast(draft: BroadcastDraft): String? {
        val result = YahpazAPI.sendUnitBroadcast(draft)
        result.exceptionOrNull()?.let { return it.message ?: BROADCAST_LOAD_FAILED }
        showToast(broadcastResultCopy(result.getOrThrow()), StampTone.DONE)
        viewModelScope.launch { reloadBroadcast() }
        return null
    }

    suspend fun inviteUser(draft: InviteDraft): AdminUsersActionResult {
        val result = YahpazAPI.inviteAdminUser(draft)
        if (result.ok) reloadAdminUsers()
        return result
    }

    suspend fun saveAdminUser(draft: InviteDraft): String? {
        YahpazAPI.saveAdminUser(draft)?.let { return it }
        reloadAdminUsers()
        showToast(USER_SAVED, StampTone.DONE)
        return null
    }

    suspend fun deleteAdminUser(userId: String): String? {
        YahpazAPI.deleteAdminUser(userId)?.let { return it }
        reloadAdminUsers()
        showToast(USER_DELETED, StampTone.DONE)
        return null
    }

    suspend fun resendAdminInvite(userId: String): AdminUsersActionResult =
        YahpazAPI.resendAdminInvite(userId)

    suspend fun copyAdminInviteLink(userId: String): AdminUsersActionResult =
        YahpazAPI.copyAdminInviteLink(userId)

    suspend fun setAdminUserOtp(userId: String, kind: String, enabled: Boolean): String? {
        YahpazAPI.setAdminUserOtp(userId, kind, enabled)?.let { return it }
        reloadAdminUsers()
        showToast(otpFlagToast(kind, enabled), StampTone.DONE)
        return null
    }

    suspend fun deleteAdminVehicle(vehicleId: String): String? =
        YahpazAPI.deleteAdminVehicle(vehicleId)

    suspend fun archiveAdminVehicle(vehicleId: String): String? =
        YahpazAPI.archiveAdminVehicle(vehicleId)

    suspend fun unarchiveAdminVehicle(vehicleId: String): String? =
        YahpazAPI.unarchiveAdminVehicle(vehicleId)

    suspend fun createOwnVehicle(plateNumber: String, model: String): String? =
        YahpazAPI.createOwnVehicle(plateNumber, model)

    suspend fun updateOwnVehicle(vehicleId: String, plateNumber: String, model: String): String? =
        YahpazAPI.updateOwnVehicle(vehicleId, plateNumber, model)

    suspend fun setDefaultVehicle(vehicleId: String): String? =
        YahpazAPI.setDefaultVehicle(vehicleId)

    suspend fun isVehicleAttachedToEvents(
        userId: String,
        vehicleId: String,
        plateNumber: String,
    ): Boolean = YahpazAPI.isVehicleAttachedToEvents(userId, vehicleId, plateNumber)

    suspend fun setUserActive(userId: String, active: Boolean): String? {
        YahpazAPI.setAdminUserActive(userId, active)?.let { return it }
        reloadAdminUsers()
        showToast(setActiveToast(active), StampTone.DONE)
        return null
    }

    /** Reports share one slot of state, so opening a different kind clears the previous rows. */
    fun openReport(kind: ReportKindId) {
        _state.update { current ->
            val switching = current.reportKind != kind
            current.copy(
                toolsDestination = ToolsDestination.REPORT,
                reportKind = kind,
                reportRows = if (switching) emptyList() else current.reportRows,
                reportFrom = if (switching) null else current.reportFrom,
                reportTo = if (switching) null else current.reportTo,
                reportFailed = false,
            )
        }
    }

    suspend fun reloadReport(from: String, to: String) {
        val kind = _state.value.reportKind
        _state.update { it.copy(reportFrom = from, reportTo = to) }
        loadSection(
            read = { it.reportRows },
            write = { state, rows, loading, failed ->
                state.copy(
                    reportRows = rows ?: state.reportRows,
                    reportLoading = loading,
                    reportFailed = failed,
                )
            },
            failureMessage = REPORT_FAILED_TITLE,
            fetch = { YahpazAPI.fetchReport(kind, from, to) },
        )
    }

    /**
     * The only report write today: replace the lead's km with the responder's odometer.
     * [ReportRow.actionId] carries the assignment id the loader put there.
     */
    suspend fun applyReportRowAction(actionId: String): String? {
        YahpazAPI.applyLeadKmFromOdometer(actionId)?.let { return it }
        val from = _state.value.reportFrom
        val to = _state.value.reportTo
        if (from != null && to != null) reloadReport(from, to)
        showToast(KM_DISCREPANCY_APPLIED, StampTone.DONE)
        return null
    }

    fun defaultReportRange(kind: ReportKindId): Pair<String, String> =
        defaultReportRange(reportSpec(kind), israelToday())

    /** Closed lists and the assignable crew back both create forms, so they load together. */
    suspend fun reloadLookups() {
        if (_state.value.userId == null) return
        _state.update { it.copy(lookupsLoading = true, lookupsFailed = false) }
        try {
            val lookups = YahpazAPI.fetchEventLookups()
            val profiles = YahpazAPI.fetchAssignableProfiles()
            _state.update {
                it.copy(lookups = lookups, assignableProfiles = profiles, lookupsFailed = false)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _state.update { it.copy(lookupsFailed = true) }
        } finally {
            _state.update { it.copy(lookupsLoading = false) }
        }
    }

    suspend fun createUnitEvent(
        draft: EventDraft,
        allowPartial: Boolean = false,
        stayOnForm: Boolean = false,
    ): String? {
        YahpazAPI.createUnitEvent(
            draft = draft,
            districts = _state.value.lookups.districts,
            vehicleKinds = _state.value.lookups.vehicleKinds,
            allowPartial = allowPartial,
        )?.let { return it }
        reloadUnitEvents()
        viewModelScope.launch { reloadEvents() }
        showToast(if (allowPartial) EVENT_DRAFT_PARTIAL_SAVED else EVENT_DRAFT_SAVED, StampTone.DONE)
        if (!stayOnForm) setTab(AppTab.UNIT_EVENTS)
        return null
    }

    suspend fun updateUnitEvent(
        eventId: String,
        draft: EventDraft,
        previousIsCancelled: Boolean,
        allowPartial: Boolean = false,
        stayOnForm: Boolean = false,
    ): String? {
        YahpazAPI.updateUnitEvent(
            eventId = eventId,
            draft = draft,
            districts = _state.value.lookups.districts,
            vehicleKinds = _state.value.lookups.vehicleKinds,
            viewerIsAdmin = _state.value.canAdmin,
            previousIsCancelled = previousIsCancelled,
            allowPartial = allowPartial,
        )?.let { return it }
        reloadUnitEvents()
        viewModelScope.launch { reloadEvents() }
        showToast(if (allowPartial) EVENT_DRAFT_PARTIAL_SAVED else EVENT_DRAFT_SAVED, StampTone.DONE)
        if (!stayOnForm) setTab(AppTab.UNIT_EVENTS)
        return null
    }

    suspend fun deleteUnitEvent(eventId: String): String? {
        YahpazAPI.deleteUnitEvent(eventId)?.let { return it }
        reloadUnitEvents()
        viewModelScope.launch { reloadEvents() }
        showToast(EVENT_DELETED, StampTone.DONE)
        return null
    }

    suspend fun addEventToMyActiveBoard(eventId: String) {
        val autoIds = _state.value.myActiveUnitEvents.map { it.id }.toSet()
        val error = YahpazAPI.addEventToMyActive(eventId, alreadyAuto = eventId in autoIds)
        if (error != null) {
            showToast(error, StampTone.PENDING)
            return
        }
        reloadUnitEvents()
        showToast(MY_ACTIVE_EVENT_PINNED, StampTone.DONE)
    }

    suspend fun removeEventFromMyActiveBoard(eventId: String) {
        val snapshot = _state.value
        val viewerId = snapshot.userId
        val event = (
            snapshot.unitEvents + snapshot.myActiveUnitEvents + snapshot.myActivePinnedEvents
            ).firstOrNull { it.id == eventId }
        if (
            event != null &&
            viewerId != null &&
            !canRemoveFromMyActive(viewerId, event.shiftLeadId, event.status, event.isCancelled)
        ) {
            showToast(MY_ACTIVE_REMOVE_LOCKED, StampTone.PENDING)
            return
        }
        val autoIds = snapshot.myActiveUnitEvents.map { it.id }.toSet()
        val error = YahpazAPI.removeEventFromMyActive(eventId, isAuto = eventId in autoIds)
        if (error != null) {
            showToast(error, StampTone.PENDING)
            return
        }
        reloadUnitEvents()
        showToast(MY_ACTIVE_EVENT_DISMISSED, StampTone.DONE)
    }

    suspend fun createUnitShift(draft: ShiftDraft): String? {
        YahpazAPI.createUnitShift(draft)?.let { return it }
        reloadUnitShifts()
        viewModelScope.launch { reloadShifts() }
        showToast(SHIFT_DRAFT_SAVED, StampTone.DONE)
        setTab(AppTab.UNIT_SHIFTS)
        return null
    }

    suspend fun updateUnitShift(shiftId: String, draft: ShiftDraft): String? {
        YahpazAPI.updateUnitShift(shiftId, draft)?.let { return it }
        reloadUnitShifts()
        viewModelScope.launch { reloadShifts() }
        showToast(SHIFT_DRAFT_SAVED, StampTone.DONE)
        setTab(AppTab.UNIT_SHIFTS)
        return null
    }

    suspend fun reloadFuelQuarter(year: Int, quarter: Int) {
        if (_state.value.userId == null || !_state.value.canAdmin) return
        _state.update {
            it.copy(
                fuelQuarterLoading = true,
                fuelQuarterFailed = false,
                fuelQuarterYear = year,
                fuelQuarterQuarter = quarter,
            )
        }
        try {
            val workbook = YahpazAPI.loadFuelQuarterWorkbook(year, quarter)
            _state.update {
                it.copy(fuelQuarter = workbook, fuelQuarterFailed = false)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _state.update { it.copy(fuelQuarter = null, fuelQuarterFailed = true) }
        } finally {
            _state.update { it.copy(fuelQuarterLoading = false) }
        }
    }

    suspend fun setEventCancelled(eventId: String, isCancelled: Boolean): String? {
        canToggleEventCancelled(isCancelled, _state.value.canAdmin)?.let { return it }
        YahpazAPI.setEventCancelled(eventId, isCancelled)?.let { return it }
        reloadUnitEvents()
        showToast(eventCancelToast(isCancelled), StampTone.DONE)
        return null
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
            val preview = parseRolePreviewRole(ViewAsStore.rolePreviewRaw())
            val visible = effectiveRoles(roles, preview)
            val impersonation = ViewAsStore.readImpersonation()
            _state.update {
                it.copy(
                    userId = userId,
                    profile = profile,
                    actualRoles = roles,
                    roles = visible,
                    impersonating = impersonation != null,
                    impersonationName = impersonation?.targetFullName,
                    impersonationCallsign = impersonation?.targetCallsign,
                    previewRole = preview?.raw,
                    tab = appTabForMobileView(defaultMobileView(visible)),
                    toolsDestination = ToolsDestination.HUB,
                    mustChangePassword = profile.mustChangePassword,
                    vehiclesLoading = true,
                    vehiclesFailed = false,
                    fillEventId = null,
                )
            }
            reloadEvents()
            reloadShifts()
            reloadVehicles()
            // Secondary surfaces load in the background so boot is not blocked on them.
            viewModelScope.launch { reloadContacts() }
            if (managesUnit(visible)) {
                viewModelScope.launch { reloadUnitEvents() }
                viewModelScope.launch { reloadUnitShifts() }
                viewModelScope.launch { reloadLookups() }
            }
            if (isAdmin(visible)) viewModelScope.launch { reloadAdminUsers() }
            reportAndroidSessionIfDue()
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

    private suspend fun reportAndroidSessionIfDue() {
        if (YahpazAPI.sessionUserId() == null) return
        val now = System.currentTimeMillis()
        if (!shouldReportAndroidSession(lastAndroidReportAtMs, now)) return
        val params = androidSessionRpcParams(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
        runCatching { YahpazAPI.reportAndroidSession(params.versionCode, params.versionName) }
            .onSuccess { lastAndroidReportAtMs = now }
    }
}
