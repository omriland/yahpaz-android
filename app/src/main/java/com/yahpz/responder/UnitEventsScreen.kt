package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.yahpz.domain.AdaptiveLayout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yahpz.domain.EVENT_DELETE_ACTION
import com.yahpz.domain.EVENT_DELETE_CONFIRM
import com.yahpz.domain.EVENT_DELETE_CONFIRM_WITH_RESPONDERS
import com.yahpz.domain.EVENT_DELETE_TITLE
import com.yahpz.domain.EVENT_EDIT_LOCKED_TOOLTIP
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.formatPatrolCallsign
import com.yahpz.domain.isEventEditAgeLocked
import com.yahpz.domain.resolvePatrolCallsign
import com.yahpz.domain.MY_ACTIVE_ADD
import com.yahpz.domain.MY_ACTIVE_DRAG_TO_ADD
import com.yahpz.domain.MY_ACTIVE_DROP_TO_ADD
import com.yahpz.domain.MY_ACTIVE_EVENTS_EMPTY
import com.yahpz.domain.MY_ACTIVE_EVENTS_TITLE
import com.yahpz.domain.MY_ACTIVE_REMOVE_LOCKED
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.StampTone
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.LEAD_ENTERED_LABEL
import com.yahpz.domain.leadEnteredAwaitingStamp
import com.yahpz.domain.leadEnteredStamp
import com.yahpz.domain.UNIT_EVENTS_LOAD_FAILED
import com.yahpz.domain.canDeleteUnassignedEvent
import com.yahpz.domain.canRemoveFromMyActive
import com.yahpz.domain.cancelledStamp
import com.yahpz.domain.eventHasMissingResponderKm
import com.yahpz.domain.reportingDocumentationStamp
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatNumber
import com.yahpz.domain.formatPlate
import com.yahpz.domain.formatTime
import com.yahpz.domain.eventMissingLeadDoneDetails
import com.yahpz.domain.leadKmPendingNote
import com.yahpz.domain.mineParticipationStamp
import com.yahpz.domain.participationStamp
import com.yahpz.domain.plateDigits
import com.yahpz.domain.visibleMyActiveIds
import com.yahpz.domain.INCOMPLETE_NOTICE_MARK
import com.yahpz.domain.SHIFT_LEAD_EVENTS_RECENT_CAPTION
import com.yahpz.domain.SHIFT_LEAD_EVENTS_RECENT_EMPTY
import com.yahpz.domain.SHOW_OLDER_UNIT_EVENTS_LABEL
import com.yahpz.domain.SHOW_OTHERS_CREATED_EVENTS_LABEL
import com.yahpz.domain.SHOW_SHIFT_EVENTS_LABEL
import com.yahpz.domain.keepShiftLeadRecentEvent
import com.yahpz.domain.shouldApplyShiftLeadRecentWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.yahpz.domain.filterShiftEvents
import com.yahpz.domain.IncompleteField
import com.yahpz.domain.LEAD_KM_VIEW_LABEL
import com.yahpz.domain.leadCardGapLabels
import com.yahpz.domain.managesUnit
import com.yahpz.domain.missingEventFields
import com.yahpz.domain.RESPONDERS_HELD_FOR_POLICE_ID_NOTE
import com.yahpz.domain.partitionIncompleteEvents
import com.yahpz.domain.responderCardShowsLeadKm
import com.yahpz.domain.shouldFilterUnitEventsToOwnCreated
import kotlinx.coroutines.launch

private const val FILTER_TITLE = "סינון"
private const val FILTER_RESET = "איפוס"
internal const val UNIT_EVENT_DETAIL_SHEET_TAG = "unit-event-detail-sheet"

private const val UNIT_EVENTS_SECTION_TITLE = "אירועי היחידה"
private const val MY_ACTIVE_REMOVE_SPOKEN = "הסרה מהפעילים"
private val ISRAEL_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")

private const val UNIT_EVENTS_CAPTION =
    "מציג 80 אירועים אחרונים - ניתן לחפש גם אירועים ישנים יותר"

private enum class ActiveDragSource { ACTIVE, CATALOG }

/** Everything the events tab does to the app, so the UI can run on plain state (and in tests). */
class UnitEventsActions(
    val reload: suspend () -> Unit,
    val setShowShiftEvents: (Boolean) -> Unit,
    val setShowOlderUnitEvents: (Boolean) -> Unit,
    val setShowOthersCreatedEvents: (Boolean) -> Unit,
    val addToMyActive: suspend (eventId: String) -> Unit,
    val removeFromMyActive: suspend (eventId: String) -> Unit,
    val openEditEvent: (eventId: String) -> Unit,
    val openFill: (eventId: String) -> Unit,
    val deleteEvent: suspend (eventId: String) -> String?,
    val showToast: (text: String, tone: StampTone) -> Unit,
    val loadDetailResponders: suspend (eventId: String) -> UnitEventDetailRespondersWrap,
    val setQuery: (query: String) -> Unit = {},
)

@Composable
fun UnitEventsScreen(app: AppModel, ui: AppUiState) {
    val actions = remember(app) {
        UnitEventsActions(
            reload = app::reloadUnitEvents,
            setShowShiftEvents = app::setShowShiftEvents,
            setShowOlderUnitEvents = app::setShowOlderUnitEvents,
            setShowOthersCreatedEvents = app::setShowOthersCreatedEvents,
            addToMyActive = app::addEventToMyActiveBoard,
            removeFromMyActive = app::removeEventFromMyActiveBoard,
            openEditEvent = app::openEditEvent,
            openFill = app::openFill,
            deleteEvent = app::deleteUnitEvent,
            showToast = app::showToast,
            setQuery = app::setUnitEventsQuery,
            loadDetailResponders = YahpazAPI::fetchUnitEventDetailResponders,
        )
    }
    UnitEventsContent(ui = ui, actions = actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitEventsContent(
    ui: AppUiState,
    actions: UnitEventsActions,
    today: LocalDate = LocalDate.now(ISRAEL_ZONE),
    layout: AdaptiveLayout = currentAdaptiveLayout(),
) {
    val scope = rememberCoroutineScope()
    val query = ui.unitEventsQuery
    var detail by remember { mutableStateOf<EventListItem?>(null) }
    var assignedEditBlocked by remember { mutableStateOf(false) }
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
        if (ui.userId != null && ui.unitEvents.isEmpty()) actions.reload()
    }

    fun tryOpenEdit(event: EventListItem): Boolean {
        if (event.blocksAssignedVolunteerEdit(ui.userId)) {
            assignedEditBlocked = true
            return false
        }
        if (isEventEditAgeLocked(event.createdAt, ui.roles)) {
            actions.showToast(EVENT_EDIT_LOCKED_TOOLTIP, StampTone.PENDING)
            return false
        }
        actions.openEditEvent(event.id)
        return true
    }

    var pendingBoardIds by remember { mutableStateOf(setOf<String>()) }
    var filtersOpen by remember { mutableStateOf(false) }
    val filters = scopeFilters(ui, actions)
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
    val catalog = if (trimmed.isEmpty()) {
        (ui.unitEvents.filter { it.id !in activeVisibleIds } + hiddenAuto.filter { d ->
            ui.unitEvents.none { it.id == d.id }
        })
    } else {
        (ui.unitEvents + hiddenAuto).distinctBy { it.id }
            .filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
    }.filter { it.id !in activeVisibleIds }
        .let { rows -> filterShiftEvents(rows, ui.showShiftEvents) { it.origin } }
        .sortedByDescending { it.eventDate }
    val applyRecentWindow = shouldApplyShiftLeadRecentWindow(ui.roles) &&
        !ui.showOlderUnitEvents &&
        trimmed.isEmpty()
    val events = if (!applyRecentWindow) {
        catalog
    } else {
        val now = Instant.now()
        catalog.filter { event ->
            keepShiftLeadRecentEvent(
                createdAt = event.createdAt,
                now = now,
                viewerId = viewerId,
                shiftLeadId = event.shiftLeadId,
                secondaryLeadIds = event.secondaryLeads.map { it.userId },
                incomplete = event.asIncompleteSnapshot(),
            )
        }
    }
    val catalogCaption = if (applyRecentWindow) SHIFT_LEAD_EVENTS_RECENT_CAPTION else UNIT_EVENTS_CAPTION
    val (incompleteEvents, restEvents) = partitionIncompleteEvents(events) { it.asIncompleteSnapshot() }

    fun dismissFromActive(eventId: String) {
        if (eventId in pendingBoardIds) return
        pendingBoardIds = pendingBoardIds + eventId
        scope.launch {
            try {
                actions.removeFromMyActive(eventId)
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
                actions.addToMyActive(eventId)
            } finally {
                pendingBoardIds = pendingBoardIds - eventId
            }
        }
    }

    val detailPane: (@Composable () -> Unit)? = detail?.let { event ->
        {
            UnitEventDetailBody(
                event = event,
                ui = ui,
                actions = actions,
                tryOpenEdit = ::tryOpenEdit,
                onClose = { detail = null },
            )
        }
    }
    ListDetailLayout(layout = layout, detailPane = detailPane, onCloseDetail = { detail = null }) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    actions.reload()
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
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                UnitEventsHeader(
                    query = query,
                    onQueryChange = actions.setQuery,
                    activeFilterCount = filters.count { it.selected },
                    onOpenFilters = { filtersOpen = true },
                )
                when {
                    ui.unitEventsFailed -> EmptyState(
                        title = UNIT_EVENTS_LOAD_FAILED,
                        actionTitle = "רענון",
                        onAction = { scope.launch { actions.reload() } },
                    )
                    ui.unitEventsLoading && ui.unitEvents.isEmpty() && ui.myActiveUnitEvents.isEmpty() ->
                        LoadingBlock("טוען אירועים…")
                    else -> Column(
                        modifier = Modifier.verticalScroll(
                            rememberScrollState(),
                            enabled = draggingId == null,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { activeDropZone = it.boundsInRoot() }
                                .then(dropZoneModifier(armed = dragSource == ActiveDragSource.CATALOG, over = overActiveDrop)),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SectionHeading(
                                title = MY_ACTIVE_EVENTS_TITLE,
                                count = activeEvents.size,
                                // Drag hints only while dragging; the buttons on each card do the same job.
                                subtitle = when {
                                    dragSource == ActiveDragSource.CATALOG && overActiveDrop -> MY_ACTIVE_DROP_TO_ADD
                                    dragSource == ActiveDragSource.CATALOG -> MY_ACTIVE_DRAG_TO_ADD
                                    activeEvents.isEmpty() -> MY_ACTIVE_EVENTS_EMPTY
                                    else -> null
                                },
                                subtitleAccent = overActiveDrop,
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
                                    viewerId = viewerId,
                                    today = today,
                                    dragging = draggingId == event.id,
                                    dragOffset = if (draggingId == event.id) dragOffset else Offset.Zero,
                                    boardActionAdds = false,
                                    boardActionEnabled = event.id !in pendingBoardIds,
                                    // A locked row keeps its button and says why on tap, instead of a hint line.
                                    boardActionMuted = !canRemove,
                                    onBoardAction = {
                                        if (canRemove) {
                                            dismissFromActive(event.id)
                                        } else {
                                            actions.showToast(MY_ACTIVE_REMOVE_LOCKED, StampTone.PENDING)
                                        }
                                    },
                                    onOpen = { tryOpenEdit(event) },
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
                        Spacer(Modifier.height(24.dp))
                        // One list: the unit's recent events, with ones missing details pinned first.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { catalogDropZone = it.boundsInRoot() }
                                .then(dropZoneModifier(armed = dragSource == ActiveDragSource.ACTIVE, over = overCatalogDrop)),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SectionHeading(
                                title = UNIT_EVENTS_SECTION_TITLE,
                                count = events.size,
                                subtitle = when {
                                    dragSource == ActiveDragSource.ACTIVE && overCatalogDrop -> "שחררו כאן להסרה מהפעילים"
                                    dragSource == ActiveDragSource.ACTIVE -> "גררו לכאן להסרה מהפעילים"
                                    events.isNotEmpty() -> catalogCaption
                                    else -> null
                                },
                                subtitleAccent = overCatalogDrop,
                            )
                            if (events.isNotEmpty()) {
                                (incompleteEvents + restEvents).forEach { event ->
                                    CatalogEventRow(
                                        event = event,
                                        viewerId = viewerId,
                                        today = today,
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
                                Spacer(Modifier.height(48.dp))
                            } else {
                                val olderHidden = applyRecentWindow && catalog.isNotEmpty()
                                EmptyState(
                                    title = when {
                                        trimmed.isNotEmpty() -> "לא נמצאו אירועים תואמים"
                                        olderHidden -> SHIFT_LEAD_EVENTS_RECENT_EMPTY
                                        else -> "אין אירועים להצגה"
                                    },
                                    // Says what this list covers, so an empty list does not read as broken.
                                    caption = if (trimmed.isEmpty()) catalogCaption else null,
                                    actionTitle = when {
                                        trimmed.isNotEmpty() -> "ניקוי חיפוש"
                                        olderHidden -> SHOW_OLDER_UNIT_EVENTS_LABEL
                                        else -> null
                                    },
                                    onAction = when {
                                        trimmed.isNotEmpty() -> ({ actions.setQuery("") })
                                        olderHidden -> ({ actions.setShowOlderUnitEvents(true) })
                                        else -> null
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(88.dp))
                    }
                }
            }
        }
    }

    if (!layout.listDetail) {
        detail?.let { event ->
            AdaptiveBottomSheet(
                onDismissRequest = { detail = null },
                modifier = Modifier.testTag(UNIT_EVENT_DETAIL_SHEET_TAG),
            ) {
                UnitEventDetailBody(
                    event = event,
                    ui = ui,
                    actions = actions,
                    tryOpenEdit = ::tryOpenEdit,
                    onClose = { detail = null },
                )
            }
        }
    }

    if (assignedEditBlocked) {
        AssignedVolunteerEditBlockedSheet(onDismiss = { assignedEditBlocked = false })
    }

    if (filtersOpen) {
        UnitEventsFilterSheet(filters = filters, onDismiss = { filtersOpen = false })
    }
}

/** The opened event's ledger, responders and actions. Shown in a sheet on phones and in the detail pane on wide screens. */
@Composable
private fun UnitEventDetailBody(
    event: EventListItem,
    ui: AppUiState,
    actions: UnitEventsActions,
    tryOpenEdit: (EventListItem) -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val current = ui.unitEvents.firstOrNull { it.id == event.id }
        ?: ui.myActiveUnitEvents.firstOrNull { it.id == event.id }
        ?: ui.myActivePinnedEvents.firstOrNull { it.id == event.id }
        ?: event
    val mine = ui.userId?.let { current.ownParticipation(it) }
    val stamp = unitEventStamp(current, ui.userId)
    val leadEntered = stamp.label == LEAD_ENTERED_LABEL
    var expandedResponderIds by remember(current.id) { mutableStateOf(setOf<String>()) }
    var detailRows by remember(current.id) { mutableStateOf<UnitEventDetailRespondersWrap?>(null) }
    var confirmDelete by remember(current.id) { mutableStateOf(false) }
    var deleting by remember(current.id) { mutableStateOf(false) }
    val loadedDetail = detailRows
    val detailResponders = loadedDetail?.responders

    LaunchedEffect(current.id) {
        expandedResponderIds = emptySet()
        detailRows = runCatching {
            actions.loadDetailResponders(current.id)
        }.getOrNull()
    }

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
        val missingLead = missingEventFields(current.asIncompleteSnapshot())
        LedgerRow("תאריך", formatDate(current.eventDate))
        LedgerRow(
            "מספר אירוע",
            current.policeEventId.orEmpty(),
            missing = IncompleteField.POLICE_EVENT_ID in missingLead,
        )
        run {
            val callsign = resolvePatrolCallsign(
                current.patrolCallsignPrefix,
                current.patrolCallsignNumber,
                current.patrolCallsign,
            )
            LedgerRow(
                "אוק",
                formatPatrolCallsign(callsign.prefix, callsign.number),
                missing = IncompleteField.PATROL_CALLSIGN in missingLead,
            )
        }
        LedgerRow(
            "שעת התחלה",
            formatTime(current.startedAt).orEmpty(),
            missing = current.startedAt.isNullOrBlank(),
        )
        LedgerRow(
            "שעת סיום",
            formatTime(current.endedAt).orEmpty(),
            missing = current.endedAt.isNullOrBlank(),
        )
        LedgerRow(
            "סוג אירוע",
            current.typeLabel,
            missing = IncompleteField.EVENT_TYPE in missingLead,
        )
        LedgerRow(
            "כביש",
            current.road?.name.orEmpty(),
            missing = IncompleteField.ROAD in missingLead,
        )
        LedgerRow(
            "מיקום",
            current.location.orEmpty(),
            missing = IncompleteField.LOCATION in missingLead,
        )
        LedgerRow("נת״צ", if (current.busLane) "כן" else "לא")
        if (current.origin == "shift" && loadedDetail != null) {
            LedgerRow("מספרי כלי רכב", treatedPlatesLabel(shiftEventPlates(loadedDetail)))
        }
        LedgerRow("סטטוס", stamp.label)
        EventLeadLedgerRows(current.shiftLead, current.secondaryLeads)
        Text("מתנדבים (${current.responders.size})", style = TypeScale.section, color = FieldTheme.textPrimary)
        if (!current.releasedToResponders()) {
            Text(
                RESPONDERS_HELD_FOR_POLICE_ID_NOTE,
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
        }
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
                    shiftBorn = current.isShiftBorn,
                    showLeadKm = responderCardShowsLeadKm(managesUnit(ui.roles)),
                    missingLeadDetails = current.origin != "shift" &&
                        eventMissingLeadDoneDetails(current.endedAt),
                    isViewer = row.responderId == ui.userId,
                    leadEntered = leadEntered,
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
        if (mine != null && current.releasedToResponders()) {
            current.fillCtaLabel(ui.userId)?.let { label ->
                PrimaryButton(title = label, onClick = {
                    val id = current.id
                    onClose()
                    actions.openFill(id)
                })
            }
        }
        if (ui.canManageUnit) {
            val ageLocked = isEventEditAgeLocked(current.createdAt, ui.roles)
            PrimaryButton(
                title = EVENT_EDIT_TITLE,
                enabled = !ageLocked,
                onClick = {
                    if (tryOpenEdit(current)) onClose()
                },
            )
            if (ageLocked) {
                Text(
                    EVENT_EDIT_LOCKED_TOOLTIP,
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
        }
        if (canDeleteUnassignedEvent(
                ui.canManageUnit,
                ui.canAdmin,
                ui.userId,
                current.shiftLeadId,
            )) {
            if (confirmDelete) {
                Text(
                    if (current.responders.isNotEmpty()) {
                        EVENT_DELETE_CONFIRM_WITH_RESPONDERS
                    } else {
                        EVENT_DELETE_CONFIRM
                    },
                    style = TypeScale.body,
                    color = FieldTheme.textSecondary,
                )
                GhostButton(
                    title = EVENT_DELETE_ACTION,
                    danger = true,
                    enabled = !deleting,
                    onClick = {
                        val id = current.id
                        scope.launch {
                            deleting = true
                            val error = actions.deleteEvent(id)
                            deleting = false
                            if (error != null) {
                                actions.showToast(error, StampTone.PENDING)
                                confirmDelete = false
                            } else {
                                onClose()
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
        TextButton(onClick = { onClose() }, modifier = Modifier.align(Alignment.End)) {
            Text("סגירה", color = FieldTheme.accent)
        }
    }
}

/** Outline that marks a board as a drop target while a card is dragged. */
private fun dropZoneModifier(armed: Boolean, over: Boolean): Modifier {
    if (!armed) return Modifier
    val shape = RoundedCornerShape(8.dp)
    return Modifier
        .border(width = 2.dp, color = if (over) FieldTheme.accent else FieldTheme.hairline, shape = shape)
        .background(if (over) FieldTheme.accentSubtle else FieldTheme.page, shape)
        .padding(8.dp)
}

/** Section title with its count, and one optional line under it. */
@Composable
private fun SectionHeading(title: String, count: Int, subtitle: String?, subtitleAccent: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            if (count > 0) "$title · $count" else title,
            style = TypeScale.section,
            color = FieldTheme.textPrimary,
        )
        subtitle?.let {
            Text(
                it,
                style = TypeScale.meta,
                color = if (subtitleAccent) FieldTheme.accent else FieldTheme.textSecondary,
            )
        }
    }
}

/** One scope toggle in the סינון sheet. Each one widens the list. */
private class ScopeFilter(val label: String, val selected: Boolean, val onChange: (Boolean) -> Unit)

/** The scope toggles this viewer's roles allow, in sheet order. */
private fun scopeFilters(ui: AppUiState, actions: UnitEventsActions): List<ScopeFilter> = buildList {
    add(ScopeFilter(SHOW_SHIFT_EVENTS_LABEL, ui.showShiftEvents, actions.setShowShiftEvents))
    if (shouldApplyShiftLeadRecentWindow(ui.roles)) {
        add(ScopeFilter(SHOW_OLDER_UNIT_EVENTS_LABEL, ui.showOlderUnitEvents, actions.setShowOlderUnitEvents))
    }
    if (shouldFilterUnitEventsToOwnCreated(ui.roles)) {
        add(ScopeFilter(SHOW_OTHERS_CREATED_EVENTS_LABEL, ui.showOthersCreatedEvents, actions.setShowOthersCreatedEvents))
    }
}

/** Pinned above the list: title, then search and סינון on one row. */
@Composable
private fun UnitEventsHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilterCount: Int,
    onOpenFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("אירועים", style = TypeScale.title, color = FieldTheme.textPrimary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f)) {
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "אירוע, כביש, מיקום או אחמ״ש",
                )
            }
            FilterButton(activeCount = activeFilterCount, onClick = onOpenFilters)
        }
    }
}

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val active = activeCount > 0
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            // Active reads as a soft filled (tonal) button; idle as an outlined one.
            .background(if (active) FieldTheme.accentSubtle else FieldTheme.raised)
            .then(if (active) Modifier else Modifier.border(1.dp, FieldTheme.strong, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .clearAndSetSemantics {
                contentDescription = if (active) "$FILTER_TITLE, $activeCount פעילים" else FILTER_TITLE
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.FilterList,
            contentDescription = null,
            tint = if (active) FieldTheme.accent else FieldTheme.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(FILTER_TITLE, style = TypeScale.label, color = if (active) FieldTheme.accent else FieldTheme.textPrimary)
        if (active) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(FieldTheme.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$activeCount", style = TypeScale.label, color = FieldTheme.textOnAccent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitEventsFilterSheet(filters: List<ScopeFilter>, onDismiss: () -> Unit) {
    AdaptiveBottomSheet(onDismissRequest = onDismiss, containerColor = FieldTheme.raised) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    FILTER_TITLE,
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (filters.any { it.selected }) {
                    TextButton(
                        onClick = { filters.filter { it.selected }.forEach { it.onChange(false) } },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(FILTER_RESET, style = TypeScale.label, color = FieldTheme.accent)
                    }
                }
            }
            filters.forEach { filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(
                            value = filter.selected,
                            role = Role.Switch,
                            onValueChange = filter.onChange,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        filter.label,
                        style = TypeScale.body,
                        color = FieldTheme.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    // The row owns the toggle, so the switch itself is not a second target.
                    Switch(
                        checked = filter.selected,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = FieldTheme.accent,
                            checkedThumbColor = FieldTheme.textOnAccent,
                            uncheckedTrackColor = FieldTheme.sunken,
                            uncheckedThumbColor = FieldTheme.textMuted,
                            uncheckedBorderColor = FieldTheme.strong,
                        ),
                    )
                }
            }
            PrimaryButton(title = "סגירה", onClick = onDismiss)
        }
    }
}

@Composable
private fun CatalogEventRow(
    event: EventListItem,
    viewerId: String?,
    today: LocalDate,
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
        viewerId = viewerId,
        today = today,
        dragging = draggingId == event.id,
        dragOffset = if (draggingId == event.id) dragOffset else Offset.Zero,
        boardActionAdds = true,
        boardActionEnabled = event.id !in pendingBoardIds,
        boardActionMuted = false,
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
    viewerId: String?,
    today: LocalDate,
    dragging: Boolean,
    dragOffset: Offset,
    boardActionAdds: Boolean,
    boardActionEnabled: Boolean,
    boardActionMuted: Boolean,
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
            viewerId = viewerId,
            today = today,
            enabled = !dragging,
            boardActionAdds = boardActionAdds,
            boardActionEnabled = boardActionEnabled && !dragging,
            boardActionMuted = boardActionMuted,
            onBoardAction = onBoardAction,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun UnitEventResponderRow(
    row: UnitEventDetailResponderRow,
    eventDate: String,
    /** One shared report: no per-responder stamp, plate, odometer, route or KM. */
    shiftBorn: Boolean,
    showLeadKm: Boolean,
    missingLeadDetails: Boolean,
    isViewer: Boolean,
    /** Viewer is the אחמ״ש and their part is complete: open volunteers read הוזן בהצלחה. */
    leadEntered: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val stamp = when {
        isViewer -> mineParticipationStamp(row.status, row.totalKm, missingLeadDetails)
        leadEntered && row.status != ParticipationStatus.DONE -> leadEnteredStamp(1)
        else -> participationStamp(row.status, false)
    }
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
                if (!shiftBorn) StampWithNote(
                    stamp,
                    note = if (isViewer) {
                        leadKmPendingNote(
                            row.status,
                            row.totalKm,
                            missingLeadDetails = missingLeadDetails,
                        )
                    } else {
                        null
                    },
                )
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
                if (showLeadKm && !shiftBorn) {
                    LedgerRow(
                        LEAD_KM_VIEW_LABEL,
                        row.totalKm?.let { "${formatNumber(it)} ק״מ" }.orEmpty(),
                        missing = row.totalKm == null,
                    )
                }
                LedgerRow("אמצעים", if (row.emergencyMeans) "כן" else "לא")
                val treated = treatedVehiclesLabel(row.treated)
                if (treated.isNotEmpty()) {
                    LedgerRow("רכבים שטופלו", treated)
                }
                if (!shiftBorn) {
                    val plates = treatedPlatesLabel(row.treatedPlates)
                    if (plates.isNotEmpty()) {
                        LedgerRow("מספרי כלי רכב", plates)
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

/**
 * One plate list for a shift-born event: the lead's event-keyed rows plus whatever
 * each responder logged from their own fill, which is keyed to the participation.
 */
private fun shiftEventPlates(detail: UnitEventDetailRespondersWrap): List<EventTreatedPlateRow> {
    val seen = mutableSetOf<String>()
    return (detail.sharedPlates + detail.responders.flatMap { it.treatedPlates })
        .filter { seen.add(plateDigits(it.plateNumber.orEmpty())) }
}

private fun treatedPlatesLabel(plates: List<EventTreatedPlateRow>): String =
    plates.mapNotNull { plate ->
        plate.plateNumber?.trim()?.takeIf { it.isNotEmpty() }?.let(::formatPlate)
    }.joinToString(", ")

/** The event's stamp as this viewer sees it: a lead whose part is done reads הוזן בהצלחה. */
private fun unitEventStamp(event: EventListItem, viewerId: String?): StampDescriptor = when {
    event.isCancelled -> cancelledStamp()
    event.isShiftBorn -> event.shiftBornStamp
    else -> {
        val snapshot = event.asIncompleteSnapshot()
        leadEnteredAwaitingStamp(snapshot, viewerId)
            ?: reportingDocumentationStamp(event.status, eventHasMissingResponderKm(snapshot))
    }
}

/**
 * Three short rows: title + status, when · where + one action, and a strip only
 * when the lead must act. Police id, leads, responders and attachments live in
 * the detail sheet / edit form.
 */
@Composable
private fun UnitEventRow(
    event: EventListItem,
    enabled: Boolean,
    boardActionAdds: Boolean,
    boardActionEnabled: Boolean,
    boardActionMuted: Boolean,
    onBoardAction: () -> Unit,
    onOpen: () -> Unit,
    viewerId: String?,
    today: LocalDate,
) {
    val stamp = unitEventStamp(event, viewerId)
    val cancelled = event.isCancelled
    val attention = if (cancelled) null else eventAttention(event)
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FieldTheme.raised)
            .border(1.dp, FieldTheme.hairline, shape)
            .clickable(enabled = enabled, onClick = onOpen)
            // The action's 48dp target supplies the bottom inset when there is no strip.
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (attention != null) 12.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                event.typeLabel.ifEmpty { "אירוע" },
                style = TypeScale.section,
                color = if (cancelled) FieldTheme.textSecondary else FieldTheme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Nudged to sit on the title's first line.
            StatusPill(stamp, modifier = Modifier.padding(top = 2.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                listOfNotNull(
                    eventWhenLabel(event.eventDate, event.startedAt, today),
                    event.road?.name?.takeIf { it.isNotBlank() },
                    event.location?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = TypeScale.meta,
                color = if (cancelled) FieldTheme.textMuted else FieldTheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            // Offset toward the end so the glyph lines up with the pill above, not the touch box.
            IconButton(
                onClick = onBoardAction,
                enabled = boardActionEnabled,
                interactionSource = interaction,
                modifier = Modifier.offset(x = 12.dp),
            ) {
                Icon(
                    if (boardActionAdds) Icons.Outlined.AddCircleOutline else Icons.Outlined.RemoveCircleOutline,
                    contentDescription = if (boardActionAdds) MY_ACTIVE_ADD else MY_ACTIVE_REMOVE_SPOKEN,
                    // Neutral at rest so the card's title leads; "+" takes the accent only while pressed.
                    tint = when {
                        !boardActionEnabled || boardActionMuted || cancelled -> FieldTheme.textSecondary.copy(alpha = 0.38f)
                        boardActionAdds && pressed -> FieldTheme.accent
                        else -> FieldTheme.textSecondary
                    },
                )
            }
        }
        attention?.let { EventAttentionStrip(it) }
    }
}

/** Calm status: the stamp's label in the app's label style on a soft tint, no border. */
@Composable
private fun StatusPill(stamp: StampDescriptor, modifier: Modifier = Modifier) {
    val (ink, tint) = when (stamp.tone) {
        StampTone.DONE -> FieldTheme.done to FieldTheme.doneTint
        StampTone.PARTIAL -> FieldTheme.partialOnTint to FieldTheme.partialTint
        StampTone.PENDING -> FieldTheme.accent to FieldTheme.accentSubtle
        StampTone.DRAFT -> FieldTheme.textSecondary to FieldTheme.sunken
        StampTone.ALERT -> FieldTheme.alert to FieldTheme.alertTint
    }
    Text(
        stamp.label,
        style = TypeScale.label,
        color = ink,
        maxLines = 1,
        modifier = modifier
            .background(tint, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * Card date for this list only: "היום 07:40", "אתמול 22:30", "15.11 · 18:10",
 * with the year only when it is not this year. The shared formatDate stays as is.
 */
internal fun eventWhenLabel(eventDate: String, startedAt: String?, today: LocalDate): String {
    val day = runCatching { LocalDate.parse(eventDate.take(10)) }.getOrNull()
    val time = formatTime(startedAt)
    val dayLabel = when {
        day == null -> eventDate
        day == today -> "היום"
        day == today.minusDays(1) -> "אתמול"
        day.year == today.year -> "${twoDigits(day.dayOfMonth)}.${twoDigits(day.monthValue)}"
        else -> formatDate(eventDate)
    }
    if (time == null) return dayLabel
    return if (day == today || day == today.minusDays(1)) "$dayLabel $time" else "$dayLabel · $time"
}

private fun twoDigits(n: Int) = n.toString().padStart(2, '0')

private const val FROZEN_SHORT_LABEL = "מוקפא, ממתין לאישור מנהל"

private class EventAttention(val text: String, val spoken: String, val frozen: Boolean)

/**
 * The strip means "you need to act": frozen first, then gaps only the lead can
 * close. Responder km and hours are left to the status pill.
 */
private fun eventAttention(event: EventListItem): EventAttention? {
    val gaps = leadCardGapLabels(event.asIncompleteSnapshot())
    val frozenTip = event.freeze.tooltipHe
    if (gaps.isEmpty() && frozenTip == null) return null
    val missing = gaps.takeIf { it.isNotEmpty() }?.joinToString(", ")
    return EventAttention(
        text = listOfNotNull(
            frozenTip?.let { FROZEN_SHORT_LABEL },
            missing?.let { "$INCOMPLETE_NOTICE_MARK $it" },
        ).joinToString(" · "),
        spoken = listOfNotNull(frozenTip, missing?.let { "חסרים: $it" }).joinToString(". "),
        frozen = frozenTip != null,
    )
}

@Composable
private fun EventAttentionStrip(attention: EventAttention) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.partialTint, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = attention.spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (attention.frozen) Icons.Outlined.AcUnit else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = FieldTheme.partialOnTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            attention.text,
            style = TypeScale.meta,
            color = FieldTheme.partialOnTint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
