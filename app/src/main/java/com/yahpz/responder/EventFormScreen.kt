package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.EVENT_ASSIGN_CLOSE
import com.yahpz.domain.EVENT_ASSIGN_EMPTY
import com.yahpz.domain.EVENT_ASSIGN_OPEN
import com.yahpz.domain.EVENT_SELF_ASSIGN_DISABLED_HINT
import com.yahpz.domain.EVENT_SELF_ASSIGN_ON_CREATE_ERROR
import com.yahpz.domain.EVENT_CANCELLED_LABEL
import com.yahpz.domain.EVENT_EDIT_LOAD_FAILED
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.EVENT_NEW_TITLE
import com.yahpz.domain.EVENT_PATROL_CALLSIGN_LABEL
import com.yahpz.domain.EVENT_SAVE_DRAFT_TITLE
import com.yahpz.domain.EVENT_SAVE_TITLE
import com.yahpz.domain.FOREIGN_EVENT_EDIT_BODY
import com.yahpz.domain.FOREIGN_EVENT_EDIT_CANCEL
import com.yahpz.domain.FOREIGN_EVENT_EDIT_CONFIRM
import com.yahpz.domain.EventDraft
import com.yahpz.domain.foreignEventEditLeadName
import com.yahpz.domain.foreignEventEditTitle
import com.yahpz.domain.isForeignShiftLeadEvent
import com.yahpz.domain.EventDraftErrors
import com.yahpz.domain.EventResponderDraft
import com.yahpz.domain.LookupOption
import com.yahpz.domain.NO_VEHICLE_KM_PLACEHOLDER
import com.yahpz.domain.applyDistrictRoadDefault
import com.yahpz.domain.bumpTreatedVehicle
import com.yahpz.domain.canToggleEventCancelled
import com.yahpz.domain.createIncludesSelfAssign
import com.yahpz.domain.eventDraftSummary
import com.yahpz.domain.isSelfAssignDisabledOnCreate
import com.yahpz.domain.israelToday
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.toggleEventResponder
import com.yahpz.domain.treatedQuantity
import com.yahpz.domain.updateEventResponder
import com.yahpz.domain.validateEventDraft
import com.yahpz.domain.validateEventDraftPartial
import kotlinx.coroutines.launch

const val NEW_EVENT_TITLE = EVENT_NEW_TITLE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    app: AppModel,
    ui: AppUiState,
    onBack: () -> Unit,
    eventId: String? = null,
) {
    val editing = eventId != null
    val scope = rememberCoroutineScope()
    var eventDate by remember { mutableStateOf(returnDateToInput(israelToday())) }
    var policeEventId by remember { mutableStateOf("") }
    var patrolCallsign by remember { mutableStateOf("") }
    var eventTypeId by remember { mutableStateOf("") }
    var roadId by remember { mutableStateOf("") }
    var districtId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var responders by remember { mutableStateOf(emptyList<EventResponderDraft>()) }
    var isCancelled by remember { mutableStateOf(false) }
    var busLane by remember { mutableStateOf(false) }
    var previousIsCancelled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(!editing) }
    var loadFailed by remember { mutableStateOf(false) }
    var shiftLeadId by remember { mutableStateOf<String?>(null) }
    var shiftLeadName by remember { mutableStateOf("") }
    var foreignEditAcked by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(EventDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var vehicleOwnerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var detailResponderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ui.userId) {
        if (ui.lookups.isEmpty && !ui.lookupsLoading) app.reloadLookups()
    }

    LaunchedEffect(responders.map { it.responderId }.sorted().joinToString()) {
        val ids = responders.map { it.responderId }
        if (ids.isEmpty()) {
            vehicleOwnerIds = emptySet()
            return@LaunchedEffect
        }
        val owners = runCatching {
            YahpazAPI.fetchVehiclesForResponders(ids).map { it.userId }.toSet()
        }.getOrDefault(emptySet())
        vehicleOwnerIds = owners
        val updated = responders.map { row -> row.copy(hasVehicle = owners.contains(row.responderId)) }
        if (updated != responders) responders = updated
    }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect
        loaded = false
        loadFailed = false
        try {
            val detail = YahpazAPI.fetchEventFormDetail(eventId)
            val vehicles = YahpazAPI.fetchVehiclesForResponders(detail.responders.map { it.responderId })
            val draft = detail.toDraft(vehicles.map { it.userId }.toSet())
            eventDate = draft.eventDate
            policeEventId = draft.policeEventId
            patrolCallsign = draft.patrolCallsign
            eventTypeId = draft.eventTypeId
            roadId = draft.roadId
            districtId = draft.districtId
            location = draft.location
            notes = draft.notes
            responders = draft.responders
            isCancelled = draft.isCancelled
            busLane = draft.busLane
            previousIsCancelled = draft.isCancelled
            shiftLeadId = detail.shiftLeadId
            shiftLeadName = foreignEventEditLeadName(
                detail.shiftLead?.fullName,
                detail.shiftLead?.callsign,
            )
            foreignEditAcked = false
            loaded = true
        } catch (_: Exception) {
            loadFailed = true
            loaded = true
        }
    }

    fun draft() = EventDraft(
        eventDate = eventDate,
        policeEventId = policeEventId,
        patrolCallsign = patrolCallsign,
        eventTypeId = eventTypeId,
        roadId = roadId,
        districtId = districtId,
        location = location,
        notes = notes,
        responders = responders,
        isCancelled = isCancelled,
        busLane = busLane,
    )

    val foreignEditPending =
        editing &&
            loaded &&
            !loadFailed &&
            isForeignShiftLeadEvent(ui.userId, shiftLeadId) &&
            !foreignEditAcked

    fun persist(allowPartial: Boolean) {
        if (isForeignShiftLeadEvent(ui.userId, shiftLeadId) && !foreignEditAcked) return
        val current = draft()
        if (!editing && createIncludesSelfAssign(ui.userId.orEmpty(), current.responders)) {
            formError = EVENT_SELF_ASSIGN_ON_CREATE_ERROR
            return
        }
        val next = if (allowPartial) {
            validateEventDraftPartial(current)
        } else {
            validateEventDraft(current, ui.lookups.districts)
        }
        errors = next
        if (!next.isEmpty) {
            formError = next.eventDate ?: next.formMessage
            return
        }
        formError = null
        scope.launch {
            saving = true
            formError = if (eventId != null) {
                app.updateUnitEvent(
                    eventId = eventId,
                    draft = current,
                    previousIsCancelled = previousIsCancelled,
                    allowPartial = allowPartial,
                )
            } else {
                app.createUnitEvent(current, allowPartial = allowPartial)
            }
            saving = false
        }
    }

    val detailResponder = detailResponderId?.let { id -> responders.firstOrNull { it.responderId == id } }
    val detailProfile = detailResponderId?.let { id -> ui.assignableProfiles.firstOrNull { it.id == id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolsBackRow(if (editing) EVENT_EDIT_TITLE else EVENT_NEW_TITLE, onBack)
        when {
            loadFailed -> EmptyState(
                title = EVENT_EDIT_LOAD_FAILED,
                actionTitle = "חזרה",
                onAction = onBack,
            )
            editing && !loaded -> LoadingBlock("טוען אירוע…")
            foreignEditPending -> { /* confirm sheet below */ }
            ui.lookupsFailed && ui.lookups.isEmpty -> EmptyState(
                title = "טעינת הרשימות נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadLookups() } },
            )
            ui.lookups.isEmpty -> LoadingBlock("טוען רשימות…")
            else -> {
                if (editing) {
                    FormCheckbox(
                        label = EVENT_CANCELLED_LABEL,
                        checked = isCancelled,
                        enabled = !isCancelled || ui.canManageUnit,
                        onCheckedChange = { next ->
                            formError = canToggleEventCancelled(next, ui.canManageUnit)
                            if (formError == null) isCancelled = next
                        },
                    )
                }
                ReturnDateField(
                    label = "תאריך",
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                errors.eventDate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                FormFieldRow {
                    FormField(
                        label = "מספר אירוע",
                        value = policeEventId,
                        onValueChange = { policeEventId = it },
                        keyboardType = KeyboardType.Number,
                        mono = true,
                        ltr = true,
                        modifier = Modifier.weight(1f),
                    )
                    FormField(
                        label = EVENT_PATROL_CALLSIGN_LABEL,
                        value = patrolCallsign,
                        onValueChange = { patrolCallsign = it },
                        keyboardType = KeyboardType.Number,
                        mono = true,
                        ltr = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                FormFieldRow {
                    LookupPickerField(
                        label = "סוג אירוע",
                        options = ui.lookups.eventTypes,
                        selectedId = eventTypeId,
                        onSelect = { eventTypeId = it },
                        placeholder = "בחירת סוג",
                        searchPlaceholder = "חיפוש סוג אירוע",
                        error = errors.eventType,
                        modifier = Modifier.weight(1f),
                    )
                    LookupPickerField(
                        label = "שלוחה",
                        options = ui.lookups.districts,
                        selectedId = districtId,
                        onSelect = { next ->
                            roadId = applyDistrictRoadDefault(
                                previousDistrictId = districtId,
                                nextDistrictId = next,
                                districts = ui.lookups.districts,
                                roads = ui.lookups.roads,
                                currentRoadId = roadId,
                            )
                            districtId = next
                        },
                        placeholder = "בחירת שלוחה",
                        searchPlaceholder = "חיפוש שלוחה",
                        allowClear = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                FormFieldRow {
                    LookupPickerField(
                        label = "כביש",
                        options = ui.lookups.roads,
                        selectedId = roadId,
                        onSelect = { roadId = it },
                        placeholder = "בחירת כביש",
                        searchPlaceholder = "חיפוש כביש",
                        error = errors.road,
                        modifier = Modifier.weight(1f),
                    )
                    FormField(
                        label = "מיקום",
                        value = location,
                        onValueChange = { location = it },
                        placeholder = "למשל: מחלף שורק",
                        error = errors.location,
                        modifier = Modifier.weight(1f),
                    )
                }
                CrewAssignmentSection(
                    assignOpenLabel = EVENT_ASSIGN_OPEN,
                    assignCloseLabel = EVENT_ASSIGN_CLOSE,
                    profiles = ui.assignableProfiles,
                    selectedIds = responders.map { it.responderId },
                    onAdd = { id ->
                        if (!isSelfAssignDisabledOnCreate(!editing, ui.userId, id)) {
                            responders = toggleEventResponder(
                                responders,
                                id,
                                hasVehicle = vehicleOwnerIds.contains(id),
                            )
                            detailResponderId = id
                        }
                    },
                    disabledIds = if (!editing) setOfNotNull(ui.userId) else emptySet(),
                    disabledHint = EVENT_SELF_ASSIGN_DISABLED_HINT,
                    onRemove = { id ->
                        responders = toggleEventResponder(responders, id)
                        if (detailResponderId == id) detailResponderId = null
                    },
                    onResponderClick = { detailResponderId = it },
                    caption = eventDraftSummary(responders.size),
                    emptyHint = EVENT_ASSIGN_EMPTY,
                    emptyRoster = "אין משתמשים פעילים להקצאה.",
                    emptyQuery = "לא נמצאו מתנדבים להקצאה",
                )
                FormArea(
                    label = "הערות",
                    value = notes,
                    onValueChange = { notes = it },
                    minHeight = 96,
                )
                formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = EVENT_SAVE_TITLE,
                    busy = saving,
                    onClick = { persist(allowPartial = false) },
                )
                GhostButton(
                    title = EVENT_SAVE_DRAFT_TITLE,
                    enabled = !saving,
                    onClick = { persist(allowPartial = true) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (foreignEditPending) {
        ModalBottomSheet(onDismissRequest = onBack) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    foreignEventEditTitle(shiftLeadName),
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(FOREIGN_EVENT_EDIT_BODY, style = TypeScale.body, color = FieldTheme.textSecondary)
                PrimaryButton(
                    title = FOREIGN_EVENT_EDIT_CONFIRM,
                    onClick = { foreignEditAcked = true },
                )
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
                    Text(FOREIGN_EVENT_EDIT_CANCEL, color = FieldTheme.accent)
                }
            }
        }
    }

    if (detailResponder != null && detailProfile != null) {
        ModalBottomSheet(onDismissRequest = { detailResponderId = null }) {
            EventResponderDetailSheet(
                profile = detailProfile,
                responder = detailResponder,
                vehicleKinds = ui.lookups.vehicleKinds,
                busLane = busLane,
                onToggleBusLane = { busLane = it },
                onDismiss = { detailResponderId = null },
                onChange = { updated ->
                    responders = updateEventResponder(responders, updated.responderId) { updated }
                },
            )
        }
    }
}

@Composable
private fun EventResponderDetailSheet(
    profile: AssignableProfile,
    responder: EventResponderDraft,
    vehicleKinds: List<LookupOption>,
    busLane: Boolean,
    onToggleBusLane: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onChange: (EventResponderDraft) -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(profile.display, style = TypeScale.section, color = FieldTheme.textPrimary)
        FormFieldRow {
            TimeField(
                label = "שעת התחלה",
                value = responder.startTime,
                onValueChange = { onChange(responder.copy(startTime = it)) },
                placeholder = "08:00",
                modifier = Modifier.weight(1f),
            )
            TimeField(
                label = "שעת סיום",
                value = responder.endTime,
                onValueChange = { onChange(responder.copy(endTime = it)) },
                placeholder = "09:30",
                modifier = Modifier.weight(1f),
            )
        }
        FormField(
            label = "קילומטרים",
            value = if (responder.hasVehicle) responder.totalKm else "",
            onValueChange = { onChange(responder.copy(totalKm = it)) },
            keyboardType = KeyboardType.Decimal,
            mono = true,
            ltr = responder.hasVehicle,
            enabled = responder.hasVehicle,
            placeholder = if (responder.hasVehicle) null else NO_VEHICLE_KM_PLACEHOLDER,
            textAlignEnd = !responder.hasVehicle,
        )
        FormCheckbox(
            label = "אמצעים",
            checked = responder.emergencyMeans,
            onCheckedChange = { onChange(responder.copy(emergencyMeans = it)) },
        )
        FormCheckbox(
            label = "נת״צ",
            checked = busLane,
            onCheckedChange = onToggleBusLane,
        )
        Text("רכבים שטופלו", style = TypeScale.label, color = FieldTheme.textSecondary)
        if (vehicleKinds.isEmpty()) {
            Text(
                "אין סוגי רכב ברשימה הסגורה.",
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicleKinds.forEach { kind ->
                    TreatedVehicleStepper(
                        label = kind.name,
                        value = treatedQuantity(responder, kind.id),
                        onDelta = { delta ->
                            onChange(
                                bumpTreatedVehicle(
                                    listOf(responder),
                                    responder.responderId,
                                    kind.id,
                                    delta,
                                ).first(),
                            )
                        },
                    )
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("סגירה", color = FieldTheme.accent)
        }
    }
}

@Composable
private fun TreatedVehicleStepper(
    label: String,
    value: Int,
    onDelta: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FormControlHeight)
            .background(FieldTheme.raised, shape)
            .border(1.dp, FieldTheme.hairline, shape)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TypeScale.body,
            color = FieldTheme.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StepperButton(label = "−", enabled = value > 0) { onDelta(-1) }
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value.toString(),
                    style = TypeScale.numeric,
                    color = FieldTheme.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            StepperButton(label = "+", enabled = true) { onDelta(1) }
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TypeScale.section,
            color = if (enabled) FieldTheme.accent else FieldTheme.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}
