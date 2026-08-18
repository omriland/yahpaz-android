package com.yahpz.responder

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@Composable
private fun MainTabs(app: AppModel, ui: AppUiState) {
    Scaffold(
        containerColor = FieldTheme.page,
        bottomBar = {
            NavigationBar(containerColor = FieldTheme.raised) {
                val items = listOf(
                    Triple(AppTab.INBOX, "האירועים שלי", Icons.AutoMirrored.Outlined.ListAlt),
                    Triple(AppTab.SHIFTS, "המשמרות שלי", Icons.Outlined.CalendarMonth),
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
                AppTab.PROFILE -> ProfileScreen(app, ui)
            }
        }
    }
}
