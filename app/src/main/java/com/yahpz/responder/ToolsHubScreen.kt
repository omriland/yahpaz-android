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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yahpz.domain.visibleReportSpecs

/** Mobile admin hub — same four segments as web `ADMIN_SEGMENTS`. */
@Composable
fun ToolsHubScreen(app: AppModel, @Suppress("UNUSED_PARAMETER") ui: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ניהול", style = TypeScale.title, color = FieldTheme.textPrimary)
        ToolCard(title = "משתמשים") {
            app.setToolsDestination(ToolsDestination.ADMIN_USERS)
        }
        ToolCard(title = "דוחות וסטטיסטיקות") {
            app.setToolsDestination(ToolsDestination.REPORT_CATALOG)
        }
        ToolCard(title = "ניהול דלק") {
            app.setToolsDestination(ToolsDestination.FUEL_QUARTER)
        }
        ToolCard(title = "הגדרות") {
            app.setToolsDestination(ToolsDestination.CLOSED_LISTS)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ReportsCatalogScreen(app: AppModel, ui: AppUiState, title: String, onBack: (() -> Unit)?) {
    val reports = visibleReportSpecs(ui.roles)
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
            ToolsBackRow(title, onBack)
        } else {
            Text(title, style = TypeScale.title, color = FieldTheme.textPrimary)
        }
        reports.forEach { spec ->
            ToolCard(
                title = spec.title,
                caption = spec.includes,
                onClick = { app.openReport(spec.id) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToolCard(title: String, caption: String? = null, onClick: () -> Unit) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = TypeScale.section, color = FieldTheme.textPrimary)
                if (!caption.isNullOrBlank()) {
                    Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = FieldTheme.accent,
            )
        }
    }
}

@Composable
fun ToolsBackRow(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onBack),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TypeScale.title, color = FieldTheme.textPrimary)
        Text("חזרה", style = TypeScale.bodyStrong, color = FieldTheme.accent)
    }
}
