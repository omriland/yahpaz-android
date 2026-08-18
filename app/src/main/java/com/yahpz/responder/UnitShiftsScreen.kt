package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yahpz.domain.SHIFT_KIND_LABELS
import com.yahpz.domain.UNIT_SHIFTS_LOAD_FAILED
import com.yahpz.domain.VEHICLE_TYPE_LABELS
import com.yahpz.domain.eventStamp
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatPlate
import com.yahpz.domain.hebrewWeekdayLetter
import com.yahpz.domain.shiftStamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitShiftsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<ShiftListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.unitShifts.isEmpty()) app.reloadUnitShifts()
    }

    val trimmed = query.trim()
    val shifts = if (trimmed.isEmpty()) {
        ui.unitShifts
    } else {
        ui.unitShifts.filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
    }.sortedByDescending { it.shiftDate }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadUnitShifts()
                refreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("משמרות היחידה", style = TypeScale.title, color = FieldTheme.textPrimary)
                TextButton(onClick = { app.setToolsDestination(ToolsDestination.NEW_SHIFT) }) {
                    Text(NEW_SHIFT_TITLE, style = TypeScale.bodyStrong, color = FieldTheme.accent)
                }
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "חיפוש לפי תאריך, סוג משמרת או אחמ״ש",
            )
            when {
                ui.unitShiftsFailed -> EmptyState(
                    title = UNIT_SHIFTS_LOAD_FAILED,
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadUnitShifts() } },
                )
                ui.unitShiftsLoading && ui.unitShifts.isEmpty() -> LoadingBlock("טוען משמרות יחידה…")
                shifts.isEmpty() -> EmptyState(
                    title = if (trimmed.isEmpty()) "אין משמרות להצגה" else "לא נמצאו משמרות תואמות",
                    actionTitle = if (trimmed.isEmpty()) null else "ניקוי חיפוש",
                    onAction = if (trimmed.isEmpty()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${shifts.size} משמרות אחרונות ביחידה",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                    shifts.forEach { shift -> UnitShiftRow(shift) { detail = shift } }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    detail?.let { shift ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("פרטי המשמרת", style = TypeScale.section, color = FieldTheme.textPrimary)
                LedgerRow("תאריך", "${formatDate(shift.shiftDate)} (${hebrewWeekdayLetter(shift.shiftDate)})")
                LedgerRow("שם משמרת", SHIFT_KIND_LABELS[shift.shiftKind] ?: shift.shiftKind)
                LedgerRow("סוג רכב", VEHICLE_TYPE_LABELS[shift.vehicleType] ?: shift.vehicleType)
                if (shift.vehicleType == "personal") {
                    LedgerRow("לוחית", formatPlate(shift.personalVehicle?.plateNumber.orEmpty()))
                }
                LedgerRow("אחמ״ש", shift.shiftLead?.display.orEmpty())
                LedgerRow("כוננים", "${shift.responders.size}")
                LedgerRow("אירועים", "${shift.bornEvents.size}")
                if (shift.bornEvents.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("אירועי המשמרת", style = TypeScale.section, color = FieldTheme.textPrimary)
                    shift.bornEvents.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { app.openFill(event.id) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    event.eventType?.name ?: "אירוע",
                                    style = TypeScale.bodyStrong,
                                    color = FieldTheme.textPrimary,
                                )
                                Text(
                                    event.policeEventId ?: formatDate(event.eventDate),
                                    style = TypeScale.caption,
                                    color = FieldTheme.textMuted,
                                )
                            }
                            StampChip(eventStamp(event.status))
                        }
                    }
                }
                if (ui.canManageUnit) {
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        title = "עריכה",
                        onClick = {
                            val id = shift.id
                            detail = null
                            app.openEditShift(id)
                        },
                    )
                }
                TextButton(onClick = { detail = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun UnitShiftRow(shift: ShiftListItem, onOpen: () -> Unit) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(shift.title, style = TypeScale.section, color = FieldTheme.textPrimary)
                Text(
                    "${formatDate(shift.shiftDate)} (${hebrewWeekdayLetter(shift.shiftDate)})",
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
                shift.shiftLead?.display?.let { lead ->
                    Text("אחמ״ש: $lead", style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            StampChip(shiftStamp(shift.status))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${shift.responders.size} כוננים · ${shift.bornEvents.size} אירועים",
            style = TypeScale.caption,
            color = FieldTheme.textSecondary,
        )
    }
}
