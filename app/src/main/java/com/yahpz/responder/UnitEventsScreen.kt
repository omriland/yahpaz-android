package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yahpz.domain.EVENT_DELETE_ACTION
import com.yahpz.domain.EVENT_DELETE_CONFIRM
import com.yahpz.domain.EVENT_DELETE_TITLE
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.MY_ACTIVE_ADD
import com.yahpz.domain.MY_ACTIVE_DRAG_TO_ACTIVE
import com.yahpz.domain.MY_ACTIVE_DRAG_TO_ADD
import com.yahpz.domain.MY_ACTIVE_DROP_TO_ADD
import com.yahpz.domain.MY_ACTIVE_EVENTS_EMPTY
import com.yahpz.domain.MY_ACTIVE_EVENTS_TITLE
import com.yahpz.domain.MY_ACTIVE_REMOVE
import com.yahpz.domain.MY_ACTIVE_REMOVE_LOCKED
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.StampTone
import com.yahpz.domain.UNIT_EVENTS_LOAD_FAILED
import com.yahpz.domain.canDeleteUnassignedEvent
import com.yahpz.domain.canRemoveFromMyActive
import com.yahpz.domain.cancelledStamp
import com.yahpz.domain.eventStamp
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatNumber
import com.yahpz.domain.formatPlate
import com.yahpz.domain.formatTime
import com.yahpz.domain.mineFillCtaLabel
import com.yahpz.domain.participationStamp
import com.yahpz.domain.visibleMyActiveIds
import com.yahpz.domain.INCOMPLETE_EVENTS_HEADING
import com.yahpz.domain.INCOMPLETE_NOTICE_MARK
import com.yahpz.domain.SHOW_OTHERS_CREATED_EVENTS_LABEL
import com.yahpz.domain.incompleteFieldLabels
import com.yahpz.domain.incompleteNoticeLabel
import com.yahpz.domain.missingEventFields
import com.yahpz.domain.partitionIncompleteEvents
import com.yahpz.domain.shouldFilterUnitEventsToOwnCreated
import kotlinx.coroutines.launch

private const val UNIT_EVENTS_CAPTION =
    "מציג 80 אירועים אחרונים - ניתן לחפש גם אירועים ישנים יותר"

private enum class ActiveDragSource { ACTIVE, CATALOG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitEventsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<EventListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragSource by remember { mutableStateOf<ActiveDragSource?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var fingerInRoot by remember { mutableStateOf(Offset.Zero) }
    var activeDropZone by remember { mutableStateOf(Rect.Zero) }
    var catalogDropZone by remember { mutableStateOf(Rect.Zero) }
    val overActiveDrop = draggingId != null &&
        dragSource == ActiveDragSource.CATALOG &&
        activeDropZone.contains(fingerInRoot)
    val overCatalogDrop = draggingId != null &&
        dragSource == ActiveDragSource.ACTIVE &&
        catalogDropZone.contains(fingerInRoot)

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.unitEvents.isEmpty()) app.reloadUnitEvents()
    }

    var pendingBoardIds by remember { mutableStateOf(setOf<String>()) }
    val trimmed = query.trim()
    val pinnedIds = ui.myActiveEventPrefs.filter { it.kind == "pin" }.map { it.eventId }.toSet()
    val hiddenIds = ui.myActiveEventPrefs.filter { it.kind == "hide" }.map { it.eventId }.toSet()
    val viewerId = ui.userId
    val lockedIds = ui.myActiveUnitEvents
        .filter { event ->
            viewerId != null &&
                !canRemoveFromMyActive(viewerId, event.shiftLeadId, event.status, event.isCancelled)
        }
        .map { it.id }
    val catalogById = (
        ui.unitEvents + ui.myActiveUnitEvents + ui.myActivePinnedEvents
        ).associateBy { it.id }
    val activeOrderedIds = visibleMyActiveIds(
        lockedIds = lockedIds,
        autoIds = ui.myActiveUnitEvents.map { it.id },
        pinnedIds = pinnedIds,
        hiddenIds = hiddenIds,
    )
    val activeEvents = activeOrderedIds.mapNotNull { catalogById[it] }
        .let { rows ->
            if (trimmed.isEmpty()) rows
            else rows.filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
        }
    val activeVisibleIds = activeEvents.map { it.id }.toSet()
    val hiddenAuto = ui.myActiveUnitEvents.filter { it.id in hiddenIds && it.id !in lockedIds }
    val events = if (trimmed.isEmpty()) {
        (ui.unitEvents.filter { it.id !in activeVisibleIds } + hiddenAuto.filter { d ->
            ui.unitEvents.none { it.id == d.id }
        })
    } else {
        (ui.unitEvents + hiddenAuto).distinctBy { it.id }
            .filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
    }.filter { it.id !in activeVisibleIds }
        .sortedByDescending { it.eventDate }
    val (incompleteEvents, restEvents) = partitionIncompleteEvents(events) { it.asIncompleteSnapshot() }

    fun dismissFromActive(eventId: String) {
        if (eventId in pendingBoardIds) return
        pendingBoardIds = pendingBoardIds + eventId
        scope.launch {
            try {
                app.removeEventFromMyActiveBoard(eventId)
            } finally {
                pendingBoardIds = pendingBoardIds - eventId
            }
        }
    }

    fun addToActive(eventId: String) {
        if (eventId in pendingBoardIds) return
        pendingBoardIds = pendingBoardIds + eventId
        scope.launch {
            try {
                app.addEventToMyActiveBoard(eventId)
            } finally {
                pendingBoardIds = pendingBoardIds - eventId
            }
        }
    }

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
                placeholder = "אירוע, כביש, מיקום או אחמ״ש",
            )
            if (shouldFilterUnitEventsToOwnCreated(ui.roles)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable { app.setShowOthersCreatedEvents(!ui.showOthersCreatedEvents) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        SHOW_OTHERS_CREATED_EVENTS_LABEL,
                        style = TypeScale.body,
                        color = FieldTheme.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = ui.showOthersCreatedEvents,
                        onCheckedChange = { app.setShowOthersCreatedEvents(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FieldTheme.textOnAccent,
                            checkedTrackColor = FieldTheme.accent,
                            uncheckedThumbColor = FieldTheme.raised,
                            uncheckedTrackColor = FieldTheme.strong,
                        ),
                    )
                }
            }
            when {
                ui.unitEventsFailed -> EmptyState(
                    title = UNIT_EVENTS_LOAD_FAILED,
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadUnitEvents() } },
                )
                ui.unitEventsLoading && ui.unitEvents.isEmpty() && ui.myActiveUnitEvents.isEmpty() ->
                    LoadingBlock("טוען אירועים…")
                else -> Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState(),
                        enabled = draggingId == null,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { activeDropZone = it.boundsInRoot() }
                            .then(
                                if (dragSource == ActiveDragSource.CATALOG) {
                                    Modifier
                                        .border(
                                            width = 2.dp,
                                            color = if (overActiveDrop) FieldTheme.accent else FieldTheme.hairline,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .background(
                                            if (overActiveDrop) FieldTheme.accentSubtle else FieldTheme.page,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(8.dp)
                                } else {
                                    Modifier
                                },
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(MY_ACTIVE_EVENTS_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
                        Text(
                            when {
                                dragSource == ActiveDragSource.CATALOG && overActiveDrop -> MY_ACTIVE_DROP_TO_ADD
                                dragSource == ActiveDragSource.CATALOG -> MY_ACTIVE_DRAG_TO_ADD
                                activeEvents.isEmpty() -> MY_ACTIVE_EVENTS_EMPTY
                                else -> "הסרה מהפעילים, או לחיצה ארוכה וגרירה לרשימה"
                            },
                            style = TypeScale.caption,
                            color = if (overActiveDrop) FieldTheme.accent else FieldTheme.textMuted,
                        )
                        if (activeEvents.isEmpty() && dragSource == ActiveDragSource.CATALOG) {
                            Spacer(Modifier.height(48.dp))
                        }
                        activeEvents.forEach { event ->
                            val canRemove = viewerId != null &&
                                canRemoveFromMyActive(
                                    viewerId,
                                    event.shiftLeadId,
                                    event.status,
                                    event.isCancelled,
                                )
                            DraggableActiveEventRow(
                                event = event,
                                dragging = draggingId == event.id,
                                dragOffset = if (draggingId == event.id) dragOffset else Offset.Zero,
                                boardActionTitle = MY_ACTIVE_REMOVE,
                                boardActionEnabled = canRemove && event.id !in pendingBoardIds,
                                boardActionHint = if (canRemove) null else MY_ACTIVE_REMOVE_LOCKED,
                                onBoardAction = { dismissFromActive(event.id) },
                                onOpen = { app.openEditEvent(event.id) },
                                onDragStart = { startInRoot ->
                                    draggingId = event.id
                                    dragSource = ActiveDragSource.ACTIVE
                                    dragOffset = Offset.Zero
                                    fingerInRoot = startInRoot
                                },
                                onDrag = { amount ->
                                    dragOffset += amount
                                    fingerInRoot += amount
                                },
                                onDragEnd = {
                                    if (overCatalogDrop) dismissFromActive(event.id)
                                    draggingId = null
                                    dragSource = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragSource = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { catalogDropZone = it.boundsInRoot() }
                            .then(
                                if (dragSource == ActiveDragSource.ACTIVE) {
                                    Modifier
                                        .border(
                                            width = 2.dp,
                                            color = if (overCatalogDrop) FieldTheme.accent else FieldTheme.hairline,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .background(
                                            if (overCatalogDrop) FieldTheme.accentSubtle else FieldTheme.page,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(8.dp)
                                } else {
                                    Modifier
                                },
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (dragSource == ActiveDragSource.ACTIVE) {
                            Text(
                                if (overCatalogDrop) "שחררו כאן להסרה מהפעילים" else "גררו לכאן להסרה מהפעילים",
                                style = TypeScale.caption,
                                color = if (overCatalogDrop) FieldTheme.accent else FieldTheme.textMuted,
                            )
                        }
                        if (events.isNotEmpty()) {
                            Text(
                                UNIT_EVENTS_CAPTION,
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                            Text(
                                MY_ACTIVE_DRAG_TO_ACTIVE,
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                            if (incompleteEvents.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        INCOMPLETE_EVENTS_HEADING,
                                        style = TypeScale.label,
                                        color = FieldTheme.textSecondary,
                                    )
                                    incompleteEvents.forEach { event ->
                                        CatalogEventRow(
                                            event = event,
                                            draggingId = draggingId,
                                            dragOffset = dragOffset,
                                            pendingBoardIds = pendingBoardIds,
                                            onAddToActive = { addToActive(event.id) },
                                            onOpen = { detail = event },
                                            onDragStart = { startInRoot ->
                                                draggingId = event.id
                                                dragSource = ActiveDragSource.CATALOG
                                                dragOffset = Offset.Zero
                                                fingerInRoot = startInRoot
                                            },
                                            onDrag = { amount ->
                                                dragOffset += amount
                                                fingerInRoot += amount
                                            },
                                            onDragEnd = {
                                                if (overActiveDrop) addToActive(event.id)
                                                draggingId = null
                                                dragSource = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggingId = null
                                                dragSource = null
                                                dragOffset = Offset.Zero
                                            },
                                        )
                                    }
                                }
                            }
                            restEvents.forEach { event ->
                                CatalogEventRow(
                                    event = event,
                                    draggingId = draggingId,
                                    dragOffset = dragOffset,
                                    pendingBoardIds = pendingBoardIds,
                                    onAddToActive = { addToActive(event.id) },
                                    onOpen = { detail = event },
                                    onDragStart = { startInRoot ->
                                        draggingId = event.id
                                        dragSource = ActiveDragSource.CATALOG
                                        dragOffset = Offset.Zero
                                        fingerInRoot = startInRoot
                                    },
                                    onDrag = { amount ->
                                        dragOffset += amount
                                        fingerInRoot += amount
                                    },
                                    onDragEnd = {
                                        if (overActiveDrop) addToActive(event.id)
                                        draggingId = null
                                        dragSource = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragSource = null
                                        dragOffset = Offset.Zero
                                    },
                                )
                            }
                        } else if (dragSource == ActiveDragSource.ACTIVE) {
                            Text(
                                UNIT_EVENTS_CAPTION,
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                            Spacer(Modifier.height(48.dp))
                        } else {
                            EmptyState(
                                title = if (trimmed.isEmpty()) "אין אירועים להצגה" else "לא נמצאו אירועים תואמים",
                                actionTitle = if (trimmed.isEmpty()) null else "ניקוי חיפוש",
                                onAction = if (trimmed.isEmpty()) null else ({ query = "" }),
                            )
                        }
                    }
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }

    detail?.let { event ->
        val current = ui.unitEvents.firstOrNull { it.id == event.id }
            ?: ui.myActiveUnitEvents.firstOrNull { it.id == event.id }
            ?: ui.myActivePinnedEvents.firstOrNull { it.id == event.id }
            ?: event
        val mine = ui.userId?.let { current.ownParticipation(it) }
        val stamp = if (current.isCancelled) cancelledStamp() else eventStamp(current.status)
        var expandedResponderIds by remember(current.id) { mutableStateOf(setOf<String>()) }
        var detailResponders by remember(current.id) { mutableStateOf<List<UnitEventDetailResponderRow>?>(null) }
        var confirmDelete by remember(current.id) { mutableStateOf(false) }
        var deleting by remember(current.id) { mutableStateOf(false) }

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
                LedgerRow("נת״צ", if (current.busLane) "כן" else "לא")
                LedgerRow("סטטוס", stamp.label)
                EventLeadLedgerRows(current.shiftLead, current.secondaryLeads)
                Text("מתנדבים (${current.responders.size})", style = TypeScale.section, color = FieldTheme.textPrimary)
                if (current.responders.isEmpty()) {
                    Text(
                        "טרם שובצו מתנדבים לאירוע",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                } else if (detailResponders == null) {
                    LoadingBlock("טוען מתנדבים…")
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
                if (canDeleteUnassignedEvent(
                        ui.canManageUnit,
                        current.responders.size,
                        ui.canAdmin,
                        ui.userId,
                        current.shiftLeadId,
                    )) {
                    if (confirmDelete) {
                        Text(EVENT_DELETE_CONFIRM, style = TypeScale.body, color = FieldTheme.textSecondary)
                        GhostButton(
                            title = EVENT_DELETE_ACTION,
                            danger = true,
                            enabled = !deleting,
                            onClick = {
                                val id = current.id
                                scope.launch {
                                    deleting = true
                                    val error = app.deleteUnitEvent(id)
                                    deleting = false
                                    if (error != null) {
                                        app.showToast(error, StampTone.PENDING)
                                        confirmDelete = false
                                    } else {
                                        detail = null
                                    }
                                }
                            },
                        )
                    } else {
                        GhostButton(
                            title = EVENT_DELETE_TITLE,
                            danger = true,
                            onClick = { confirmDelete = true },
                        )
                    }
                }
                TextButton(onClick = { detail = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun CatalogEventRow(
    event: EventListItem,
    draggingId: String?,
    dragOffset: Offset,
    pendingBoardIds: Set<String>,
    onAddToActive: () -> Unit,
    onOpen: () -> Unit,
    onDragStart: (startInRoot: Offset) -> Unit,
    onDrag: (amount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    DraggableActiveEventRow(
        event = event,
        dragging = draggingId == event.id,
        dragOffset = if (draggingId == event.id) dragOffset else Offset.Zero,
        boardActionTitle = MY_ACTIVE_ADD,
        boardActionEnabled = event.id !in pendingBoardIds,
        boardActionHint = null,
        onBoardAction = onAddToActive,
        onOpen = onOpen,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
    )
}

@Composable
private fun DraggableActiveEventRow(
    event: EventListItem,
    dragging: Boolean,
    dragOffset: Offset,
    boardActionTitle: String,
    boardActionEnabled: Boolean,
    boardActionHint: String?,
    onBoardAction: () -> Unit,
    onOpen: () -> Unit,
    onDragStart: (startInRoot: Offset) -> Unit,
    onDrag: (amount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)
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
                        latestOnDragStart(originInRoot + start)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        latestOnDrag(amount)
                    },
                    onDragEnd = { latestOnDragEnd() },
                    onDragCancel = { latestOnDragCancel() },
                )
            },
    ) {
        UnitEventRow(
            event = event,
            enabled = !dragging,
            boardActionTitle = boardActionTitle,
            boardActionEnabled = boardActionEnabled && !dragging,
            boardActionHint = boardActionHint,
            onBoardAction = onBoardAction,
            onOpen = onOpen,
            incompleteFields = incompleteFieldLabels(missingEventFields(event.asIncompleteSnapshot())),
            incompleteSpoken = incompleteNoticeLabel(missingEventFields(event.asIncompleteSnapshot())),
        )
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
                    row.profile?.fullName?.trim().orEmpty().ifEmpty { "מתנדב" },
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
    boardActionTitle: String? = null,
    boardActionEnabled: Boolean = false,
    boardActionHint: String? = null,
    onBoardAction: (() -> Unit)? = null,
    onOpen: () -> Unit,
    incompleteFields: List<String> = emptyList(),
    incompleteSpoken: String = "",
) {
    val stamp = if (event.isCancelled) cancelledStamp() else eventStamp(event.status)
    val incomplete = incompleteFields.isNotEmpty()
    val cardShape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .height(IntrinsicSize.Min)
            .clip(cardShape)
            .background(FieldTheme.raised)
            .border(1.dp, FieldTheme.hairline, cardShape)
            .clickable(enabled = enabled, onClick = onOpen),
    ) {
        if (incomplete) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(FieldTheme.alert),
            )
        }
        Column(Modifier.weight(1f).padding(16.dp)) {
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
                    event.leadsCaption().takeIf { it.isNotEmpty() }?.let { lead ->
                        Text("אחמ״ש: $lead", style = TypeScale.caption, color = FieldTheme.textMuted)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    StampChip(stamp)
                    if (boardActionTitle != null && onBoardAction != null) {
                        TextButton(
                            onClick = onBoardAction,
                            enabled = boardActionEnabled,
                            modifier = Modifier.heightIn(min = 44.dp),
                        ) {
                            Text(
                                boardActionTitle,
                                style = TypeScale.bodyStrong,
                                color = if (boardActionEnabled) FieldTheme.accent else FieldTheme.textMuted,
                            )
                        }
                        boardActionHint?.let { hint ->
                            Text(hint, style = TypeScale.caption, color = FieldTheme.textMuted)
                        }
                    }
                }
            }
            val pending = event.responders.count { it.status != ParticipationStatus.DONE }
            if (event.responders.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${event.responders.size} מתנדבים · $pending ממתינים לתיעוד",
                    style = TypeScale.caption,
                    color = FieldTheme.textSecondary,
                )
            }
            if (incomplete) {
                IncompleteFieldsNotice(fields = incompleteFields, spoken = incompleteSpoken)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncompleteFieldsNotice(fields: List<String>, spoken: String) {
    if (fields.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .semantics { contentDescription = spoken },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FieldTheme.hairline),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                INCOMPLETE_NOTICE_MARK,
                style = TypeScale.label,
                color = FieldTheme.partialOnTint,
            )
            fields.forEach { label ->
                Text(
                    label,
                    style = TypeScale.caption,
                    color = FieldTheme.textPrimary,
                    modifier = Modifier.drawBehind {
                        val stroke = 1.5.dp.toPx()
                        drawLine(
                            color = FieldTheme.partial,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        )
                    },
                )
            }
        }
    }
}
