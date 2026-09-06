package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.yahpz.domain.MOBILE_MORE_LABEL
import com.yahpz.domain.MobileNavEntry
import com.yahpz.domain.canStartImpersonation
import com.yahpz.domain.normalizeLoginEmail
import com.yahpz.domain.normalizeLoginSecret
import com.yahpz.domain.canStartRolePreview
import com.yahpz.domain.defaultMobileView
import com.yahpz.domain.mobileNavEntries
import com.yahpz.domain.parseRolePreviewRole
import com.yahpz.domain.splitMobileNav
import kotlinx.coroutines.launch

@Composable
fun RootScreen(app: AppModel, ui: AppUiState) {
    var feedbackOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val trackBlocking = ui.trackToken != null && ui.fillEventId == null && !ui.mustChangePassword
    val showFeedbackFab = ui.isSignedIn &&
        ui.forceUpdate == null &&
        !ui.privacyOpen &&
        !trackBlocking &&
        shouldShowFeedbackFab(ui.feedbackHiddenUntilRefresh, ui.toolsDestination, ui.fillEventId != null) &&
        !feedbackOpen
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        if (ui.isSignedIn && (ui.impersonating || ui.previewRole != null)) {
            Box(Modifier.statusBarsPadding()) {
                ViewAsBanner(app, ui)
            }
        }
        Box(Modifier.weight(1f).fillMaxSize()) {
        when {
            ui.booting -> SafeEdgeScreen { Booting() }
            ui.forceUpdate != null -> SafeEdgeScreen { ForceUpdateScreen(ui.forceUpdate) }
            ui.trackToken != null && !ui.isSignedIn -> SafeEdgeScreen {
                LiveTrackScreen(ui.trackToken, app::closeTrack)
            }
            !ui.isSignedIn -> SafeEdgeScreen { LoginGate(app, ui) }
            ui.mustChangePassword -> Box(Modifier.fillMaxSize()) {
                SafeEdgeScreen { ProfileScreen(app, ui) }
                if (showFeedbackFab) {
                    FeedbackMiniFab(
                        onClick = { feedbackOpen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    )
                }
            }
            ui.fillEventId != null -> SafeEdgeScreen { FillScreen(ui.fillEventId, app) }
            else -> MainTabs(app, ui, showFeedbackFab) { feedbackOpen = true }
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
    if (ui.privacyOpen && ui.forceUpdate == null) {
        PrivacyPolicyScreen(onClose = app::closePrivacy)
    }
    if (feedbackOpen && ui.isSignedIn && ui.forceUpdate == null) {
        FeedbackSheet(
            pagePath = feedbackPagePathForUi(ui),
            onDismiss = { feedbackOpen = false },
            onHideUntilRefresh = {
                app.hideFeedbackUntilRefresh()
                feedbackOpen = false
            },
            onSubmit = { kind, body, audio, mime, attachments ->
                app.submitUserFeedback(
                    kind,
                    body,
                    feedbackPagePathForUi(ui),
                    audio,
                    mime,
                    attachments,
                )
            },
        )
    }
    val optionalUpdate = ui.optionalUpdate
    if (optionalUpdate != null && ui.forceUpdate == null && !ui.privacyOpen) {
        OptionalUpdateSheet(
            update = optionalUpdate,
            onLater = {
                OptionalUpdatePrefs.skip(context, optionalUpdate.latestVersionCode)
                app.dismissOptionalUpdate()
            },
        )
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
            if (update.messageHe.isNotBlank()) {
                Text(
                    text = update.messageHe,
                    style = TypeScale.body,
                    color = CommandTheme.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }
            InAppUpdateActions(
                apkUrl = update.apkUrl,
                updateTitle = "הורדה והתקנה",
                command = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalUpdateSheet(
    update: OptionalUpdateAvailable,
    onLater: () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = { if (!busy) onLater() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("גרסה חדשה", style = TypeScale.title, color = FieldTheme.textPrimary)
            Text(
                text = update.messageHe.ifBlank { DEFAULT_OPTIONAL_UPDATE_MESSAGE },
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
            )
            InAppUpdateActions(
                apkUrl = update.apkUrl,
                updateTitle = "עדכון",
                laterTitle = "אחר כך",
                onLater = onLater,
                onBusyChange = { busy = it },
            )
        }
    }
}

/** Downloads and installs the APK in-process. Do not open the APK URL in a browser. */
@Composable
private fun InAppUpdateActions(
    apkUrl: String,
    updateTitle: String,
    laterTitle: String? = null,
    onLater: (() -> Unit)? = null,
    command: Boolean = false,
    onBusyChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorHe by remember { mutableStateOf<String?>(null) }

    fun setBusy(value: Boolean) {
        busy = value
        onBusyChange(value)
    }

    if (busy) {
        val percent = (progress * 100).toInt()
        Text(
            text = if (percent > 0) "מוריד עדכון… $percent%" else "מוריד עדכון…",
            style = TypeScale.caption,
            color = if (command) CommandTheme.textSecondary else FieldTheme.textSecondary,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    errorHe?.let {
        Text(it, style = TypeScale.caption, color = FieldTheme.alert)
    }
    PrimaryButton(
        title = updateTitle,
        busy = busy,
        enabled = !busy,
        command = command,
        onClick = {
            errorHe = null
            progress = 0f
            setBusy(true)
            scope.launch {
                val result = SideloadApkInstaller.downloadAndInstall(context, apkUrl) { progress = it }
                setBusy(false)
                result.onFailure { error ->
                    errorHe = if (error is NeedsUnknownSourcesException) {
                        "אשרו התקנה מאבן דרך בהגדרות, ואז לחצו עדכון שוב."
                    } else {
                        "ההורדה נכשלה. נסו שוב."
                    }
                }
            }
        },
    )
    if (laterTitle != null && onLater != null) {
        GhostButton(title = laterTitle, onClick = onLater, enabled = !busy)
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
            val trimmed = normalizeLoginEmail(email)
            if (mode == LoginMode.SIGNIN) {
                app.submitSignIn(trimmed, normalizeLoginSecret(password))
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
        onOpenPrivacy = { app.openPrivacy() },
    )
}

private fun iconForView(view: String): ImageVector = when (view) {
    "mine" -> Icons.AutoMirrored.Outlined.ListAlt
    "my_shifts" -> Icons.Outlined.CalendarMonth
    "contacts" -> Icons.Outlined.Contacts
    "map" -> Icons.Outlined.Map
    "events" -> Icons.AutoMirrored.Outlined.EventNote
    "shifts" -> Icons.Outlined.Schedule
    "reports" -> Icons.Outlined.Assessment
    "users" -> Icons.Outlined.AdminPanelSettings
    "profile" -> Icons.Outlined.Person
    else -> Icons.Outlined.MoreHoriz
}

private val navItemColors
    @Composable get() = NavigationBarItemDefaults.colors(
        selectedIconColor = FieldTheme.accent,
        selectedTextColor = FieldTheme.accent,
        indicatorColor = FieldTheme.accentSubtle,
        unselectedIconColor = FieldTheme.textMuted,
        unselectedTextColor = FieldTheme.textMuted,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabs(
    app: AppModel,
    ui: AppUiState,
    showFeedbackFab: Boolean,
    onOpenFeedback: () -> Unit,
) {
    val entries = remember(ui.roles) { mobileNavEntries(ui.roles) }
    val split = remember(entries) { splitMobileNav(entries) }
    val allowed = remember(entries) { entries.map { appTabForMobileView(it.view) }.toSet() }
    val contentTab = when {
        ui.tab in allowed -> ui.tab
        else -> {
            val fallback = appTabForMobileView(defaultMobileView(ui.roles))
            if (fallback in allowed) fallback else allowed.first()
        }
    }
    val canViewAsUser = canStartImpersonation(ui.actualRoles, ui.impersonating)
    val canViewAsRole = canStartRolePreview(
        actualRoles = ui.actualRoles,
        impersonating = ui.impersonating,
        previewing = parseRolePreviewRole(ui.previewRole) != null,
    )
    val showViewAs = canViewAsUser || canViewAsRole || ui.impersonating || ui.previewRole != null
    val showMore = split.more.isNotEmpty() || showViewAs
    val overlay = ui.toolsDestination
    val adminTab = contentTab == AppTab.TOOLS && isAdminTabDestination(overlay)
    val showingMain = overlay == ToolsDestination.HUB || adminTab
    val reduceMotion = rememberReducedMotion()
    var moreOpen by remember { mutableStateOf(false) }
    var rolePickerOpen by remember { mutableStateOf(false) }
    var userPickerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = overlay != ToolsDestination.HUB && !adminTab) {
        when (overlay) {
            ToolsDestination.REPORT -> app.setToolsDestination(
                if (contentTab == AppTab.REPORTS) ToolsDestination.HUB else ToolsDestination.REPORT_CATALOG,
            )
            ToolsDestination.BROADCAST -> app.setToolsDestination(ToolsDestination.CLOSED_LISTS)
            else -> app.setToolsDestination(ToolsDestination.HUB)
        }
    }

    Scaffold(
        containerColor = FieldTheme.page,
        floatingActionButton = {
            val showCreate = overlay == ToolsDestination.HUB &&
                (contentTab == AppTab.UNIT_EVENTS || contentTab == AppTab.UNIT_SHIFTS)
            if (showFeedbackFab || showCreate) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showFeedbackFab) {
                        FeedbackMiniFab(onClick = onOpenFeedback)
                    }
                    if (showCreate) {
                        when (contentTab) {
                            AppTab.UNIT_EVENTS -> PrimaryCreateFab(NEW_EVENT_TITLE) {
                                app.setToolsDestination(ToolsDestination.NEW_EVENT)
                            }
                            AppTab.UNIT_SHIFTS -> PrimaryCreateFab(NEW_SHIFT_TITLE) {
                                app.setToolsDestination(ToolsDestination.NEW_SHIFT)
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = FieldTheme.raised, tonalElevation = 0.dp) {
                split.tabs.forEach { entry ->
                    val tab = appTabForMobileView(entry.view)
                    NavigationBarItem(
                        selected = contentTab == tab,
                        onClick = {
                            moreOpen = false
                            app.setTab(tab)
                        },
                        icon = { Icon(iconForView(entry.view), contentDescription = entry.label) },
                        label = { Text(entry.label, style = TypeScale.caption) },
                        colors = navItemColors,
                        modifier = Modifier.heightIn(min = 44.dp),
                    )
                }
                if (showMore) {
                    val moreSelected = split.more.any { appTabForMobileView(it.view) == contentTab }
                    NavigationBarItem(
                        selected = moreSelected,
                        onClick = { moreOpen = true },
                        icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = MOBILE_MORE_LABEL) },
                        label = { Text(MOBILE_MORE_LABEL, style = TypeScale.caption) },
                        colors = navItemColors,
                        modifier = Modifier.heightIn(min = 44.dp),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (showingMain) {
                Crossfade(
                    targetState = contentTab,
                    animationSpec = tween(if (reduceMotion) 0 else 200),
                    label = "main-tab",
                ) { tab ->
                    TabBody(app, ui, tab)
                }
            } else {
                OverlayBody(app, ui, overlay, contentTab)
            }
        }
    }

    if (moreOpen && showMore) {
        ModalBottomSheet(onDismissRequest = { moreOpen = false }) {
            MoreSheet(
                rows = split.more,
                current = contentTab,
                onPick = { entry ->
                    moreOpen = false
                    app.setTab(appTabForMobileView(entry.view))
                },
                extra = {
                    MoreViewAsRows(
                        canViewAsUser = canViewAsUser,
                        canViewAsRole = canViewAsRole,
                        impersonating = ui.impersonating,
                        previewing = ui.previewRole != null,
                        onViewAsUser = {
                            moreOpen = false
                            userPickerOpen = true
                        },
                        onViewAsRole = {
                            moreOpen = false
                            rolePickerOpen = true
                        },
                        onStopImpersonation = {
                            moreOpen = false
                            app.stopImpersonation()
                        },
                        onStopPreview = {
                            moreOpen = false
                            app.stopRolePreview()
                        },
                    )
                },
            )
        }
    }
    if (rolePickerOpen) {
        RolePreviewSheet(
            onClose = { rolePickerOpen = false },
            onPick = { role ->
                rolePickerOpen = false
                app.startRolePreview(role)
            },
        )
    }
    if (userPickerOpen) {
        val actorId = ui.userId
        if (actorId != null) {
            ImpersonationSheet(
                actorUserId = actorId,
                onClose = { userPickerOpen = false },
                onConfirm = { targetId -> app.startImpersonation(targetId) },
            )
        }
    }
}

@Composable
private fun MoreSheet(
    rows: List<MobileNavEntry>,
    current: AppTab,
    onPick: (MobileNavEntry) -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(MOBILE_MORE_LABEL, style = TypeScale.title, color = FieldTheme.textPrimary)
        rows.forEach { entry ->
            val selected = appTabForMobileView(entry.view) == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable { onPick(entry) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = iconForView(entry.view),
                    contentDescription = null,
                    tint = if (selected) FieldTheme.accent else FieldTheme.textMuted,
                )
                Text(
                    entry.label,
                    style = TypeScale.body,
                    color = if (selected) FieldTheme.accent else FieldTheme.textPrimary,
                )
            }
        }
        if (rows.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }
        extra()
    }
}

@Composable
private fun TabBody(app: AppModel, ui: AppUiState, tab: AppTab) {
    when (tab) {
        AppTab.INBOX -> InboxScreen(app, ui)
        AppTab.SHIFTS -> MyShiftsScreen(app, ui)
        AppTab.CONTACTS -> ContactsScreen(app, ui)
        AppTab.MAP -> MapScreen(app, ui)
        AppTab.UNIT_EVENTS -> UnitEventsScreen(app, ui)
        AppTab.UNIT_SHIFTS -> UnitShiftsScreen(app, ui)
        AppTab.TOOLS -> AdminShell(app, ui)
        AppTab.REPORTS -> ReportsCatalogScreen(app, ui, title = "דוחות", onBack = null)
        AppTab.PROFILE -> ProfileScreen(app, ui)
    }
}

@Composable
private fun OverlayBody(app: AppModel, ui: AppUiState, overlay: ToolsDestination, contentTab: AppTab) {
    val backToHub = { app.setToolsDestination(ToolsDestination.HUB) }
    val backFromReport = {
        app.setToolsDestination(
            if (contentTab == AppTab.REPORTS) ToolsDestination.HUB else ToolsDestination.REPORT_CATALOG,
        )
    }
    when (overlay) {
        ToolsDestination.HUB -> {}
        ToolsDestination.REPORT_CATALOG -> ReportsCatalogScreen(
            app, ui,
            title = "דוחות וסטטיסטיקות",
            onBack = backToHub,
        )
        ToolsDestination.REPORT -> ReportScreen(app, ui, backFromReport)
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
        ToolsDestination.BROADCAST -> BroadcastScreen(
            app, ui,
            onBack = { app.setToolsDestination(ToolsDestination.CLOSED_LISTS) },
        )
        ToolsDestination.FUEL_QUARTER -> FuelQuarterScreen(app, ui, backToHub)
        ToolsDestination.CLOSED_LISTS -> ClosedListsScreen(app, backToHub)
        ToolsDestination.COCKPIT -> CockpitScreen(app, ui, backToHub)
    }
}
