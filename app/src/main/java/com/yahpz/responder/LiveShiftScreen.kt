package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yahpz.domain.EVENT_DELETE_ACTION
import com.yahpz.domain.EventStatus
import com.yahpz.domain.LIVE_SHIFT_BACK
import com.yahpz.domain.LIVE_SHIFT_LOADING
import com.yahpz.domain.LIVE_SHIFT_RETRY
import com.yahpz.domain.LIVE_SHIFT_UNNAMED_CREW_MEMBER
import com.yahpz.domain.LookupOption
import com.yahpz.domain.LIVE_SHIFT_LOAD_FAILED
import com.yahpz.domain.LIVE_SHIFT_NEW_EVENT_ACTION
import com.yahpz.domain.LIVE_SHIFT_NOT_AVAILABLE
import com.yahpz.domain.LIVE_SHIFT_NO_EVENTS
import com.yahpz.domain.LIVE_SHIFT_ODOMETER_ACTION
import com.yahpz.domain.SHIFT_EVENT_DELETE_BODY
import com.yahpz.domain.SHIFT_EVENT_DELETE_CANCEL
import com.yahpz.domain.SHIFT_EVENT_DELETE_TITLE
import com.yahpz.domain.ShiftPhase
import com.yahpz.domain.canCreateShiftEvent
import com.yahpz.domain.canDeleteShiftEvent
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatJerusalemTimeOfDay
import com.yahpz.domain.hebrewWeekdayLetter
import com.yahpz.domain.isShiftBornEventEmpty
import com.yahpz.domain.liveShiftCrewTitle
import com.yahpz.domain.liveShiftEventStamp
import com.yahpz.domain.liveShiftOpenedAtLabel
import com.yahpz.domain.liveShiftEventTypeLabel
import com.yahpz.domain.liveShiftEventsTitle
import com.yahpz.domain.liveShiftStamp
import com.yahpz.domain.shiftTimeLeftLabel
import com.yahpz.domain.shiftWindowPhase
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MinTouchTarget = 48.dp
internal const val CLOCK_TICK_MS = 60_000L

data class LiveShiftEvent(
    val id: String,
    val typeName: String,
    val location: String?,
    val openedAt: Instant?,
    val empty: Boolean,
    val done: Boolean,
)

data class LiveShiftState(
    val shift: ShiftListItem,
    val phase: ShiftPhase,
    val now: Instant,
    val events: List<LiveShiftEvent>,
    val crewNames: List<String>,
    val canManage: Boolean,
)

class LiveShiftActions(
    val refresh: suspend () -> Unit,
    val openNewEvent: () -> Unit,
    val openEvent: (String) -> Unit,
    /** Deletes the event; the answer is the refusal to show, or null when it is gone. */
    val deleteEvent: suspend (String) -> String?,
    val openOdometer: () -> Unit,
)

private sealed interface LiveShiftLoad {
    data object Loading : LiveShiftLoad
    data object Failed : LiveShiftLoad
    data object Missing : LiveShiftLoad
    data class Ready(val shift: ShiftListItem) : LiveShiftLoad
}

private fun parseCreatedAt(value: String?): Instant? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

private fun ShiftBornEvent.toLiveShiftEvent() = LiveShiftEvent(
    id = id,
    typeName = eventType?.name.orEmpty(),
    location = location?.takeIf { it.isNotBlank() },
    openedAt = parseCreatedAt(createdAt),
    empty = isShiftBornEventEmpty(snapshot),
    done = status == EventStatus.DONE,
)

private fun ShiftListItem.liveShiftState(now: Instant, canManage: Boolean) = LiveShiftState(
    shift = this,
    phase = toShiftWindow().let { shiftWindowPhase(it?.startsAt, it?.endsAt, now) },
    now = now,
    events = bornEvents.map { it.toLiveShiftEvent() }.sortedByDescending { it.openedAt },
    crewNames = responders.map { it.profile?.display ?: LIVE_SHIFT_UNNAMED_CREW_MEMBER },
    canManage = canManage,
)

private fun ShiftListItem.hoursLabel(): String? {
    val window = toShiftWindow() ?: return null
    return "${formatJerusalemTimeOfDay(window.startsAt)}–${formatJerusalemTimeOfDay(window.endsAt)}"
}

/** A shift with hours, opened from the shift lists or the on-shift bar. */
@Composable
fun LiveShiftScreen(shiftId: String, app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var load by remember(shiftId) { mutableStateOf<LiveShiftLoad>(LiveShiftLoad.Loading) }
    var now by remember { mutableStateOf(Instant.now()) }
    var newEventOpen by remember(shiftId) { mutableStateOf(false) }
    var odometerOpen by remember(shiftId) { mutableStateOf(false) }

    suspend fun reload() {
        now = Instant.now()
        load = try {
            YahpazAPI.fetchLiveShift(shiftId)?.let { LiveShiftLoad.Ready(it) } ?: LiveShiftLoad.Missing
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            load as? LiveShiftLoad.Ready ?: LiveShiftLoad.Failed
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { reload() } }
    LaunchedEffect(shiftId) { reload() }
    LaunchedEffect(Unit) {
        if (ui.lookups.isEmpty && !ui.lookupsLoading) app.reloadLookups()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CLOCK_TICK_MS)
            now = Instant.now()
        }
    }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(FieldTheme.page)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget).padding(horizontal = 8.dp)) {
            Text(LIVE_SHIFT_BACK, color = FieldTheme.accent, style = TypeScale.body)
        }
        when (val current = load) {
            LiveShiftLoad.Loading -> LoadingBlock(LIVE_SHIFT_LOADING)
            LiveShiftLoad.Failed -> EmptyState(
                title = LIVE_SHIFT_LOAD_FAILED,
                actionTitle = LIVE_SHIFT_RETRY,
                onAction = { scope.launch { reload() } },
            )
            LiveShiftLoad.Missing -> EmptyState(title = LIVE_SHIFT_NOT_AVAILABLE)
            is LiveShiftLoad.Ready -> {
                val actions = LiveShiftActions(
                    refresh = { reload() },
                    openNewEvent = { newEventOpen = true },
                    openEvent = app::openShiftBornFill,
                    deleteEvent = { eventId -> deleteLiveShiftEvent(eventId) { reload() } },
                    openOdometer = { odometerOpen = true },
                )
                LiveShiftContent(current.shift.liveShiftState(now, ui.canManageUnit), actions)
                if (newEventOpen) {
                    NewShiftEventFlow(
                        shiftId = shiftId,
                        types = ui.lookups.eventTypes,
                        onOpened = { eventId ->
                            newEventOpen = false
                            app.openShiftBornFill(eventId)
                        },
                        onDismiss = { newEventOpen = false },
                    )
                }
                if (odometerOpen) {
                    ShiftOdometerSheet(
                        shift = current.shift,
                        app = app,
                        onDismiss = {
                            odometerOpen = false
                            scope.launch { reload() }
                        },
                    )
                }
            }
        }
    }
}

private suspend fun deleteLiveShiftEvent(eventId: String, reload: suspend () -> Unit): String? {
    val result = YahpazAPI.deleteEmptyShiftEvent(eventId)
    if (result.isFailure) return result.exceptionOrNull()?.message
    reload()
    return null
}

/** One tap on a type creates the event; the sheet stays open with the refusal when the server says no. */
@Composable
private fun NewShiftEventFlow(
    shiftId: String,
    types: List<LookupOption>,
    onOpened: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    NewShiftEventSheet(
        types = types,
        errorMessage = errorMessage,
        onPick = { type ->
            if (!creating) {
                scope.launch {
                    creating = true
                    errorMessage = null
                    YahpazAPI.createShiftEvent(shiftId, type.id).fold(
                        onSuccess = onOpened,
                        onFailure = { errorMessage = it.message },
                    )
                    creating = false
                }
            }
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveShiftContent(state: LiveShiftState, actions: LiveShiftActions) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                actions.refresh()
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(FieldTheme.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShiftHeader(state)
            if (canCreateShiftEvent(state.canManage, state.phase)) {
                PrimaryButton(
                    title = LIVE_SHIFT_NEW_EVENT_ACTION,
                    onClick = actions.openNewEvent,
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
            EventsCard(state, actions.openEvent) { eventToDelete = it }
            CrewCard(state.crewNames)
            GhostButton(
                title = LIVE_SHIFT_ODOMETER_ACTION,
                onClick = actions.openOdometer,
                modifier = Modifier.heightIn(min = MinTouchTarget),
            )
        }
    }

    eventToDelete?.let { eventId ->
        DeleteEventDialog(
            onConfirm = { actions.deleteEvent(eventId) },
            onClose = { eventToDelete = null },
        )
    }
}

@Composable
private fun ShiftHeader(state: LiveShiftState) {
    val shift = state.shift
    val timeLeft = shift.toShiftWindow()?.let { shiftTimeLeftLabel(state.phase, it.endsAt, state.now) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(shift.title, style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(
                "${formatDate(shift.shiftDate)} (${hebrewWeekdayLetter(shift.shiftDate)})",
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
            shift.hoursLabel()?.let { Text(it, style = TypeScale.caption, color = FieldTheme.textMuted) }
            timeLeft?.let { Text(it, style = TypeScale.caption, color = FieldTheme.textSecondary) }
        }
        StampChip(liveShiftStamp(state.phase, shift.status))
    }
}

@Composable
private fun EventsCard(state: LiveShiftState, onOpenEvent: (String) -> Unit, onAskDelete: (String) -> Unit) {
    FieldCard {
        Text(liveShiftEventsTitle(state.events.size), style = TypeScale.section, color = FieldTheme.textPrimary)
        if (state.events.isEmpty()) {
            Text(LIVE_SHIFT_NO_EVENTS, style = TypeScale.body, color = FieldTheme.textSecondary, modifier = Modifier.padding(top = 8.dp))
        }
        state.events.forEach { event ->
            val mayDelete = canDeleteShiftEvent(state.canManage, state.phase, event.empty, event.done)
            EventRow(event, mayDelete, onOpen = { onOpenEvent(event.id) }, onAskDelete = { onAskDelete(event.id) })
        }
    }
}

@Composable
private fun EventRow(event: LiveShiftEvent, mayDelete: Boolean, onOpen: () -> Unit, onAskDelete: () -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldTheme.sunken)
                .heightIn(min = MinTouchTarget)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    liveShiftEventTypeLabel(event.typeName),
                    style = TypeScale.bodyStrong,
                    color = FieldTheme.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                StampChip(liveShiftEventStamp(event.empty, event.done))
            }
            event.location?.let { Text(it, style = TypeScale.caption, color = FieldTheme.textMuted) }
            event.openedAt?.let {
                Text(liveShiftOpenedAtLabel(it), style = TypeScale.caption, color = FieldTheme.textMuted)
            }
        }
        if (mayDelete) {
            GhostButton(
                title = EVENT_DELETE_ACTION,
                danger = true,
                onClick = onAskDelete,
                modifier = Modifier.heightIn(min = MinTouchTarget),
            )
        }
    }
}

@Composable
private fun CrewCard(names: List<String>) {
    FieldCard {
        Text(liveShiftCrewTitle(names.size), style = TypeScale.section, color = FieldTheme.textPrimary)
        names.forEach { name ->
            Text(name, style = TypeScale.body, color = FieldTheme.textPrimary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun DeleteEventDialog(onConfirm: suspend () -> String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!deleting) onClose() },
        title = { Text(SHIFT_EVENT_DELETE_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(SHIFT_EVENT_DELETE_BODY, style = TypeScale.body, color = FieldTheme.textSecondary)
                errorMessage?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !deleting,
                modifier = Modifier.heightIn(min = MinTouchTarget),
                onClick = {
                    scope.launch {
                        deleting = true
                        errorMessage = onConfirm()
                        deleting = false
                        if (errorMessage == null) onClose()
                    }
                },
            ) {
                Text(EVENT_DELETE_ACTION, color = FieldTheme.alert)
            }
        },
        dismissButton = {
            TextButton(
                enabled = !deleting,
                modifier = Modifier.heightIn(min = MinTouchTarget),
                onClick = onClose,
            ) {
                Text(SHIFT_EVENT_DELETE_CANCEL, color = FieldTheme.accent)
            }
        },
    )
}
