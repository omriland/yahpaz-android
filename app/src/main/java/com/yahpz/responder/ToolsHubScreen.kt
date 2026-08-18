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
import com.yahpz.domain.BROADCAST_CAPTION
import com.yahpz.domain.BROADCAST_TITLE
import com.yahpz.domain.canSendUnitBroadcast
import com.yahpz.domain.highestRoleLabel
import com.yahpz.domain.toolsTabLabel
import com.yahpz.domain.visibleReportSpecs

@Composable
fun ToolsHubScreen(app: AppModel, ui: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(toolsTabLabel(ui.roles), style = TypeScale.title, color = FieldTheme.textPrimary)
        Text(
            listOfNotNull(ui.profile?.fullName, highestRoleLabel(ui.roles)).joinToString(" · "),
            style = TypeScale.body,
            color = FieldTheme.textSecondary,
        )
        if (ui.canManageUnit) {
            ToolsSectionLabel("היחידה")
            ToolCard(
                title = NEW_EVENT_TITLE,
                caption = "הזנת אירוע ושיבוץ כוננים לתיעוד",
                onClick = { app.setToolsDestination(ToolsDestination.NEW_EVENT) },
            )
            ToolCard(
                title = NEW_SHIFT_TITLE,
                caption = "פתיחת משמרת ושיבוץ צוות",
                onClick = { app.setToolsDestination(ToolsDestination.NEW_SHIFT) },
            )
            ToolCard(
                title = "אירועי היחידה",
                caption = "כל האירועים האחרונים ביחידה",
                onClick = { app.setTab(AppTab.UNIT_EVENTS) },
            )
            ToolCard(
                title = "משמרות היחידה",
                caption = "כל המשמרות האחרונות ביחידה",
                onClick = { app.setTab(AppTab.UNIT_SHIFTS) },
            )
        }
        val reports = visibleReportSpecs(ui.roles)
        if (reports.isNotEmpty()) {
            ToolsSectionLabel("דוחות")
            reports.forEach { spec ->
                ToolCard(
                    title = spec.title,
                    caption = spec.includes,
                    onClick = { app.openReport(spec.id) },
                )
            }
        }
        if (ui.canAdmin) {
            ToolsSectionLabel("ניהול")
            ToolCard(
                title = "משתמשים",
                caption = "רשימת המשתמשים, תפקידים וזמינות",
                onClick = { app.setToolsDestination(ToolsDestination.ADMIN_USERS) },
            )
            if (canSendUnitBroadcast(ui.roles)) {
                ToolCard(
                    title = BROADCAST_TITLE,
                    caption = BROADCAST_CAPTION,
                    onClick = { app.setToolsDestination(ToolsDestination.BROADCAST) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToolsSectionLabel(title: String) {
    Text(title, style = TypeScale.label, color = FieldTheme.textSecondary)
}

@Composable
private fun ToolCard(title: String, caption: String, onClick: () -> Unit) {
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
                Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
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
