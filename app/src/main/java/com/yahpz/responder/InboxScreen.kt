package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yahpz.domain.FUEL_NOTE
import com.yahpz.domain.MINE_LOGGED_EMPTY_TITLE
import com.yahpz.domain.MINE_LOGGED_TAB_LABEL
import com.yahpz.domain.MINE_PENDING_EMPTY_CAPTION
import com.yahpz.domain.MINE_PENDING_EMPTY_TITLE
import com.yahpz.domain.MINE_PENDING_EMPTY_VIEW_LOGGED
import com.yahpz.domain.MineListEvent
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.cancelledStamp
import com.yahpz.domain.formatDate
import com.yahpz.domain.fuelNoteNeeded
import com.yahpz.domain.israelToday
import com.yahpz.domain.mineEventMatchesQuery
import com.yahpz.domain.mineFillCtaLabel
import com.yahpz.domain.mineLoggedNoResultsTitle
import com.yahpz.domain.minePendingTabLabel
import com.yahpz.domain.openMineSummary
import com.yahpz.domain.participationStamp
import com.yahpz.domain.partitionMineList
import com.yahpz.domain.shiftGroupPendingCaption
import com.yahpz.domain.shiftGroupShouldStartOpen
import kotlinx.coroutines.launch

private enum class MineInboxTab { PENDING, LOGGED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(MineInboxTab.PENDING) }
    var loggedQuery by remember { mutableStateOf("") }
    var windowsLoaded by remember { mutableIntStateOf(1) }
    var detail by remember { mutableStateOf<EventListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.events.isEmpty()) app.reloadEvents()
    }

    val pending = ui.events.filter { event ->
        val userId = ui.userId ?: return@filter false
        event.ownParticipation(userId) != ParticipationStatus.DONE
    }.sortedByDescending { it.eventDate }

    val loggedWindow = if (ui.userId == null) {
        com.yahpz.domain.MineListSections<MineListEvent>(emptyList(), emptyList(), false)
    } else {
        partitionMineList(
            ui.events.map {
                MineListEvent(it.id, it.eventDate, it.ownParticipation(ui.userId) ?: ParticipationStatus.PENDING)
            },
            israelToday(),
            windowsLoaded,
        )
    }
    val loggedIds = loggedWindow.logged.map { it.id }.toSet()
    val logged = ui.events.filter { it.id in loggedIds }.sortedByDescending { it.eventDate }
    val query = loggedQuery.trim()
    val filteredLogged = if (query.isEmpty()) logged else logged.filter { mineEventMatchesQuery(it.searchFields, query) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadEvents()
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
            Text("האירועים שלי", style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(
                openMineSummary(pending.size, !ui.eventsLoading),
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
            )
            if (fuelNoteNeeded(pending.size)) {
                Text(FUEL_NOTE, style = TypeScale.body, color = FieldTheme.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip(minePendingTabLabel(pending.size), tab == MineInboxTab.PENDING) { tab = MineInboxTab.PENDING }
                TabChip(MINE_LOGGED_TAB_LABEL, tab == MineInboxTab.LOGGED) { tab = MineInboxTab.LOGGED }
            }
            when {
                ui.eventsFailed -> EmptyState(
                    title = "טעינת האירועים נכשלה. בדקו את החיבור ונסו שוב.",
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadEvents() } },
                )
                ui.eventsLoading && ui.events.isEmpty() -> BoxCentered("טוען את הדיווחים שלך…")
                tab == MineInboxTab.PENDING -> PendingList(
                    pending = pending,
                    userId = ui.userId,
                    loggedIsEmpty = logged.isEmpty(),
                    onFill = app::openFill,
                    onOpen = { detail = it },
                    onViewLogged = { tab = MineInboxTab.LOGGED },
                )
                else -> LoggedList(
                    query = loggedQuery,
                    onQuery = { loggedQuery = it },
                    items = filteredLogged,
                    hasMore = loggedWindow.hasMoreLogged,
                    onOpen = { detail = it },
                    onMore = { windowsLoaded += 1 },
                    onClear = { loggedQuery = "" },
                )
            }
        }
    }

    detail?.let { event ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(Modifier.padding(16.dp)) {
                Text("פרטי האירוע", style = TypeScale.section, color = FieldTheme.textPrimary)
                Spacer(Modifier.height(12.dp))
                LedgerRow("תאריך", formatDate(event.eventDate))
                LedgerRow("מספר אירוע", event.policeEventId.orEmpty())
                LedgerRow("סוג אירוע", event.typeLabel)
                LedgerRow("כביש", event.road?.name.orEmpty())
                LedgerRow("מיקום", event.location.orEmpty())
                LedgerRow("אחמ״ש", event.shiftLead?.display.orEmpty())
                TextButton(onClick = { detail = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = TypeScale.label,
        color = if (selected) FieldTheme.accent else FieldTheme.textSecondary,
        modifier = Modifier
            .background(if (selected) FieldTheme.accentSubtle else FieldTheme.raised, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) FieldTheme.accent else FieldTheme.strong, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 36.dp),
    )
}

@Composable
private fun PendingList(
    pending: List<EventListItem>,
    userId: String?,
    loggedIsEmpty: Boolean,
    onFill: (String) -> Unit,
    onOpen: (EventListItem) -> Unit,
    onViewLogged: () -> Unit,
) {
    if (pending.isEmpty()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            EmptyState(
                title = MINE_PENDING_EMPTY_TITLE,
                caption = MINE_PENDING_EMPTY_CAPTION,
                actionTitle = if (loggedIsEmpty) null else MINE_PENDING_EMPTY_VIEW_LOGGED,
                onAction = if (loggedIsEmpty) null else onViewLogged,
            )
        }
        return
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        pendingBlocks(pending).forEach { block ->
            when (block) {
                is InboxBlock.Single -> EventCard(block.event, userId, onFill, onOpen)
                is InboxBlock.Shift -> ShiftGroup(
                    title = block.title,
                    caption = shiftGroupPendingCaption(block.events.size),
                    startsOpen = shiftGroupShouldStartOpen(block.events.size),
                ) {
                    block.events.forEach { EventCard(it, userId, onFill, onOpen) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LoggedList(
    query: String,
    onQuery: (String) -> Unit,
    items: List<EventListItem>,
    hasMore: Boolean,
    onOpen: (EventListItem) -> Unit,
    onMore: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("חיפוש לפי מספר אירוע, כביש, מיקום", style = TypeScale.body) },
            textStyle = TypeScale.body,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldTheme.raised,
                unfocusedContainerColor = FieldTheme.raised,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, FieldTheme.strong, RoundedCornerShape(4.dp)),
        )
        Text("תועדו · 30 יום אחרונים", style = TypeScale.caption, color = FieldTheme.textMuted)
        if (items.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) MINE_LOGGED_EMPTY_TITLE else mineLoggedNoResultsTitle(query.trim()),
                actionTitle = if (query.isBlank()) null else "ניקוי חיפוש",
                onAction = if (query.isBlank()) null else onClear,
            )
        } else {
            Column(
                modifier = Modifier
                    .background(FieldTheme.raised, RoundedCornerShape(8.dp))
                    .border(1.dp, FieldTheme.hairline, RoundedCornerShape(8.dp))
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(event) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                event.typeLabel.ifEmpty { "אירוע" },
                                style = TypeScale.bodyStrong,
                                color = FieldTheme.textPrimary,
                            )
                            Text(
                                "${formatDate(event.eventDate)} · ${event.policeEventId.orEmpty()}",
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                        }
                        StampChip(participationStamp(ParticipationStatus.DONE, true))
                    }
                }
            }
            if (hasMore) {
                GhostButton(title = "הצג 30 יום נוספים", onClick = onMore)
            }
        }
    }
}

@Composable
private fun EventCard(
    event: EventListItem,
    userId: String?,
    onFill: (String) -> Unit,
    onOpen: (EventListItem) -> Unit,
) {
    val mine = userId?.let { event.ownParticipation(it) } ?: ParticipationStatus.PENDING
    val stamp = if (event.isCancelled) cancelledStamp() else participationStamp(mine, true)
    FieldCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(event.typeLabel.ifEmpty { "אירוע" }, style = TypeScale.section, color = FieldTheme.textPrimary)
                Text(
                    listOf(formatDate(event.eventDate), event.policeEventId).filter { !it.isNullOrEmpty() }.joinToString(" · "),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            StampChip(stamp)
        }
        Spacer(Modifier.height(12.dp))
        mineFillCtaLabel(mine)?.let { label ->
            PrimaryButton(title = label, onClick = { onFill(event.id) })
            Spacer(Modifier.height(8.dp))
        }
        GhostButton(title = "פרטי האירוע", onClick = { onOpen(event) })
    }
}

@Composable
private fun ShiftGroup(
    title: String,
    caption: String,
    startsOpen: Boolean,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(startsOpen) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.clickable { open = !open }.fillMaxWidth()) {
            Text(title, style = TypeScale.label, color = FieldTheme.textSecondary)
            Text(caption, style = TypeScale.body, color = FieldTheme.textSecondary)
        }
        if (open) content()
    }
}

@Composable
private fun BoxCentered(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = FieldTheme.accent)
        Spacer(Modifier.height(12.dp))
        Text(text, style = TypeScale.body, color = FieldTheme.textSecondary)
    }
}

private sealed class InboxBlock {
    data class Single(val event: EventListItem) : InboxBlock()
    data class Shift(val title: String, val events: List<EventListItem>) : InboxBlock()
}

private fun pendingBlocks(items: List<EventListItem>): List<InboxBlock> {
    val blocks = mutableListOf<InboxBlock>()
    var index = 0
    while (index < items.size) {
        val event = items[index]
        if (event.origin == "shift" && event.shiftId != null) {
            val grouped = mutableListOf(event)
            index += 1
            while (index < items.size && items[index].origin == "shift" && items[index].shiftId == event.shiftId) {
                grouped += items[index]
                index += 1
            }
            blocks += InboxBlock.Shift(event.shiftGroupTitle, grouped)
        } else {
            blocks += InboxBlock.Single(event)
            index += 1
        }
    }
    return blocks
}
