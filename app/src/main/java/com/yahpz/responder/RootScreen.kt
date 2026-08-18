package com.yahpz.responder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yahpz.domain.toolsTabLabel
import kotlinx.coroutines.launch

@Composable
fun RootScreen(app: AppModel, ui: AppUiState) {
    Box(Modifier.fillMaxSize()) {
        when {
            ui.booting -> SafeEdgeScreen { Booting() }
            ui.forceUpdate != null -> SafeEdgeScreen { ForceUpdateScreen(ui.forceUpdate) }
            ui.trackToken != null && !ui.isSignedIn -> SafeEdgeScreen {
                LiveTrackScreen(ui.trackToken, app::closeTrack)
            }
            !ui.isSignedIn -> SafeEdgeScreen { LoginGate(app, ui) }
            ui.mustChangePassword -> SafeEdgeScreen { ProfileScreen(app, ui) }
            ui.fillEventId != null -> SafeEdgeScreen { FillScreen(ui.fillEventId, app) }
            else -> MainTabs(app, ui)
        }
        if (
            ui.forceUpdate == null &&
            ui.isSignedIn &&
            ui.trackToken != null &&
            ui.fillEventId == null &&
            !ui.mustChangePassword
        ) {
            Box(Modifier.fillMaxSize().background(FieldTheme.page)) {
                SafeEdgeScreen {
                    LiveTrackScreen(ui.trackToken, app::closeTrack)
                }
            }
        }
        ui.toast?.let { toast ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ToastBanner(toast, ui.toastTone)
            }
        }
    }
}

/** Full-screen routes drawn under edge-to-edge system bars. MainTabs uses Scaffold insets instead. */
@Composable
private fun SafeEdgeScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        content()
    }
}

@Composable
private fun Booting() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CommandTheme.page),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = CommandTheme.accent)
    }
}

@Composable
private fun ForceUpdateScreen(update: ForceUpdateRequired) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CommandTheme.page)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "אבן דרך",
                style = TypeScale.brand,
                color = CommandTheme.textPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = update.messageHe,
                style = TypeScale.body,
                color = CommandTheme.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                title = "הורדה והתקנה",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl))
                    context.startActivity(intent)
                },
                command = true,
            )
        }
    }
}

@Composable
private fun LoginGate(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(LoginMode.SIGNIN) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var resetBusy by remember { mutableStateOf(false) }

    LoginScreen(
        busy = if (mode == LoginMode.SIGNIN) ui.signingIn else resetBusy,
        error = if (mode == LoginMode.SIGNIN) ui.signInError else resetError,
        email = email,
        password = password,
        mode = mode,
        onEmail = { email = it },
        onPassword = { password = it },
        onSubmit = {
            val trimmed = email.trim()
            if (mode == LoginMode.SIGNIN) {
                app.submitSignIn(trimmed, password)
            } else {
                scope.launch {
                    resetBusy = true
                    resetError = null
                    val message = YahpazAPI.requestPasswordReset(trimmed)
                    if (message != null) resetError = message else mode = LoginMode.RESET_SENT
                    resetBusy = false
                }
            }
        },
        onToggleMode = {
            resetError = null
            app.clearSignInError()
            mode = when (mode) {
                LoginMode.SIGNIN -> LoginMode.RESET
                else -> LoginMode.SIGNIN
            }
        },
    )
}

private data class TabItem(val tab: AppTab, val label: String, val icon: ImageVector)

/** Bottom bar holds at most five items; unit lists live under the tools tab. */
private fun tabItems(ui: AppUiState): List<TabItem> = buildList {
    if (ui.canRespond) {
        add(TabItem(AppTab.INBOX, "האירועים שלי", Icons.AutoMirrored.Outlined.ListAlt))
        add(TabItem(AppTab.SHIFTS, "המשמרות שלי", Icons.Outlined.CalendarMonth))
    }
    add(TabItem(AppTab.CONTACTS, "אנשי קשר", Icons.Outlined.Contacts))
    if (ui.canManageUnit) {
        add(TabItem(AppTab.TOOLS, toolsTabLabel(ui.roles), Icons.Outlined.Build))
    }
    add(TabItem(AppTab.PROFILE, "פרופיל", Icons.Outlined.Person))
}

@Composable
private fun MainTabs(app: AppModel, ui: AppUiState) {
    val items = tabItems(ui)
    val unitList = ui.canManageUnit && (ui.tab == AppTab.UNIT_EVENTS || ui.tab == AppTab.UNIT_SHIFTS)
    // A role may not grant every tab (an admin without כונן has no inbox), so fall back to the first.
    val contentTab = when {
        unitList -> ui.tab
        items.any { it.tab == ui.tab } -> ui.tab
        else -> items.first().tab
    }
    val barTab = if (unitList) AppTab.TOOLS else contentTab

    BackHandler(enabled = unitList) { app.setTab(AppTab.TOOLS) }
    BackHandler(enabled = contentTab == AppTab.TOOLS && ui.toolsDestination != ToolsDestination.HUB) {
        app.setToolsDestination(ToolsDestination.HUB)
    }

    Scaffold(
        containerColor = FieldTheme.page,
        bottomBar = {
            NavigationBar(containerColor = FieldTheme.raised) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = barTab == item.tab,
                        onClick = { app.setTab(item.tab) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = TypeScale.caption) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FieldTheme.accent,
                            selectedTextColor = FieldTheme.accent,
                            indicatorColor = FieldTheme.accentSubtle,
                            unselectedIconColor = FieldTheme.textMuted,
                            unselectedTextColor = FieldTheme.textMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (contentTab) {
                AppTab.INBOX -> InboxScreen(app, ui)
                AppTab.SHIFTS -> MyShiftsScreen(app, ui)
                AppTab.CONTACTS -> ContactsScreen(app, ui)
                AppTab.UNIT_EVENTS -> UnitEventsScreen(app, ui)
                AppTab.UNIT_SHIFTS -> UnitShiftsScreen(app, ui)
                AppTab.TOOLS -> ToolsTab(app, ui)
                AppTab.PROFILE -> ProfileScreen(app, ui)
            }
        }
    }
}

@Composable
private fun ToolsTab(app: AppModel, ui: AppUiState) {
    val backToHub = { app.setToolsDestination(ToolsDestination.HUB) }
    when (ui.toolsDestination) {
        ToolsDestination.HUB -> ToolsHubScreen(app, ui)
        ToolsDestination.REPORT -> ReportScreen(app, ui, backToHub)
        ToolsDestination.ADMIN_USERS -> AdminUsersScreen(app, ui, backToHub)
        ToolsDestination.NEW_EVENT -> EventFormScreen(app, ui, backToHub)
        ToolsDestination.EDIT_EVENT -> EventFormScreen(
            app, ui,
            onBack = { app.setTab(AppTab.UNIT_EVENTS) },
            eventId = ui.editingEventId,
        )
        ToolsDestination.NEW_SHIFT -> ShiftFormScreen(app, ui, backToHub)
        ToolsDestination.EDIT_SHIFT -> ShiftFormScreen(
            app, ui,
            onBack = { app.setTab(AppTab.UNIT_SHIFTS) },
            shiftId = ui.editingShiftId,
        )
        ToolsDestination.BROADCAST -> BroadcastScreen(app, ui, backToHub)
        ToolsDestination.FUEL_QUARTER -> FuelQuarterScreen(app, ui, backToHub)
        ToolsDestination.CLOSED_LISTS -> ClosedListsScreen(app, backToHub)
    }
}
