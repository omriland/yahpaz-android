package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun RootScreen(app: AppModel, ui: AppUiState) {
    Box(Modifier.fillMaxSize()) {
        when {
            ui.booting -> Booting()
            ui.trackToken != null && !ui.isSignedIn -> LiveTrackScreen(ui.trackToken, app::closeTrack)
            !ui.isSignedIn -> LoginGate(app)
            ui.mustChangePassword -> ProfileScreen(app, ui)
            ui.fillEventId != null -> FillScreen(ui.fillEventId, app)
            else -> MainTabs(app, ui)
        }
        if (ui.isSignedIn && ui.trackToken != null && ui.fillEventId == null && !ui.mustChangePassword) {
            Box(Modifier.fillMaxSize().background(FieldTheme.page)) {
                LiveTrackScreen(ui.trackToken, app::closeTrack)
            }
        }
        ui.toast?.let { toast ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ToastBanner(toast, ui.toastTone)
            }
        }
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
private fun LoginGate(app: AppModel) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(LoginMode.SIGNIN) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LoginScreen(
        busy = busy,
        error = error,
        email = email,
        password = password,
        mode = mode,
        onEmail = { email = it },
        onPassword = { password = it },
        onSubmit = {
            scope.launch {
                busy = true
                error = null
                val trimmed = email.trim()
                if (mode == LoginMode.SIGNIN) {
                    error = app.signIn(trimmed, password)
                } else {
                    val message = YahpazAPI.requestPasswordReset(trimmed)
                    if (message != null) error = message else mode = LoginMode.RESET_SENT
                }
                busy = false
            }
        },
        onToggleMode = {
            error = null
            mode = when (mode) {
                LoginMode.SIGNIN -> LoginMode.RESET
                else -> LoginMode.SIGNIN
            }
        },
    )
}

@Composable
private fun MainTabs(app: AppModel, ui: AppUiState) {
    Scaffold(
        containerColor = FieldTheme.page,
        bottomBar = {
            NavigationBar(containerColor = FieldTheme.raised) {
                val items = listOf(
                    Triple(AppTab.INBOX, "האירועים שלי", Icons.AutoMirrored.Outlined.ListAlt),
                    Triple(AppTab.SHIFTS, "המשמרות שלי", Icons.Outlined.CalendarMonth),
                    Triple(AppTab.AVAILABILITY, "זמינות", Icons.Outlined.Circle),
                    Triple(AppTab.PROFILE, "פרופיל", Icons.Outlined.Person),
                )
                items.forEach { (tab, label, icon) ->
                    NavigationBarItem(
                        selected = ui.tab == tab,
                        onClick = { app.setTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = TypeScale.caption) },
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
            when (ui.tab) {
                AppTab.INBOX -> InboxScreen(app, ui)
                AppTab.SHIFTS -> MyShiftsScreen(app, ui)
                AppTab.AVAILABILITY -> AvailabilityScreen(app, ui)
                AppTab.PROFILE -> ProfileScreen(app, ui)
            }
        }
    }
}
