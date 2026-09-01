package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yahpz.domain.MY_ACTIVE_EVENT_DISMISSED
import com.yahpz.domain.MY_ACTIVE_EVENTS_TITLE
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.StampTone
import com.yahpz.domain.UNIT_EVENTS_LOAD_FAILED
import com.yahpz.domain.cancelledStamp
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.eventStamp
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatNumber
import com.yahpz.domain.formatPlate
import com.yahpz.domain.formatTime
import com.yahpz.domain.mineFillCtaLabel
import com.yahpz.domain.participationStamp
import kotlinx.coroutines.launch

private const val UNIT_EVENTS_CAPTION =
    "מציג 80 אירועים אחרונים - ניתן לחפש גם אירועים ישנים יותר"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitEventsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<EventListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var dismissedIds by remember(ui.userId) {
        mutableStateOf(
            ui.userId?.let { ActiveEventDismissStore.dismissedIds(context, it) }.orEmpty(),
        )
    }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var fingerInRoot by remember { mutableStateOf(Offset.Zero) }
    var dropZone by remember { mutableStateOf(Rect.Zero) }
    val overDropZone = draggingId != null && dropZone.contains(fingerInRoot)

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.unitEvents.isEmpty()) app.reloadUnitEvents()
    }

    LaunchedEffect(ui.userId, ui.unitEvents, ui.myActiveUnitEvents) {
        val userId = ui.userId ?: return@LaunchedEffect
        val known = (ui.unitEvents.map { it.id } + ui.myActiveUnitEvents.map { it.id }).toSet()
        ActiveEventDismissStore.prune(context, userId, known)
        dismissedIds = ActiveEventDismissStore.dismissedIds(context, userId)
    }

    fun dismissFromActive(eventId: String) {
        val userId = ui.userId ?: return
        ActiveEventDismissStore.dismiss(context, userId, eventId)
        dismissedIds = ActiveEventDismissStore.dismissedIds(context, userId)
        app.showToast(MY_ACTIVE_EVENT_DISMISSED, StampTone.DONE)
    }

    val trimmed = query.trim()
    val activeEvents = ui.myActiveUnitEvents
        .filter { it.id !in dismissedIds }
        .let { rows ->
            if (trimmed.isEmpty()) rows
            else rows.filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
        }
    val activeVisibleIds = activeEvents.map { it.id }.toSet()
    val dismissedActive = ui.myActiveUnitEvents.filter { it.id in dismissedIds }
    val events = if (trimmed.isEmpty()) {
        (ui.unitEvents.filter { it.id !in activeVisibleIds } + dismissedActive.filter { d ->
            ui.unitEvents.none { it.id == d.id }
        })
    } else {
        (ui.unitEvents + dismissedActive).distinctBy { it.id }
            .filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
    }.sortedByDescending { it.eventDate }
    val hasAnyEvents = activeEvents.isNotEmpty() || events.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadUnitEvents()
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("אירועים", style = TypeScale.title, color = FieldTheme.textPrimary)
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "חיפוש לפי מספר אירוע, כביש, מיקום או אחמ״ש",
            )
            when {
                ui.unitEventsFailed -> EmptyState(
                    title = UNIT_EVENTS_LOAD_FAILED,
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadUnitEvents() } },
                )
                ui.unitEventsLoading && ui.unitEvents.isEmpty() && ui.myActiveUnitEvents.isEmpty() ->
                    LoadingBlock("טוען אירועים…")
                !hasAnyEvents -> EmptyState(
                    title = if (trimmed.isEmpty()) "אין אירועים להצגה" else "לא נמצאו אירועים תואמים",
                    actionTitle = if (trimmed.isEmpty()) null else "ניקוי חיפוש",
                    onAction = if (trimmed.isEmpty()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (activeEvents.isNotEmpty()) {
                        Text(MY_ACTIVE_EVENTS_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
                        Text(
                            "לחיצה ארוכה וגרירה לרשימה המלאה להסרה",
                            style = TypeScale.caption,
                            color = FieldTheme.textMuted,
                        )
                        activeEvents.forEach { event ->
                            DraggableActiveEventRow(
                                event = event,
                                dragging = draggingId == event.id,
                                dragOffset = if (draggingId == event.id) dragOffset else Offset.Zero,
                                onOpen = { app.openEditEvent(event.id) },
                                onDragStart = { startInRoot ->
                                    draggingId = event.id
                                    dragOffset = Offset.Zero
                                    fingerInRoot = startInRoot
                                },
                                onDrag = { amount ->
                                    dragOffset += amount
                                    fingerInRoot += amount
                                },
                                onDragEnd = {
                                    if (dropZone.contains(fingerInRoot)) {
                                        dismissFromActive(event.id)
                                    }
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { dropZone = it.boundsInRoot() }
                            .then(
                                if (draggingId != null) {
                                    Modifier
                                        .border(
                                            width = 2.dp,
                                            color = if (overDropZone) FieldTheme.accent else FieldTheme.hairline,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .background(
                                            if (overDropZone) FieldTheme.accentSubtle else FieldTheme.page,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(8.dp)
                                } else {
                                    Modifier
                                },
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (draggingId != null) {
                            Text(
                                if (overDropZone) "שחררו כאן להסרה מהפעילים" else "גררו לכאן להסרה מהפעילים",
                                style = TypeScale.caption,
                                color = if (overDropZone) FieldTheme.accent else FieldTheme.textMuted,
                            )
                        }
                        if (events.isNotEmpty()) {
                            Text(
                                UNIT_EVENTS_CAPTION,
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                            events.forEach { event -> UnitEventRow(event) { detail = event } }
                        } else if (draggingId != null) {
                            Text(
                                UNIT_EVENTS_CAPTION,
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }

    detail?.let { event ->
        val current = ui.unitEvents.firstOrNull { it.id == event.id } ?: event
        val mine = ui.userId?.let { current.ownParticipation(it) }
        val stamp = if (current.isCancelled) cancelledStamp() else eventStamp(current.status)
        var expandedResponderIds by remember(current.id) { mutableStateOf(setOf<String>()) }
        var detailResponders by remember(current.id) { mutableStateOf<List<UnitEventDetailResponderRow>?>(null) }

        LaunchedEffect(current.id) {
            expandedResponderIds = emptySet()
            detailResponders = runCatching {
                YahpazAPI.fetchUnitEventDetailResponders(current.id)
            }.getOrNull()
        }

        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (current.freeze.isFrozen) {
                            FrozenEventMark(current.freeze)
                        }
                        Text("פרטי האירוע", style = TypeScale.section, color = FieldTheme.textPrimary)
                    }
                    StampChip(stamp)
                }
                LedgerRow("תאריך", formatDate(current.eventDate))
                LedgerRow("מספר אירוע", current.policeEventId.orEmpty())
                LedgerRow("סוג אירוע", current.typeLabel)
                LedgerRow("כביש", current.road?.name.orEmpty())
                LedgerRow("מיקום", current.location.orEmpty())
                LedgerRow("סטטוס", stamp.label)
                LedgerRow("אחמ״ש", current.shiftLead?.display.orEmpty())
                Text("כוננים (${current.responders.size})", style = TypeScale.section, color = FieldTheme.textPrimary)
                if (current.responders.isEmpty()) {
                    Text(
                        "טרם שובצו כוננים לאירוע",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                } else if (detailResponders == null) {
                    LoadingBlock("טוען כוננים…")
                } else {
                    val loadedResponders = detailResponders.orEmpty()
                    loadedResponders.forEach { row ->
                        UnitEventResponderRow(
                            row = row,
                            eventDate = current.eventDate,
                            showTreatedPlates = current.origin != "shift",
                            isViewer = row.responderId == ui.userId,
                            expanded = row.id in expandedResponderIds,
                            onToggle = {
                                expandedResponderIds = if (row.id in expandedResponderIds) {
                                    expandedResponderIds - row.id
                                } else {
                                    expandedResponderIds + row.id
                                }
                            },
                        )
                    }
                }
                if (mine != null) {
                    mineFillCtaLabel(mine)?.let { label ->
                        PrimaryButton(title = label, onClick = {
                            val id = current.id
                            detail = null
                            app.openFill(id)
                        })
                    }
                }
                if (ui.canManageUnit) {
                    PrimaryButton(
                        title = EVENT_EDIT_TITLE,
                        onClick = {
                            val id = current.id
                            detail = null
                            app.openEditEvent(id)
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
private fun DraggableActiveEventRow(
    event: EventListItem,
    dragging: Boolean,
    dragOffset: Offset,
    onOpen: () -> Unit,
    onDragStart: (startInRoot: Offset) -> Unit,
    onDrag: (amount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 2f else 0f)
            .onGloballyPositioned { originInRoot = it.boundsInRoot().topLeft }
            .graphicsLayer {
                if (dragging) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    shadowElevation = 12f
                    alpha = 0.92f
                }
            }
            .pointerInput(event.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        onDragStart(originInRoot + start)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            },
    ) {
        UnitEventRow(event = event, enabled = !dragging, onOpen = onOpen)
    }
}

@Composable
private fun UnitEventResponderRow(
    row: UnitEventDetailResponderRow,
    eventDate: String,
    showTreatedPlates: Boolean,
    isViewer: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val stamp = participationStamp(row.status, isViewer)
    FieldCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.profile?.fullName?.trim().orEmpty().ifEmpty { "כונן" },
                    style = TypeScale.body,
                    color = FieldTheme.textPrimary,
                )
                row.profile?.callsign?.trim()?.takeIf { it.isNotEmpty() }?.let { callsign ->
                    Text("או״ק $callsign", style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StampChip(stamp)
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = FieldTheme.accent,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) -90f else 0f),
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                LedgerRow("זמן התחלה", formatTime(row.startedAt).orEmpty())
                LedgerRow("זמן סיום", formatTime(row.endedAt).orEmpty())
                row.totalKm?.let { km ->
                    LedgerRow("קילומטרים", "${formatNumber(km)} ק״מ")
                }
                LedgerRow("אמצעים", if (row.emergencyMeans) "כן" else "לא")
                val treated = treatedVehiclesLabel(row.treated)
                if (treated.isNotEmpty()) {
                    LedgerRow("רכבים שטופלו", treated)
                }
                if (showTreatedPlates) {
                    val plates = treatedPlatesLabel(row.treatedPlates)
                    if (plates.isNotEmpty()) {
                        LedgerRow("מספרי כלי רכב", plates)
                    }
                }
                row.vehiclePlate?.trim()?.takeIf { it.isNotEmpty() }?.let { plate ->
                    LedgerRow("לוחית רישוי", formatPlate(plate))
                }
                row.odometerStart?.let { start ->
                    LedgerRow("מד אוץ התחלה", formatNumber(start))
                }
                row.odometerEnd?.let { end ->
                    LedgerRow("מד אוץ סיום", formatNumber(end))
                }
                row.route?.trim()?.takeIf { it.isNotEmpty() }?.let { route ->
                    LedgerRow("נתיב נסיעה", route)
                }
            }
            row.treatmentDetail?.trim()?.takeIf { it.isNotEmpty() }?.let { detail ->
                Spacer(Modifier.height(8.dp))
                Text("פירוט הטיפול", style = TypeScale.label, color = FieldTheme.textSecondary)
                Text(detail, style = TypeScale.body, color = FieldTheme.textPrimary)
            }
            row.treatmentNotes?.trim()?.takeIf { it.isNotEmpty() }?.let { notes ->
                Spacer(Modifier.height(8.dp))
                Text("הערות לטיפול", style = TypeScale.label, color = FieldTheme.textSecondary)
                Text(notes, style = TypeScale.body, color = FieldTheme.textPrimary)
            }
        }
    }
}

private fun treatedVehiclesLabel(treated: List<TreatedVehicleKindRow>): String =
    treated.mapNotNull { row ->
        val qty = row.quantity ?: return@mapNotNull null
        val name = row.kind?.name?.trim().orEmpty().ifEmpty { "רכב" }
        "$name × $qty"
    }.joinToString(", ")

private fun treatedPlatesLabel(plates: List<EventTreatedPlateRow>): String =
    plates.mapNotNull { plate ->
        plate.plateNumber?.trim()?.takeIf { it.isNotEmpty() }?.let(::formatPlate)
    }.joinToString(", ")

@Composable
private fun UnitEventRow(
    event: EventListItem,
    enabled: Boolean = true,
    onOpen: () -> Unit,
) {
    val stamp = if (event.isCancelled) cancelledStamp() else eventStamp(event.status)
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(enabled = enabled, onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (event.freeze.isFrozen) {
                        FrozenEventMark(event.freeze)
                    }
                    Text(
                        event.typeLabel.ifEmpty { "אירוע" },
                        style = TypeScale.section,
                        color = FieldTheme.textPrimary,
                    )
                }
                Text(
                    listOfNotNull(
                        formatDate(event.eventDate),
                        event.policeEventId?.takeIf { it.isNotEmpty() },
                        event.road?.name?.takeIf { it.isNotEmpty() },
                        event.location?.takeIf { it.isNotEmpty() },
                    ).joinToString(" · "),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
                event.shiftLead?.display?.let { lead ->
                    Text("אחמ״ש: $lead", style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            StampChip(stamp)
        }
        val pending = event.responders.count { it.status != ParticipationStatus.DONE }
        if (event.responders.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${event.responders.size} כוננים · $pending ממתינים לתיעוד",
                style = TypeScale.caption,
                color = FieldTheme.textSecondary,
            )
        }
    }
}
