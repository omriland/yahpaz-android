package com.yahpz.responder

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
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
import com.yahpz.domain.OPEN_DOC_EMPTY_TITLE
import com.yahpz.domain.OPEN_DOC_FAILED_TITLE
import com.yahpz.domain.OPEN_DOC_RANGE_ERROR
import com.yahpz.domain.OPEN_DOC_TITLE
import com.yahpz.domain.OpenDocFillStatus
import com.yahpz.domain.OpenDocRow
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.formatDate
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.openDocSummary
import com.yahpz.domain.returnDateToInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenDocReportScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val defaults = remember { app.defaultOpenDocRange() }
    var from by remember { mutableStateOf(returnDateToInput(ui.openDocFrom ?: defaults.first)) }
    var to by remember { mutableStateOf(returnDateToInput(ui.openDocTo ?: defaults.second)) }
    var rangeError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    fun isoRange(): Pair<String, String>? {
        val fromIso = normalizeReturnDate(from) ?: return null
        val toIso = normalizeReturnDate(to) ?: return null
        if (fromIso > toIso) return null
        return fromIso to toIso
    }

    suspend fun load() {
        val range = isoRange()
        if (range == null) {
            rangeError = OPEN_DOC_RANGE_ERROR
            return
        }
        rangeError = null
        app.reloadOpenDocumentation(range.first, range.second)
    }

    LaunchedEffect(Unit) {
        if (ui.openDocRows.isEmpty()) load()
    }

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
            ToolsBackRow(OPEN_DOC_TITLE, onBack)
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
            PrimaryButton(
                title = "טעינת הדוח",
                busy = ui.openDocLoading,
                onClick = { scope.launch { load() } },
            )
            when {
                ui.openDocFailed -> EmptyState(
                    title = OPEN_DOC_FAILED_TITLE,
                    actionTitle = "רענון",
                    onAction = { scope.launch { load() } },
                )
                ui.openDocLoading && ui.openDocRows.isEmpty() -> LoadingBlock("טוען את הדוח…")
                ui.openDocRows.isEmpty() -> EmptyState(title = OPEN_DOC_EMPTY_TITLE)
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        openDocSummary(ui.openDocRows.size),
                        style = TypeScale.body,
                        color = FieldTheme.textSecondary,
                    )
                    ui.openDocRows.forEach { row -> OpenDocRowCard(row) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OpenDocRowCard(row: OpenDocRow) {
    FieldCard(modifier = Modifier.heightIn(min = 44.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.responderDisplay, style = TypeScale.section, color = FieldTheme.textPrimary)
                Text(
                    listOfNotNull(
                        formatDate(row.eventDate),
                        row.policeEventId?.takeIf { it.isNotEmpty() },
                        row.roadName?.takeIf { it.isNotEmpty() },
                        row.location?.takeIf { it.isNotEmpty() },
                    ).joinToString(" · "),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            StampChip(
                StampDescriptor(
                    row.fillStatusLabel,
                    if (row.fillStatus == OpenDocFillStatus.IN_PROGRESS) StampTone.DRAFT else StampTone.PENDING,
                ),
            )
        }
        if (row.leadDisplay.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("אחמ״ש: ${row.leadDisplay}", style = TypeScale.caption, color = FieldTheme.textSecondary)
        }
    }
}
