package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.yahpz.domain.FUEL_QUARTER_CAPTION
import com.yahpz.domain.FUEL_QUARTER_EMPTY
import com.yahpz.domain.FUEL_QUARTER_LOAD_FAILED
import com.yahpz.domain.FUEL_QUARTER_SEARCH_EMPTY
import com.yahpz.domain.FUEL_QUARTER_SEARCH_PLACEHOLDER
import com.yahpz.domain.FUEL_QUARTER_TITLE
import com.yahpz.domain.FuelQuarterRow
import com.yahpz.domain.defaultFuelQuarter
import com.yahpz.domain.filterFuelQuarterRows
import com.yahpz.domain.formatNumber
import com.yahpz.domain.fuelQuarterLabel
import com.yahpz.domain.sumRemainingKm
import com.yahpz.domain.unitFuelQuarterKpisFromRows
import kotlinx.coroutines.launch

@Composable
fun FuelQuarterScreen(app: AppModel, ui: AppUiState, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val initial = remember { defaultFuelQuarter() }
    var year by remember { mutableIntStateOf(ui.fuelQuarterYear ?: initial.year) }
    var quarter by remember { mutableIntStateOf(ui.fuelQuarterQuarter ?: initial.quarter) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(year, quarter, ui.userId) {
        if (ui.userId != null && ui.canAdmin) {
            app.reloadFuelQuarter(year, quarter)
        }
    }

    val workbook = ui.fuelQuarter?.takeIf { it.year == year && it.quarter == quarter }
    val rows = workbook?.rows.orEmpty()
    val filtered = filterFuelQuarterRows(rows, query)
    val kpis = unitFuelQuarterKpisFromRows(rows)
    val remainingTotal = sumRemainingKm(rows)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            ToolsBackRow(FUEL_QUARTER_TITLE, onBack)
        } else {
            Text(FUEL_QUARTER_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
        }
        Text(FUEL_QUARTER_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)

        FormField(
            label = "שנה",
            value = year.toString(),
            onValueChange = { raw ->
                raw.filter { it.isDigit() }.take(4).toIntOrNull()?.let { year = it }
            },
            mono = true,
            ltr = true,
        )

        Text("רבעון", style = TypeScale.label, color = FieldTheme.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..4).forEach { q ->
                val selected = quarter == q
                FieldCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clickable { quarter = q },
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = q.toString(),
                            style = TypeScale.bodyStrong,
                            color = if (selected) FieldTheme.accent else FieldTheme.textPrimary,
                        )
                    }
                }
            }
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = FUEL_QUARTER_SEARCH_PLACEHOLDER,
        )

        when {
            ui.fuelQuarterFailed && workbook == null -> EmptyState(
                title = FUEL_QUARTER_LOAD_FAILED,
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadFuelQuarter(year, quarter) } },
            )
            ui.fuelQuarterLoading && workbook == null -> LoadingBlock("טוען כרטיסי דלק…")
            workbook != null && rows.isEmpty() -> EmptyState(title = FUEL_QUARTER_EMPTY)
            workbook != null && filtered.isEmpty() -> EmptyState(
                title = FUEL_QUARTER_SEARCH_EMPTY,
                actionTitle = "ניקוי חיפוש",
                onAction = { query = "" },
            )
            workbook != null -> {
                Text(
                    "${fuelQuarterLabel(quarter)} · $year" +
                        if (workbook.status == "locked") " · נעול" else "",
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
                FieldCard {
                    Text("סיכום היחידה", style = TypeScale.section, color = FieldTheme.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    LedgerRow("סה״כ ק״מ", formatNumber(kpis.totalKm))
                    LedgerRow("כרטיסים מוקצים", formatNumber(kpis.suggestedCards))
                    LedgerRow("כרטיסים שחולקו", formatNumber(kpis.issuedCards))
                    LedgerRow("יתרה כוללת (ק״מ)", formatNumber(remainingTotal))
                }
                filtered.forEach { row -> FuelResponderCard(row, workbook.monthLabels) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FuelResponderCard(row: FuelQuarterRow, monthLabels: List<String>) {
    FieldCard(modifier = Modifier.heightIn(min = 44.dp)) {
        Text(row.display, style = TypeScale.section, color = FieldTheme.textPrimary)
        if (!row.active) {
            Text("לא פעיל", style = TypeScale.caption, color = FieldTheme.alert)
        }
        Spacer(Modifier.height(6.dp))
        LedgerRow("יתרה מרבעון קודם", formatNumber(row.openingBalanceKm))
        if (monthLabels.size >= 3) {
            LedgerRow(monthLabels[0], formatNumber(row.kmMonth1))
            LedgerRow(monthLabels[1], formatNumber(row.kmMonth2))
            LedgerRow(monthLabels[2], formatNumber(row.kmMonth3))
        }
        LedgerRow("סה״כ ק״מ", formatNumber(row.quarterKm))
        LedgerRow("כרטיסים", formatNumber(row.cards))
        LedgerRow("יתרה (ק״מ)", formatNumber(row.remainingKm))
    }
}
