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
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.filterContacts
import com.yahpz.domain.formatPhone
import com.yahpz.domain.israelToday
import com.yahpz.domain.roleLabels
import com.yahpz.domain.volunteerStatusLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<AdminUserListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val today = remember { israelToday() }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.adminUsers.isEmpty()) app.reloadAdminUsers()
    }

    val filtered = filterContacts(ui.adminUsers, query) { it.searchFields }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadAdminUsers()
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
            ToolsBackRow("משתמשים", onBack)
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "חיפוש לפי שם, או״ק, טלפון או דוא״ל",
            )
            when {
                ui.adminUsersFailed -> EmptyState(
                    title = "טעינת המשתמשים נכשלה. בדקו את החיבור ונסו שוב.",
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadAdminUsers() } },
                )
                ui.adminUsersLoading && ui.adminUsers.isEmpty() -> LoadingBlock("טוען משתמשים…")
                filtered.isEmpty() -> EmptyState(
                    title = if (query.isBlank()) "אין משתמשים להצגה" else "לא נמצאו משתמשים תואמים",
                    actionTitle = if (query.isBlank()) null else "ניקוי חיפוש",
                    onAction = if (query.isBlank()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${filtered.size} משתמשים",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                    filtered.forEach { user ->
                        AdminUserRow(user, today) { detail = user }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    detail?.let { user ->
        ModalBottomSheet(onDismissRequest = { detail = null }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(user.fullName.ifEmpty { "משתמש" }, style = TypeScale.section, color = FieldTheme.textPrimary)
                LedgerRow("או״ק", user.callsign)
                LedgerRow("טלפון", user.phone?.let { formatPhone(it) }.orEmpty())
                LedgerRow("דוא״ל", user.email)
                LedgerRow("תפקידים", roleLabels(user.roles).joinToString(" · "))
                LedgerRow("סטטוס מתנדב", volunteerStatusLabel(user.volunteerStatus))
                LedgerRow("זמינות", availabilityText(user, today))
                LedgerRow("רכבים", "${user.vehicleCount}")
                LedgerRow("חשבון", if (user.active) "פעיל" else "לא פעיל")
                TextButton(onClick = { detail = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun AdminUserRow(user: AdminUserListItem, today: String, onOpen: () -> Unit) {
    val effective = effectiveAvailability(user.availability, user.availableFrom, today)
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
                    user.fullName.ifEmpty { "משתמש" },
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(
                    listOf(user.callsign, roleLabels(user.roles).firstOrNull().orEmpty())
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            StampChip(
                StampDescriptor(
                    availabilityLabel(effective),
                    if (effective == AvailabilityStatus.AVAILABLE) StampTone.DONE else StampTone.PENDING,
                ),
            )
        }
        if (!user.active) {
            Spacer(Modifier.height(8.dp))
            Text("חשבון לא פעיל", style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

private fun availabilityText(user: AdminUserListItem, today: String): String {
    val effective = effectiveAvailability(user.availability, user.availableFrom, today)
    val caption = if (effective == AvailabilityStatus.AVAILABLE) {
        null
    } else {
        availabilityReturnCaption(user.availableFrom)
    }
    return listOfNotNull(availabilityLabel(effective), caption).joinToString(" · ")
}
