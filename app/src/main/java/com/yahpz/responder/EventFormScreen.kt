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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventDraftErrors
import com.yahpz.domain.applyDistrictRoadDefault
import com.yahpz.domain.districtNeedsLocation
import com.yahpz.domain.eventDraftSummary
import com.yahpz.domain.israelToday
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.toggleEventResponder
import com.yahpz.domain.validateEventDraft
import kotlinx.coroutines.launch

const val NEW_EVENT_TITLE = "אירוע חדש"

@Composable
fun EventFormScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var eventDate by remember { mutableStateOf(returnDateToInput(israelToday())) }
    var policeEventId by remember { mutableStateOf("") }
    var eventTypeId by remember { mutableStateOf("") }
    var roadId by remember { mutableStateOf("") }
    var districtId by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var responderIds by remember { mutableStateOf(emptyList<String>()) }
    var errors by remember { mutableStateOf(EventDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.lookups.isEmpty && !ui.lookupsLoading) app.reloadLookups()
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
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolsBackRow(NEW_EVENT_TITLE, onBack)
        when {
            ui.lookupsFailed && ui.lookups.isEmpty -> EmptyState(
                title = "טעינת הרשימות נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadLookups() } },
            )
            ui.lookups.isEmpty -> LoadingBlock("טוען רשימות…")
            else -> {
                ReturnDateField(
                    label = "תאריך האירוע",
                    value = eventDate,
                    onValueChange = { eventDate = it },
                )
                errors.eventDate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                FormField(
                    label = "מספר אירוע (לא חובה)",
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
                    label = "שלוחה (לא חובה)",
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
                    label = if (districtNeedsLocation(ui.lookups.districts, districtId)) {
                        "מיקום"
                    } else {
                        "מיקום (לא חובה)"
                    },
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "צומת, ק״מ או תיאור",
                    error = errors.location,
                )
                CrewPickerField(
                    label = "כוננים",
                    profiles = ui.assignableProfiles,
                    selectedIds = responderIds,
                    onToggle = { responderIds = toggleEventResponder(responderIds, it) },
                    placeholder = "שיבוץ כוננים",
                    caption = eventDraftSummary(responderIds.size),
                )
                FormArea(
                    label = "הערות (לא חובה)",
                    value = notes,
                    onValueChange = { notes = it },
                    minHeight = 96,
                )
                formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = "שמירת האירוע",
                    busy = saving,
                    onClick = {
                        val current = draft()
                        val next = validateEventDraft(current, ui.lookups.districts)
                        errors = next
                        if (!next.isEmpty) {
                            formError = next.formMessage
                            return@PrimaryButton
                        }
                        formError = null
                        scope.launch {
                            saving = true
                            formError = app.createUnitEvent(current)
                            saving = false
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
