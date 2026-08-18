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
import com.yahpz.domain.REPORT_FAILED_TITLE
import com.yahpz.domain.REPORT_LOADING_TITLE
import com.yahpz.domain.REPORT_LOAD_ACTION
import com.yahpz.domain.REPORT_RANGE_ERROR
import com.yahpz.domain.ReportRow
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.filterReportRows
import com.yahpz.domain.isValidReportRange
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.reportRowSummary
import com.yahpz.domain.reportSpec
import com.yahpz.domain.returnDateToInput
import kotlinx.coroutines.launch

/**
 * One screen for every report in the library: date range, search, rows. Report kinds
 * differ only in copy and in how the loader collapses source rows into [ReportRow].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val spec = remember(ui.reportKind) { reportSpec(ui.reportKind) }
    val defaults = remember(ui.reportKind) { app.defaultReportRange(ui.reportKind) }
    var from by remember(ui.reportKind) {
        mutableStateOf(returnDateToInput(ui.reportFrom ?: defaults.first))
    }
    var to by remember(ui.reportKind) {
        mutableStateOf(returnDateToInput(ui.reportTo ?: defaults.second))
    }
    var query by remember(ui.reportKind) { mutableStateOf("") }
    var rangeError by remember(ui.reportKind) { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var actionRow by remember(ui.reportKind) { mutableStateOf<ReportRow?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var applying by remember { mutableStateOf(false) }

    suspend fun load() {
        // Reports without a range still send the defaults; their loader ignores them.
        val fromIso = normalizeReturnDate(from) ?: defaults.first.takeUnless { spec.hasDateRange }
        val toIso = normalizeReturnDate(to) ?: defaults.second.takeUnless { spec.hasDateRange }
        if (fromIso == null || toIso == null || !isValidReportRange(fromIso, toIso)) {
            rangeError = REPORT_RANGE_ERROR
            return
        }
        rangeError = null
        app.reloadReport(fromIso, toIso)
    }

    LaunchedEffect(ui.reportKind) {
        if (ui.reportRows.isEmpty()) load()
    }

    val trimmed = query.trim()
    val rows = filterReportRows(ui.reportRows, trimmed)

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                load()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolsBackRow(spec.title, onBack)
            Text(spec.includes, style = TypeScale.caption, color = FieldTheme.textMuted)
            if (spec.hasDateRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReturnDateField(
                        label = "מתאריך",
                        value = from,
                        onValueChange = { from = it },
                        modifier = Modifier.weight(1f),
                    )
                    ReturnDateField(
                        label = "עד תאריך",
                        value = to,
                        onValueChange = { to = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                rangeError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
            }
            PrimaryButton(
                title = REPORT_LOAD_ACTION,
                busy = ui.reportLoading,
                onClick = { scope.launch { load() } },
            )
            if (ui.reportRows.isNotEmpty()) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = spec.searchPlaceholder,
                )
            }
            when {
                ui.reportFailed -> EmptyState(
                    title = REPORT_FAILED_TITLE,
                    actionTitle = "רענון",
                    onAction = { scope.launch { load() } },
                )
                ui.reportLoading && ui.reportRows.isEmpty() -> LoadingBlock(REPORT_LOADING_TITLE)
                ui.reportRows.isEmpty() -> EmptyState(title = spec.emptyTitle)
                rows.isEmpty() -> EmptyState(
                    title = "לא נמצאו שורות תואמות",
                    actionTitle = "ניקוי חיפוש",
                    onAction = { query = "" },
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        reportRowSummary(rows.size),
                        style = TypeScale.body,
                        color = FieldTheme.textSecondary,
                    )
                    rows.forEach { row ->
                        ReportRowCard(
                            row = row,
                            // A row that offers a write opens its confirm sheet instead of the fill.
                            onOpen = if (row.actionId != null) {
                                { actionError = null; actionRow = row }
                            } else {
                                row.eventId
                                    ?.takeIf { ui.ownsEventParticipation(it) }
                                    ?.let { eventId -> { app.openFill(eventId) } }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    actionRow?.let { row ->
        val actionId = row.actionId ?: return@let
        ModalBottomSheet(onDismissRequest = { actionRow = null }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(row.title, style = TypeScale.section, color = FieldTheme.textPrimary)
                if (row.subtitle.isNotEmpty()) {
                    Text(row.subtitle, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
                row.trailing?.let {
                    Text(it, style = TypeScale.numeric, color = FieldTheme.textPrimary)
                }
                row.actionConfirm?.let {
                    Text(it, style = TypeScale.body, color = FieldTheme.textSecondary)
                }
                actionError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = row.actionTitle.orEmpty(),
                    busy = applying,
                    onClick = {
                        scope.launch {
                            applying = true
                            val error = app.applyReportRowAction(actionId)
                            applying = false
                            actionError = error
                            if (error == null) actionRow = null
                        }
                    },
                )
                TextButton(onClick = { actionRow = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

/** A row is tappable only when the viewer is the responder who still owns the documentation. */
private fun AppUiState.ownsEventParticipation(eventId: String): Boolean {
    val viewer = userId ?: return false
    val event = (unitEvents + events).firstOrNull { it.id == eventId } ?: return false
    return event.ownParticipation(viewer) != null
}

@Composable
private fun ReportRowCard(row: ReportRow, onOpen: (() -> Unit)?) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .let { if (onOpen == null) it else it.clickable(onClick = onOpen) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = TypeScale.section, color = FieldTheme.textPrimary)
                if (row.subtitle.isNotEmpty()) {
                    Text(row.subtitle, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            row.stampLabel?.let { label ->
                StampChip(StampDescriptor(label, row.stampTone))
            }
        }
        row.trailing?.let { trailing ->
            Spacer(Modifier.height(8.dp))
            Text(trailing, style = TypeScale.numeric, color = FieldTheme.textPrimary)
        }
        row.detail?.let { detail ->
            Spacer(Modifier.height(8.dp))
            Text(detail, style = TypeScale.caption, color = FieldTheme.textSecondary)
        }
    }
}
