package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.yahpz.domain.AppRole
import com.yahpz.domain.IMPERSONATION_EMPTY
import com.yahpz.domain.IMPERSONATION_HINT
import com.yahpz.domain.IMPERSONATION_LOAD_FAILED
import com.yahpz.domain.PREVIEWABLE_ROLES
import com.yahpz.domain.ROLE_PREVIEW_HINT
import com.yahpz.domain.STOP_IMPERSONATION_LABEL
import com.yahpz.domain.STOP_ROLE_PREVIEW_LABEL
import com.yahpz.domain.USERS_SEARCH_PLACEHOLDER
import com.yahpz.domain.VIEW_AS_ROLE_LABEL
import com.yahpz.domain.VIEW_AS_USER_LABEL
import com.yahpz.domain.fieldsMatchQuery
import com.yahpz.domain.impersonationBannerText
import com.yahpz.domain.parseRolePreviewRole
import com.yahpz.domain.rolePreviewBannerText
import com.yahpz.domain.rolePreviewLabel
import kotlinx.coroutines.launch

private val rowShape = RoundedCornerShape(4.dp)

@Composable
fun ViewAsBanner(app: AppModel, ui: AppUiState) {
    val preview = parseRolePreviewRole(ui.previewRole)
    when {
        ui.impersonating && ui.impersonationName != null -> {
            ViewAsBannerCard(
                text = impersonationBannerText(
                    ui.impersonationName,
                    ui.impersonationCallsign.orEmpty(),
                ),
                action = STOP_IMPERSONATION_LABEL,
                onAction = { app.stopImpersonation() },
            )
        }
        preview != null -> {
            ViewAsBannerCard(
                text = rolePreviewBannerText(preview),
                action = STOP_ROLE_PREVIEW_LABEL,
                onAction = { app.stopRolePreview() },
            )
        }
    }
}

@Composable
private fun ViewAsBannerCard(text: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.accentSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = TypeScale.caption, color = FieldTheme.textPrimary)
        GhostButton(title = action, onClick = onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePreviewSheet(onClose: () -> Unit, onPick: (AppRole) -> Unit) {
    var selected by remember { mutableStateOf(AppRole.RESPONDER) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(VIEW_AS_ROLE_LABEL, style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(ROLE_PREVIEW_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
            PREVIEWABLE_ROLES.forEach { role ->
                ChoiceRow(
                    title = rolePreviewLabel(role),
                    selected = selected == role,
                    onClick = { selected = role },
                )
            }
            PrimaryButton(
                title = "המשך כ־${rolePreviewLabel(selected)}",
                onClick = { onPick(selected) },
            )
            GhostButton(title = "ביטול", onClick = onClose)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpersonationSheet(
    actorUserId: String,
    onClose: () -> Unit,
    onConfirm: suspend (String) -> String?,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<AdminUserListItem>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actorUserId) {
        loadError = null
        candidates = null
        runCatching { YahpazAPI.fetchImpersonationCandidates(actorUserId) }
            .onSuccess { candidates = it }
            .onFailure { loadError = IMPERSONATION_LOAD_FAILED }
    }

    val filtered = remember(candidates, query) {
        val rows = candidates.orEmpty()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) rows
        else rows.filter { fieldsMatchQuery(listOf(it.fullName, it.callsign, it.email), trimmed) }
    }
    val selected = filtered.firstOrNull { it.id == selectedId }

    ModalBottomSheet(onDismissRequest = { if (!busy) onClose() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(VIEW_AS_USER_LABEL, style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(IMPERSONATION_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
            FormField(
                label = "חיפוש",
                value = query,
                onValueChange = { query = it },
                placeholder = USERS_SEARCH_PLACEHOLDER,
            )
            loadError?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
            actionError?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
            if (candidates == null && loadError == null) {
                Text("טוען…", style = TypeScale.caption, color = FieldTheme.textMuted)
            }
            if (candidates != null && filtered.isEmpty()) {
                Text(IMPERSONATION_EMPTY, style = TypeScale.caption, color = FieldTheme.textMuted)
            }
            filtered.forEach { row ->
                ChoiceRow(
                    title = row.fullName,
                    caption = "או״ק ${row.callsign} · ${row.email}",
                    selected = row.id == selectedId,
                    onClick = { selectedId = row.id },
                )
            }
            PrimaryButton(
                title = if (selected != null) "המשך כ־${selected.fullName}" else "המשך",
                busy = busy,
                enabled = selected != null && !busy,
                onClick = {
                    val id = selected?.id ?: return@PrimaryButton
                    scope.launch {
                        busy = true
                        actionError = null
                        val error = onConfirm(id)
                        busy = false
                        if (error != null) actionError = error else onClose()
                    }
                },
            )
            GhostButton(title = "ביטול", enabled = !busy, onClick = onClose)
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    caption: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(if (selected) FieldTheme.accentSubtle else FieldTheme.raised, rowShape)
            .border(1.dp, if (selected) FieldTheme.accent else FieldTheme.strong, rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = if (selected) TypeScale.bodyStrong else TypeScale.body,
            color = if (selected) FieldTheme.accent else FieldTheme.textPrimary,
        )
        if (caption != null) {
            Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
        }
    }
}

@Composable
fun MoreViewAsRows(
    canViewAsUser: Boolean,
    canViewAsRole: Boolean,
    impersonating: Boolean,
    previewing: Boolean,
    onViewAsUser: () -> Unit,
    onViewAsRole: () -> Unit,
    onStopImpersonation: () -> Unit,
    onStopPreview: () -> Unit,
) {
    if (canViewAsUser) {
        MoreActionRow(VIEW_AS_USER_LABEL, onViewAsUser)
    }
    if (canViewAsRole) {
        MoreActionRow(VIEW_AS_ROLE_LABEL, onViewAsRole)
    }
    if (previewing) {
        MoreActionRow(STOP_ROLE_PREVIEW_LABEL, onStopPreview)
    }
    if (impersonating) {
        MoreActionRow(STOP_IMPERSONATION_LABEL, onStopImpersonation)
    }
}

@Composable
private fun MoreActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TypeScale.body, color = FieldTheme.textPrimary)
    }
}
