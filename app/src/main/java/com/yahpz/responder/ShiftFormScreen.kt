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
import com.yahpz.domain.SHIFT_KIND_ORDER
import com.yahpz.domain.SHIFT_VEHICLE_TYPE_ORDER
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.ShiftDraftErrors
import com.yahpz.domain.israelToday
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.shiftCrewSummary
import com.yahpz.domain.shiftKindLabel
import com.yahpz.domain.shiftVehicleTypeLabel
import com.yahpz.domain.toggleCrewSelection
import com.yahpz.domain.validateShiftDraft
import kotlinx.coroutines.launch

const val NEW_SHIFT_TITLE = "משמרת חדשה"

@Composable
fun ShiftFormScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var shiftDate by remember { mutableStateOf(returnDateToInput(israelToday())) }
    var shiftKind by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var responderIds by remember { mutableStateOf(emptyList<String>()) }
    var errors by remember { mutableStateOf(ShiftDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.assignableProfiles.isEmpty() && !ui.lookupsLoading) app.reloadLookups()
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
        ToolsBackRow(NEW_SHIFT_TITLE, onBack)
        when {
            ui.lookupsFailed && ui.assignableProfiles.isEmpty() -> EmptyState(
                title = "טעינת רשימת הכוננים נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadLookups() } },
            )
            ui.assignableProfiles.isEmpty() -> LoadingBlock("טוען כוננים…")
            else -> {
                ReturnDateField(
                    label = "תאריך המשמרת",
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
                    label = "רכב",
                    options = SHIFT_VEHICLE_TYPE_ORDER.map { it to shiftVehicleTypeLabel(it) },
                    selected = vehicleType,
                    onSelect = { vehicleType = it },
                    error = errors.vehicleType,
                )
                CrewPickerField(
                    label = "כוננים",
                    profiles = ui.assignableProfiles,
                    selectedIds = responderIds,
                    onToggle = { responderIds = toggleCrewSelection(responderIds, it) },
                    placeholder = "שיבוץ כוננים",
                    caption = shiftCrewSummary(responderIds.size),
                    error = errors.crew,
                )
                FormArea(
                    label = "הערות (לא חובה)",
                    value = notes,
                    onValueChange = { notes = it },
                    minHeight = 96,
                )
                formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = "שמירת המשמרת",
                    busy = saving,
                    onClick = {
                        val draft = ShiftDraft(
                            shiftDate = shiftDate,
                            shiftKind = shiftKind,
                            vehicleType = vehicleType,
                            notes = notes,
                            responderIds = responderIds,
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
                            formError = app.createUnitShift(draft)
                            saving = false
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
