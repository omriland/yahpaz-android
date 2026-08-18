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
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.cancelledStamp
import com.yahpz.domain.EVENT_CANCELLED_LABEL
import com.yahpz.domain.EVENT_EDIT_TITLE
import com.yahpz.domain.eventStamp
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.formatDate
import com.yahpz.domain.mineFillCtaLabel
import com.yahpz.domain.participationStamp
import com.yahpz.domain.UNIT_EVENTS_LOAD_FAILED
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitEventsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<EventListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var cancelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.unitEvents.isEmpty()) app.reloadUnitEvents()
    }

    val trimmed = query.trim()
    val events = if (trimmed.isEmpty()) {
        ui.unitEvents
    } else {
        ui.unitEvents.filter { fieldsMatchQuery(it.unitSearchFields, trimmed) }
    }.sortedByDescending { it.eventDate }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("אירועי היחידה", style = TypeScale.title, color = FieldTheme.textPrimary)
                TextButton(onClick = { app.setToolsDestination(ToolsDestination.NEW_EVENT) }) {
                    Text(NEW_EVENT_TITLE, style = TypeScale.bodyStrong, color = FieldTheme.accent)
                }
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
                ui.unitEventsLoading && ui.unitEvents.isEmpty() -> LoadingBlock("טוען אירועי יחידה…")
                events.isEmpty() -> EmptyState(
                    title = if (trimmed.isEmpty()) "אין אירועים להצגה" else "לא נמצאו אירועים תואמים",
                    actionTitle = if (trimmed.isEmpty()) null else "ניקוי חיפוש",
                    onAction = if (trimmed.isEmpty()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${events.size} אירועים אחרונים ביחידה",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                    events.forEach { event -> UnitEventRow(event) { detail = event } }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    detail?.let { event ->
        // Re-read from state so a cancel toggle refreshes the open sheet.
        val current = ui.unitEvents.firstOrNull { it.id == event.id } ?: event
        val mine = ui.userId?.let { current.ownParticipation(it) }
        val stamp = if (current.isCancelled) cancelledStamp() else eventStamp(current.status)
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
                    Text("פרטי האירוע", style = TypeScale.section, color = FieldTheme.textPrimary)
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
                }
                current.responders.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.profile?.display ?: "כונן",
                            style = TypeScale.body,
                            color = FieldTheme.textPrimary,
                        )
                        StampChip(participationStamp(row.status, row.responderId == ui.userId))
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
                    cancelError?.let {
                        Text(it, style = TypeScale.caption, color = FieldTheme.alert)
                    }
                    PrimaryButton(
                        title = EVENT_EDIT_TITLE,
                        onClick = {
                            val id = current.id
                            detail = null
                            app.openEditEvent(id)
                        },
                    )
                    FormCheckbox(
                        label = EVENT_CANCELLED_LABEL,
                        checked = current.isCancelled,
                        enabled = !cancelling && (!current.isCancelled || ui.canAdmin),
                        onCheckedChange = { next ->
                            scope.launch {
                                cancelling = true
                                cancelError = app.setEventCancelled(current.id, next)
                                cancelling = false
                            }
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
private fun UnitEventRow(event: EventListItem, onOpen: () -> Unit) {
    val stamp = if (event.isCancelled) cancelledStamp() else eventStamp(event.status)
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
                Text(
                    event.typeLabel.ifEmpty { "אירוע" },
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(
                    listOfNotNull(
                        formatDate(event.eventDate),
                        event.policeEventId?.takeIf { it.isNotEmpty() },
                        event.road?.name?.takeIf { it.isNotEmpty() },
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
