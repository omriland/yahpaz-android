package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EVENT_ASSIGN_CLOSE
import com.yahpz.domain.EVENT_ASSIGN_EMPTY
import com.yahpz.domain.EVENT_ASSIGN_OPEN
import com.yahpz.domain.EVENT_CANCELLED_LABEL
import com.yahpz.domain.EVENT_EDIT_LOAD_FAILED
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.EVENT_NEW_TITLE
import com.yahpz.domain.EVENT_SAVE_AND_NEW_TITLE
import com.yahpz.domain.EVENT_SAVE_TITLE
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventDraftErrors
import com.yahpz.domain.applyDistrictRoadDefault
import com.yahpz.domain.canToggleEventCancelled
import com.yahpz.domain.eventDraftSummary
import com.yahpz.domain.israelToday
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.toggleEventResponder
import com.yahpz.domain.validateEventDraft
import kotlinx.coroutines.launch

const val NEW_EVENT_TITLE = EVENT_NEW_TITLE

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
    var eventTypeId by remember { mutableStateOf("") }
    var roadId by remember { mutableStateOf("") }
    var districtId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var responderIds by remember { mutableStateOf(emptyList<String>()) }
    var isCancelled by remember { mutableStateOf(false) }
    var previousIsCancelled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(!editing) }
    var loadFailed by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(EventDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.lookups.isEmpty && !ui.lookupsLoading) app.reloadLookups()
    }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect
        loaded = false
        loadFailed = false
        try {
            val detail = YahpazAPI.fetchEventFormDetail(eventId)
            val draft = detail.toDraft()
            eventDate = draft.eventDate
            policeEventId = draft.policeEventId
            eventTypeId = draft.eventTypeId
            roadId = draft.roadId
            districtId = draft.districtId
            location = draft.location
            notes = draft.notes
            responderIds = draft.responderIds
            isCancelled = draft.isCancelled
            previousIsCancelled = draft.isCancelled
            loaded = true
        } catch (_: Exception) {
            loadFailed = true
            loaded = true
        }
    }

    fun draft() = EventDraft(
        eventDate = eventDate,
        policeEventId = policeEventId,
        eventTypeId = eventTypeId,
        roadId = roadId,
        districtId = districtId,
        location = location,
        notes = notes,
        responderIds = responderIds,
        isCancelled = isCancelled,
    )

    fun resetCreateForm() {
        eventDate = returnDateToInput(israelToday())
        policeEventId = ""
        eventTypeId = ""
        roadId = ""
        districtId = ""
        location = ""
        notes = ""
        responderIds = emptyList()
        isCancelled = false
        previousIsCancelled = false
        errors = EventDraftErrors()
        formError = null
    }

    fun persist(createNew: Boolean) {
        val current = draft()
        val next = validateEventDraft(current, ui.lookups.districts)
        errors = next
        if (!next.isEmpty) {
            formError = next.formMessage
            return
        }
        formError = null
        scope.launch {
            saving = true
            formError = if (eventId != null) {
                app.updateUnitEvent(eventId, current, previousIsCancelled, stayOnForm = createNew)
            } else {
                app.createUnitEvent(current, stayOnForm = createNew)
            }
            if (formError == null && createNew) {
                if (eventId != null) {
                    app.setToolsDestination(ToolsDestination.NEW_EVENT)
                } else {
                    resetCreateForm()
                }
            }
            saving = false
        }
    }

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
            ui.lookupsFailed && ui.lookups.isEmpty -> EmptyState(
                title = "טעינת הרשימות נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadLookups() } },
            )
            ui.lookups.isEmpty -> LoadingBlock("טוען רשימות…")
            else -> {
                FormCheckbox(
                    label = EVENT_CANCELLED_LABEL,
                    checked = isCancelled,
                    enabled = !isCancelled || ui.canAdmin,
                    onCheckedChange = { next ->
                        formError = canToggleEventCancelled(next, ui.canAdmin)
                        if (formError == null) isCancelled = next
                    },
                )
                ReturnDateField(
                    label = "תאריך",
                    value = eventDate,
                    onValueChange = { eventDate = it },
                )
                errors.eventDate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                FormField(
                    label = "מספר אירוע",
                    value = policeEventId,
                    onValueChange = { policeEventId = it },
                    keyboardType = KeyboardType.Number,
                    mono = true,
                    ltr = true,
                )
                LookupPickerField(
                    label = "סוג אירוע",
                    options = ui.lookups.eventTypes,
                    selectedId = eventTypeId,
                    onSelect = { eventTypeId = it },
                    placeholder = "בחירת סוג אירוע",
                    searchPlaceholder = "חיפוש סוג אירוע",
                    error = errors.eventType,
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
                )
                LookupPickerField(
                    label = "כביש",
                    options = ui.lookups.roads,
                    selectedId = roadId,
                    onSelect = { roadId = it },
                    placeholder = "בחירת כביש",
                    searchPlaceholder = "חיפוש כביש",
                    error = errors.road,
                )
                FormField(
                    label = "מיקום",
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "למשל: מחלף שורק, לכיוון צפון",
                    error = errors.location,
                )
                CrewAssignmentSection(
                    assignOpenLabel = EVENT_ASSIGN_OPEN,
                    assignCloseLabel = EVENT_ASSIGN_CLOSE,
                    profiles = ui.assignableProfiles,
                    selectedIds = responderIds,
                    onAdd = { responderIds = toggleEventResponder(responderIds, it) },
                    onRemove = { responderIds = toggleEventResponder(responderIds, it) },
                    caption = eventDraftSummary(responderIds.size),
                    emptyHint = EVENT_ASSIGN_EMPTY,
                    emptyRoster = "אין משתמשים פעילים להקצאה.",
                    emptyQuery = "לא נמצאו כוננים להקצאה",
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
                    onClick = { persist(createNew = false) },
                )
                GhostButton(
                    title = EVENT_SAVE_AND_NEW_TITLE,
                    enabled = !saving,
                    onClick = { persist(createNew = true) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
