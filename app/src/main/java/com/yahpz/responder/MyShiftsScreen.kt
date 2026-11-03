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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.MINE_SHIFTS_LOGGED_EMPTY
import com.yahpz.domain.MINE_SHIFTS_NONE
import com.yahpz.domain.MINE_SHIFTS_PENDING_EMPTY
import com.yahpz.domain.SHIFT_KIND_LABELS
import com.yahpz.domain.VEHICLE_TYPE_LABELS
import com.yahpz.domain.SHIFT_FILL_TITLE
import com.yahpz.domain.SHIFT_ODOMETER_SAVE_TITLE
import com.yahpz.domain.SHIFT_TOO_EARLY_MESSAGE
import com.yahpz.domain.ShiftFillButton
import com.yahpz.domain.digitsOnly
import com.yahpz.domain.formatNumber
import com.yahpz.domain.odometerDelta
import com.yahpz.domain.shiftFillButton
import com.yahpz.domain.shiftOdometerError
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatPlate
import com.yahpz.domain.hebrewWeekdayLetter
import com.yahpz.domain.israelToday
import com.yahpz.domain.partitionMineShifts
import com.yahpz.domain.shiftStamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyShiftsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var windowsLoaded by remember { mutableIntStateOf(1) }
    var selected by remember { mutableStateOf<ShiftListItem?>(null) }
    var odometerShift by remember { mutableStateOf<ShiftListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.shifts.isEmpty()) app.reloadShifts()
    }

    val sections = partitionMineShifts(ui.shifts.map { it.mineItem }, israelToday(), windowsLoaded)
    val byId = ui.shifts.associateBy { it.id }
    val pending = sections.pending.mapNotNull { byId[it.id] }
    val future = sections.future.mapNotNull { byId[it.id] }
    val logged = sections.logged.mapNotNull { byId[it.id] }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadShifts()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("המשמרות שלי", style = TypeScale.title, color = FieldTheme.textPrimary)
            when {
                ui.shiftsFailed -> EmptyState(
                    title = ui.shiftsFailedMessage,
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadShifts() } },
                )
                ui.shiftsLoading && ui.shifts.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FieldTheme.accent)
                    Spacer(Modifier.height(12.dp))
                    Text("טוען משמרות…", style = TypeScale.body, color = FieldTheme.textSecondary)
                }
                ui.shifts.isEmpty() -> EmptyState(title = MINE_SHIFTS_NONE)
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    val today = israelToday()
                    val fillButton = { shift: ShiftListItem ->
                        shiftFillButton(shift.shiftDate, shift.status, ui.canManageUnit, today)
                    }
                    val onFillShift = { shift: ShiftListItem -> odometerShift = shift }
                    ShiftSection("משמרות ממתינות לתיעוד", MINE_SHIFTS_PENDING_EMPTY, pending, fillButton, onFillShift, app::openFill) { selected = it }
                    if (future.isNotEmpty()) {
                        ShiftSection("משמרות עתידיות", null, future, fillButton, onFillShift, app::openFill) { selected = it }
                    }
                    ShiftSection("משמרות שתועדו", MINE_SHIFTS_LOGGED_EMPTY, logged, fillButton, onFillShift, app::openFill) { selected = it }
                    if (sections.hasMoreLogged) {
                        GhostButton(title = "הצג 30 יום נוספים", onClick = { windowsLoaded += 1 })
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    selected?.let { shift ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(16.dp)) {
                Text("פרטי המשמרת", style = TypeScale.section, color = FieldTheme.textPrimary)
                Spacer(Modifier.height(12.dp))
                LedgerRow("תאריך", "${formatDate(shift.shiftDate)} (${hebrewWeekdayLetter(shift.shiftDate)})")
                LedgerRow("שם משמרת", SHIFT_KIND_LABELS[shift.shiftKind] ?: shift.shiftKind)
                LedgerRow("רכב", VEHICLE_TYPE_LABELS[shift.vehicleType] ?: shift.vehicleType)
                if (shift.vehicleType == "personal") {
                    LedgerRow("לוחית", formatPlate(shift.personalVehicle?.plateNumber.orEmpty()))
                }
                LedgerRow("אחמ״ש", shift.shiftLead?.display.orEmpty())
                LedgerRow("מתנדבים", "${shift.responders.size}")
                LedgerRow("אירועים", "${shift.bornEvents.size}")
                TextButton(onClick = { selected = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }

    odometerShift?.let { shift ->
        ShiftOdometerSheet(shift = shift, app = app, onDismiss = { odometerShift = null })
    }
}

private fun odometerInput(value: Double?): String = when {
    value == null -> ""
    value % 1.0 == 0.0 -> value.toLong().toString()
    else -> value.toString()
}

/**
 * The shift's one odometer pair, as on the web shift form: either reading may be saved
 * alone, only a reversed pair is refused. Status follows on the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShiftOdometerSheet(shift: ShiftListItem, app: AppModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var start by remember(shift.id) { mutableStateOf(odometerInput(shift.odometerStart)) }
    var end by remember(shift.id) { mutableStateOf(odometerInput(shift.odometerEnd)) }
    var error by remember(shift.id) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(SHIFT_FILL_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
            Text(
                "${shift.title} · ${formatDate(shift.shiftDate)}",
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
            FormField(
                label = "מד אוץ התחלה",
                value = start,
                onValueChange = {
                    start = digitsOnly(it)
                    error = null
                },
                keyboardType = KeyboardType.Number,
                mono = true,
            )
            FormField(
                label = "מד אוץ סיום",
                value = end,
                onValueChange = {
                    end = digitsOnly(it)
                    error = null
                },
                keyboardType = KeyboardType.Number,
                mono = true,
                error = error,
            )
            LedgerRow("קילומטרים", odometerDelta(start, end)?.let { formatNumber(it) }.orEmpty())
            PrimaryButton(
                title = SHIFT_ODOMETER_SAVE_TITLE,
                busy = saving,
                onClick = {
                    shiftOdometerError(start, end)?.let {
                        error = it
                        return@PrimaryButton
                    }
                    scope.launch {
                        saving = true
                        val failed = app.saveShiftOdometer(shift.id, start, end)
                        saving = false
                        if (failed == null) onDismiss() else error = failed
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShiftSection(
    title: String,
    empty: String?,
    items: List<ShiftListItem>,
    fillButton: (ShiftListItem) -> ShiftFillButton,
    onFill: (ShiftListItem) -> Unit,
    onEvent: (String) -> Unit,
    onOpen: (ShiftListItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = TypeScale.label, color = FieldTheme.textSecondary)
        if (items.isEmpty() && empty != null) {
            Text(empty, style = TypeScale.body, color = FieldTheme.textSecondary)
        } else {
            items.forEach { shift -> ShiftCard(shift, fillButton(shift), onFill, onOpen, onEvent) }
        }
    }
}

@Composable
private fun ShiftCard(
    shift: ShiftListItem,
    fillButton: ShiftFillButton,
    onFill: (ShiftListItem) -> Unit,
    onOpen: (ShiftListItem) -> Unit,
    onEvent: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    FieldCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(shift.title, style = TypeScale.section, color = FieldTheme.textPrimary)
                Text(
                    "${shift.responders.size} מתנדבים · ${shift.bornEvents.size} אירועים",
                    style = TypeScale.body,
                    color = FieldTheme.textSecondary,
                )
                Text(
                    "${formatDate(shift.shiftDate)} (${hebrewWeekdayLetter(shift.shiftDate)})",
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            StampChip(shiftStamp(shift.status))
        }
        Spacer(Modifier.height(12.dp))
        when (fillButton) {
            ShiftFillButton.ENABLED -> {
                PrimaryButton(title = SHIFT_FILL_TITLE, onClick = { onFill(shift) })
                Spacer(Modifier.height(8.dp))
            }
            ShiftFillButton.TOO_EARLY -> {
                PrimaryButton(title = SHIFT_FILL_TITLE, enabled = false, onClick = {})
                Text(SHIFT_TOO_EARLY_MESSAGE, style = TypeScale.caption, color = FieldTheme.textMuted)
                Spacer(Modifier.height(8.dp))
            }
            ShiftFillButton.HIDDEN -> Unit
        }
        GhostButton(title = "פרטי המשמרת", onClick = { onOpen(shift) })
        if (open) {
            Spacer(Modifier.height(8.dp))
            if (shift.bornEvents.isEmpty()) {
                Text("אין אירועים ממשמרת זו.", style = TypeScale.body, color = FieldTheme.textSecondary)
            } else {
                shift.bornEvents.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(FieldTheme.sunken)
                            .clickable { onEvent(event.id) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(event.eventType?.name ?: "אירוע", style = TypeScale.bodyStrong, color = FieldTheme.textPrimary)
                            Text(event.policeEventId ?: formatDate(event.eventDate), style = TypeScale.caption, color = FieldTheme.textMuted)
                        }
                        StampChip(event.stamp)
                    }
                }
            }
        }
    }
}
