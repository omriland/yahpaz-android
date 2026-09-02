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
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EVENT_ASSIGN_REMOVE
import com.yahpz.domain.LookupOption
import com.yahpz.domain.SHIFT_ASSIGN_CLOSE
import com.yahpz.domain.SHIFT_ASSIGN_EMPTY
import com.yahpz.domain.SHIFT_ASSIGN_OPEN
import com.yahpz.domain.SHIFT_EDIT_LOAD_FAILED
import com.yahpz.domain.SHIFT_EDIT_TITLE
import com.yahpz.domain.SHIFT_KIND_ORDER
import com.yahpz.domain.SHIFT_NEW_TITLE
import com.yahpz.domain.SHIFT_SAVE_TITLE
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.ShiftDraftErrors
import com.yahpz.domain.crewVehicleLabel
import com.yahpz.domain.israelToday
import com.yahpz.domain.keepPersonalVehicleId
import com.yahpz.domain.offeredShiftVehicleTypes
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.shiftCrewSummary
import com.yahpz.domain.shiftKindLabel
import com.yahpz.domain.shiftVehicleTypeLabel
import com.yahpz.domain.toggleCrewSelection
import com.yahpz.domain.validateShiftDraft
import kotlinx.coroutines.launch

const val NEW_SHIFT_TITLE = SHIFT_NEW_TITLE

@Composable
fun ShiftFormScreen(
    app: AppModel,
    ui: AppUiState,
    onBack: () -> Unit,
    shiftId: String? = null,
) {
    val editing = shiftId != null
    val scope = rememberCoroutineScope()
    var shiftDate by remember { mutableStateOf(returnDateToInput(israelToday())) }
    var shiftKind by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var personalVehicleId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var responderIds by remember { mutableStateOf(emptyList<String>()) }
    var crewVehicles by remember { mutableStateOf(emptyList<CrewVehicleRow>()) }
    var vehiclesLoaded by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(!editing) }
    var loadFailed by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(ShiftDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.assignableProfiles.isEmpty() && !ui.lookupsLoading) app.reloadLookups()
    }

    LaunchedEffect(shiftId) {
        if (shiftId == null) return@LaunchedEffect
        loaded = false
        loadFailed = false
        try {
            val detail = YahpazAPI.fetchShiftFormDetail(shiftId)
            val draft = detail.toDraft()
            shiftDate = draft.shiftDate
            shiftKind = draft.shiftKind
            vehicleType = draft.vehicleType
            personalVehicleId = draft.personalVehicleId
            notes = draft.notes
            responderIds = draft.responderIds
            loaded = true
        } catch (_: Exception) {
            loadFailed = true
            loaded = true
        }
    }

    LaunchedEffect(responderIds) {
        if (responderIds.isEmpty()) {
            crewVehicles = emptyList()
            vehiclesLoaded = true
            personalVehicleId = null
            if (vehicleType == "personal") vehicleType = ""
            return@LaunchedEffect
        }
        vehiclesLoaded = false
        try {
            val rows = YahpazAPI.fetchVehiclesForResponders(responderIds)
            crewVehicles = rows
            vehiclesLoaded = true
            val kept = keepPersonalVehicleId(personalVehicleId, rows.map { it.id }.toSet())
            personalVehicleId = kept
            if (vehicleType == "personal" && rows.isEmpty()) vehicleType = ""
        } catch (_: Exception) {
            crewVehicles = emptyList()
            vehiclesLoaded = false
        }
    }

    val includePersonal = vehiclesLoaded && crewVehicles.isNotEmpty()
    val vehicleOptions = remember(vehicleType, includePersonal) {
        val base = offeredShiftVehicleTypes(includePersonal).map { it to shiftVehicleTypeLabel(it) }
        if (vehicleType.isNotEmpty() && base.none { it.first == vehicleType }) {
            base + (vehicleType to shiftVehicleTypeLabel(vehicleType))
        } else {
            base
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
        ToolsBackRow(if (editing) SHIFT_EDIT_TITLE else SHIFT_NEW_TITLE, onBack)
        when {
            loadFailed -> EmptyState(
                title = SHIFT_EDIT_LOAD_FAILED,
                actionTitle = "חזרה",
                onAction = onBack,
            )
            editing && !loaded -> LoadingBlock("טוען משמרת…")
            ui.lookupsFailed && ui.assignableProfiles.isEmpty() -> EmptyState(
                title = "טעינת רשימת המתנדבים נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadLookups() } },
            )
            ui.assignableProfiles.isEmpty() -> LoadingBlock("טוען מתנדבים…")
            else -> {
                ReturnDateField(
                    label = "תאריך",
                    value = shiftDate,
                    onValueChange = { shiftDate = it },
                )
                errors.shiftDate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                OptionRowSelector(
                    label = "שם משמרת",
                    options = SHIFT_KIND_ORDER.map { it to shiftKindLabel(it) },
                    selected = shiftKind,
                    onSelect = { shiftKind = it },
                    error = errors.shiftKind,
                )
                OptionRowSelector(
                    label = "סוג רכב",
                    options = vehicleOptions,
                    selected = vehicleType,
                    onSelect = {
                        vehicleType = it
                        if (it != "personal") personalVehicleId = null
                    },
                    error = errors.vehicleType,
                )
                if (vehicleType == "personal" && includePersonal) {
                    LookupPickerField(
                        label = "לוחית",
                        options = crewVehicles.map {
                            LookupOption(
                                id = it.id,
                                name = crewVehicleLabel(it.plateNumber, it.model),
                            )
                        },
                        selectedId = personalVehicleId.orEmpty(),
                        onSelect = { personalVehicleId = it.ifEmpty { null } },
                        placeholder = if (responderIds.isEmpty()) {
                            "יש לשבץ מתנדבים תחילה"
                        } else {
                            "בחירת לוחית"
                        },
                        searchPlaceholder = "חיפוש לוחית",
                        error = errors.plate,
                    )
                }
                CrewAssignmentSection(
                    assignOpenLabel = SHIFT_ASSIGN_OPEN,
                    assignCloseLabel = SHIFT_ASSIGN_CLOSE,
                    profiles = ui.assignableProfiles,
                    selectedIds = responderIds,
                    onAdd = { responderIds = toggleCrewSelection(responderIds, it) },
                    onRemove = { responderIds = toggleCrewSelection(responderIds, it) },
                    caption = shiftCrewSummary(responderIds.size),
                    emptyHint = SHIFT_ASSIGN_EMPTY,
                    emptyRoster = "אין משתמשים פעילים לשיבוץ.",
                    emptyQuery = "לא נמצאו מתנדבים לשיבוץ",
                    error = errors.crew,
                    removeLabel = EVENT_ASSIGN_REMOVE,
                )
                FormArea(
                    label = "הערות כלליות",
                    value = notes,
                    onValueChange = { notes = it },
                    minHeight = 96,
                )
                formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = SHIFT_SAVE_TITLE,
                    busy = saving,
                    onClick = {
                        val draft = ShiftDraft(
                            shiftDate = shiftDate,
                            shiftKind = shiftKind,
                            vehicleType = vehicleType,
                            notes = notes,
                            responderIds = responderIds,
                            personalVehicleId = personalVehicleId,
                        )
                        val next = validateShiftDraft(draft)
                        errors = next
                        if (!next.isEmpty) {
                            formError = next.formMessage
                            return@PrimaryButton
                        }
                        formError = null
                        scope.launch {
                            saving = true
                            formError = if (shiftId != null) {
                                app.updateUnitShift(shiftId, draft)
                            } else {
                                app.createUnitShift(draft)
                            }
                            saving = false
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
