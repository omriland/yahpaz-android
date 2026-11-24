package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.yahpz.domain.SHIFT_END_TIME_LABEL
import com.yahpz.domain.SHIFT_START_TIME_LABEL
import com.yahpz.domain.SHIFT_TIME_PICKER_CANCEL
import com.yahpz.domain.SHIFT_TIME_PICKER_CONFIRM
import com.yahpz.domain.SHIFT_TIME_PLACEHOLDER
import com.yahpz.domain.ShiftHours
import com.yahpz.domain.hasStashableInput
import com.yahpz.domain.hoursAfterKindChange
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
import com.yahpz.domain.SHIFT_DRAFT_WAITING_FOR_REQUIRED
import com.yahpz.domain.clearedFor
import kotlinx.coroutines.launch
import java.util.Locale

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
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var isLegacy by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var responderIds by remember { mutableStateOf(emptyList<String>()) }
    var crewVehicles by remember { mutableStateOf(emptyList<CrewVehicleRow>()) }
    var vehiclesLoaded by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(!editing) }
    var loadFailed by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(ShiftDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    /** The draft the errors above were raised for; edits since then clear what they touched. */
    var errorsFor by remember { mutableStateOf<ShiftDraft?>(null) }
    var autoSaveError by remember { mutableStateOf<String?>(null) }
    var autoSaveFailedFor by remember { mutableStateOf<ShiftDraft?>(null) }
    var saving by remember { mutableStateOf(false) }
    var quietSaving by remember { mutableStateOf(false) }
    var persistedDraft by remember { mutableStateOf<ShiftDraft?>(null) }

    fun currentDraft() = ShiftDraft(
        shiftDate = shiftDate,
        shiftKind = shiftKind,
        vehicleType = vehicleType,
        notes = notes,
        responderIds = responderIds,
        personalVehicleId = personalVehicleId,
        startTime = startTime,
        endTime = endTime,
        isLegacy = isLegacy,
    )

    LaunchedEffect(ui.userId) {
        if (ui.assignableProfiles.isEmpty() && !ui.lookupsLoading) app.reloadLookups()
    }

    LaunchedEffect(Unit) {
        if (!editing) {
            val stashed = ShiftDraftStore.read()
            if (stashed != null) {
                shiftDate = stashed.draft.shiftDate.ifBlank { shiftDate }
                shiftKind = stashed.draft.shiftKind
                vehicleType = stashed.draft.vehicleType
                personalVehicleId = stashed.draft.personalVehicleId
                startTime = stashed.draft.startTime
                endTime = stashed.draft.endTime
                notes = stashed.draft.notes
                responderIds = stashed.draft.responderIds
            }
        }
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
            startTime = draft.startTime
            endTime = draft.endTime
            isLegacy = draft.isLegacy
            notes = draft.notes
            responderIds = draft.responderIds
            persistedDraft = draft
            loaded = true
        } catch (_: Exception) {
            loadFailed = true
            loaded = true
        }
    }

    val current = currentDraft()

    // For new shifts: auto-stash locally on change
    LaunchedEffect(current) {
        if (!editing) {
            if (current.hasStashableInput()) {
                kotlinx.coroutines.delay(600)
                ShiftDraftStore.stash(current)
            }
        }
    }

    // For editing shifts: auto-save to server debounced when valid and changed
    LaunchedEffect(current, loaded) {
        if (!editing || !loaded) return@LaunchedEffect
        val stored = persistedDraft ?: return@LaunchedEffect
        if (stored == current) return@LaunchedEffect
        val validation = validateShiftDraft(current)
        if (!validation.isEmpty) return@LaunchedEffect
        kotlinx.coroutines.delay(800)
        quietSaving = true
        val error = app.autoSaveUnitShift(shiftId, current)
        quietSaving = false
        if (error == null) {
            persistedDraft = current
            autoSaveError = null
        } else {
            autoSaveError = error
            autoSaveFailedFor = current
        }
    }

    androidx.compose.runtime.DisposableEffect(shiftId) {
        onDispose {
            if (!editing) {
                val draft = currentDraft()
                if (draft.hasStashableInput()) {
                    ShiftDraftStore.stash(draft)
                }
            } else if (loaded) {
                val draft = currentDraft()
                if (draft != persistedDraft) {
                    app.flushShiftForm(shiftId, draft)
                }
            }
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

    val shownErrors = errorsFor?.let { errors.clearedFor(it, current) } ?: errors
    val shownFormError = formError?.takeIf { errorsFor == null || errorsFor == current }

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
            .imePadding()
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
                shownErrors.shiftDate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                OptionRowSelector(
                    label = "שם משמרת",
                    options = SHIFT_KIND_ORDER.map { it to shiftKindLabel(it) },
                    selected = shiftKind,
                    onSelect = {
                        val previousKind = shiftKind
                        shiftKind = it
                        if (!isLegacy) {
                            val hours = hoursAfterKindChange(ShiftHours(startTime, endTime), previousKind, it)
                            startTime = hours.start
                            endTime = hours.end
                        }
                    },
                    error = shownErrors.shiftKind,
                )
                if (!isLegacy) {
                    ShiftHoursRow(
                        startTime = startTime,
                        endTime = endTime,
                        canEdit = ui.canManageUnit,
                        onStartChange = { startTime = it },
                        onEndChange = { endTime = it },
                        startError = shownErrors.startTime,
                        endError = shownErrors.endTime,
                    )
                }
                OptionRowSelector(
                    label = "סוג רכב",
                    options = vehicleOptions,
                    selected = vehicleType,
                    onSelect = {
                        vehicleType = it
                        if (it != "personal") personalVehicleId = null
                    },
                    error = shownErrors.vehicleType,
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
                        error = shownErrors.plate,
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
                    error = shownErrors.crew,
                    removeLabel = EVENT_ASSIGN_REMOVE,
                )
                FormArea(
                    label = "הערות כלליות",
                    value = notes,
                    onValueChange = { notes = it },
                    minHeight = 96,
                )
                shownFormError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                if (shownFormError == null) {
                    if (editing && loaded) {
                        val pending = persistedDraft != current
                        val failed = autoSaveError?.takeIf { autoSaveFailedFor == current && !quietSaving }
                        Text(
                            when {
                                failed != null -> failed
                                saving || quietSaving -> "שומר…"
                                pending && !validateShiftDraft(current).isEmpty -> SHIFT_DRAFT_WAITING_FOR_REQUIRED
                                pending -> "שומר…"
                                else -> "השינויים נשמרים אוטומטית."
                            },
                            style = TypeScale.caption,
                            color = if (failed != null) FieldTheme.alert else FieldTheme.textMuted,
                        )
                    } else if (!editing && (shiftKind.isNotBlank() || vehicleType.isNotBlank() || notes.isNotBlank() || responderIds.isNotEmpty())) {
                        Text(
                            "הטיוטה נשמרת אוטומטית במכשיר.",
                            style = TypeScale.caption,
                            color = FieldTheme.textMuted,
                        )
                    }
                }
                PrimaryButton(
                    title = SHIFT_SAVE_TITLE,
                    busy = saving,
                    onClick = {
                        val draft = currentDraft()
                        val next = validateShiftDraft(draft)
                        errors = next
                        errorsFor = draft
                        if (!next.isEmpty) {
                            formError = next.formMessage
                            return@PrimaryButton
                        }
                        formError = null
                        scope.launch {
                            saving = true
                            val err = if (shiftId != null) {
                                app.updateUnitShift(shiftId, draft)
                            } else {
                                val createdErr = app.createUnitShift(draft)
                                if (createdErr == null) ShiftDraftStore.clear()
                                createdErr
                            }
                            formError = err
                            saving = false
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val timeFieldShape = RoundedCornerShape(4.dp)

@Composable
private fun ShiftHoursRow(
    startTime: String,
    endTime: String,
    canEdit: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    startError: String?,
    endError: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShiftTimeField(SHIFT_START_TIME_LABEL, startTime, canEdit, onStartChange, Modifier.weight(1f))
            ShiftTimeField(SHIFT_END_TIME_LABEL, endTime, canEdit, onEndChange, Modifier.weight(1f))
        }
        listOfNotNull(startError, endError).distinct().forEach { message ->
            Text(message, style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

@Composable
private fun ShiftTimeField(
    label: String,
    value: String,
    canEdit: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(FormControlHeight)
                .background(FieldTheme.raised, timeFieldShape)
                .border(1.dp, FieldTheme.strong, timeFieldShape)
                .then(
                    if (canEdit) {
                        Modifier.clickable(role = Role.Button, onClickLabel = label) { showPicker = true }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = value.ifEmpty { SHIFT_TIME_PLACEHOLDER },
                style = TypeScale.numeric.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                color = if (value.isEmpty()) FieldTheme.textMuted else FieldTheme.textPrimary,
            )
        }
    }
    if (showPicker) {
        ShiftTimePickerDialog(
            initial = value,
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftTimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.substringBefore(":").toIntOrNull() ?: 0,
        initialMinute = initial.substringAfter(":", "").toIntOrNull() ?: 0,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(String.format(Locale.ROOT, "%02d:%02d", state.hour, state.minute)) }) {
                Text(SHIFT_TIME_PICKER_CONFIRM, color = FieldTheme.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(SHIFT_TIME_PICKER_CANCEL, color = FieldTheme.accent)
            }
        },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TimePicker(state = state)
            }
        },
    )
}
