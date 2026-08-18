package com.yahpz.responder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.ADD_VEHICLE
import com.yahpz.domain.AdminVehicleDraft
import com.yahpz.domain.AppRole
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.DEACTIVATE_USER_ACTION
import com.yahpz.domain.DEACTIVATE_USER_BODY
import com.yahpz.domain.DELETE_USER_ACTION
import com.yahpz.domain.DELETE_USER_BODY
import com.yahpz.domain.DELETE_USER_TITLE
import com.yahpz.domain.EMAIL_INVITE_HINT
import com.yahpz.domain.EMAIL_LOCKED_HINT
import com.yahpz.domain.FIELD_CALLSIGN
import com.yahpz.domain.FIELD_EMAIL
import com.yahpz.domain.FIELD_FULL_NAME
import com.yahpz.domain.FIELD_PHONE
import com.yahpz.domain.FIELD_ROLES
import com.yahpz.domain.FIELD_VEHICLES
import com.yahpz.domain.FIELD_VOLUNTEER_STATUS
import com.yahpz.domain.INVITABLE_ROLES
import com.yahpz.domain.INVITE_LINK_COPIED
import com.yahpz.domain.INVITE_LINK_COPY_FAILED
import com.yahpz.domain.INVITE_PENDING_LABEL
import com.yahpz.domain.INVITE_RESENT_COPIED
import com.yahpz.domain.INVITE_TITLE
import com.yahpz.domain.INACTIVE_ACCOUNT_LABEL
import com.yahpz.domain.InviteDraft
import com.yahpz.domain.OTP_ENABLE_ACTION
import com.yahpz.domain.OTP_ENABLE_LOGIN_TITLE
import com.yahpz.domain.OTP_ENABLE_USERS_PAGE_TITLE
import com.yahpz.domain.OTP_PHONE_REQUIRED
import com.yahpz.domain.OVERFLOW_COPY_INVITE_LINK
import com.yahpz.domain.OVERFLOW_DELETE
import com.yahpz.domain.OVERFLOW_EDIT
import com.yahpz.domain.OVERFLOW_RESEND_INVITE
import com.yahpz.domain.PHONE_HINT
import com.yahpz.domain.ROLES_HINT
import com.yahpz.domain.SELF_DELETE_ERROR
import com.yahpz.domain.SUPER_ADMIN_LOCK_ERROR
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.USER_CREATED
import com.yahpz.domain.USER_CREATED_COPIED
import com.yahpz.domain.USERS_SEARCH_PLACEHOLDER
import com.yahpz.domain.USERS_TITLE
import com.yahpz.domain.USER_EDIT_TITLE
import com.yahpz.domain.USER_SAVE_LABEL
import com.yahpz.domain.VEHICLE_ARCHIVE_CONFIRM
import com.yahpz.domain.VEHICLE_ARCHIVED_CAPTION
import com.yahpz.domain.VEHICLE_DELETE_CONFIRM
import com.yahpz.domain.VEHICLE_MODEL_LABEL
import com.yahpz.domain.VEHICLE_PLATE_LABEL
import com.yahpz.domain.VolunteerStatus
import com.yahpz.domain.addressKindLabel
import com.yahpz.domain.adminUserMatchesQuery
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.canMutateAdminUser
import com.yahpz.domain.canToggleUsersPageOtp
import com.yahpz.domain.createUserEmailError
import com.yahpz.domain.deactivateConfirmTitle
import com.yahpz.domain.deleteUserConfirm
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.formatPhone
import com.yahpz.domain.formatPlate
import com.yahpz.domain.hasSuperAdminRole
import com.yahpz.domain.isAssignableRoleLocked
import com.yahpz.domain.isInvitePending
import com.yahpz.domain.isValidIlMobile
import com.yahpz.domain.israelToday
import com.yahpz.domain.otpLoginActionLabel
import com.yahpz.domain.otpUserLabel
import com.yahpz.domain.otpUsersPageActionLabel
import com.yahpz.domain.roleLabels
import com.yahpz.domain.setActiveActionLabel
import com.yahpz.domain.toggleAssignableRole
import com.yahpz.domain.validateAdminUserDraft
import com.yahpz.domain.volunteerStatusLabel
import com.yahpz.domain.withImpliedAssignableRoles
import kotlinx.coroutines.launch

private sealed class AdminConfirm {
    data class Deactivate(val user: AdminUserListItem) : AdminConfirm()
    data class Delete(val user: AdminUserListItem) : AdminConfirm()
    data class Otp(val user: AdminUserListItem, val kind: String) : AdminConfirm()
    data class Vehicle(val mode: String, val vehicle: AdminVehicleDraft) : AdminConfirm()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(app: AppModel, ui: AppUiState, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = remember { israelToday() }
    val actorIsSuperAdmin = hasSuperAdminRole(ui.roles)
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<AdminUserListItem?>(null) }
    var form by remember { mutableStateOf<InviteDraft?>(null) }
    var confirm by remember { mutableStateOf<AdminConfirm?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.adminUsers.isEmpty()) app.reloadAdminUsers()
    }

    val filtered = if (query.isBlank()) {
        ui.adminUsers
    } else {
        ui.adminUsers.filter { adminUserMatchesQuery(it.searchInput, query, today) }
    }

    Box(Modifier.fillMaxSize()) {
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
                if (onBack != null) {
                    ToolsBackRow(USERS_TITLE, onBack)
                } else {
                    Text(USERS_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
                }
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = USERS_SEARCH_PLACEHOLDER,
                )
                GhostButton(title = INVITE_TITLE, onClick = {
                    formError = null
                    form = InviteDraft()
                })
                when {
                    ui.adminUsersFailed -> EmptyState(
                        title = "טעינת המשתמשים נכשלה. בדקו את החיבור ונסו שוב.",
                        actionTitle = "רענון",
                        onAction = { scope.launch { app.reloadAdminUsers() } },
                    )
                    ui.adminUsersLoading && ui.adminUsers.isEmpty() -> LoadingBlock("טוען משתמשים…")
                    filtered.isEmpty() -> EmptyState(
                        title = if (query.isBlank()) "אין משתמשים להצגה" else "לא נמצאו משתמשים תואמים",
                        caption = if (query.isBlank()) "משתמש חדש יופיע כאן ברגע שיוזמן." else null,
                        actionTitle = if (query.isBlank()) INVITE_TITLE else "ניקוי חיפוש",
                        onAction = if (query.isBlank()) {
                            {
                                formError = null
                                form = InviteDraft()
                            }
                        } else {
                            { query = "" }
                        },
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

        form?.let { draft ->
            AdminUserForm(
                draft = draft,
                actorUserId = ui.userId,
                addresses = ui.adminUsers.firstOrNull { it.id == draft.id }?.addresses.orEmpty(),
                saving = saving,
                formError = formError,
                onChange = { form = it },
                onClose = { if (!saving) form = null },
                onSave = {
                    val next = validateAdminUserDraft(
                        draft,
                        actorUserId = ui.userId,
                        isSuperAdmin = actorIsSuperAdmin,
                        existingRoles = ui.adminUsers.firstOrNull { it.id == draft.id }?.roles ?: draft.roles,
                    )
                    if (!next.isEmpty) {
                        formError = next.formMessage
                        return@AdminUserForm
                    }
                    formError = null
                    scope.launch {
                        saving = true
                        if (draft.id == null) {
                            val result = app.inviteUser(draft)
                            saving = false
                            if (result.error != null) {
                                formError = result.error
                            } else {
                                val copied = copyInviteLink(context, result.actionLink)
                                app.showToast(
                                    if (copied) USER_CREATED_COPIED else (result.message ?: USER_CREATED),
                                    StampTone.DONE,
                                )
                                form = null
                            }
                        } else {
                            formError = app.saveAdminUser(draft)
                            saving = false
                            if (formError == null) form = null
                        }
                    }
                },
                onRemoveVehicle = { vehicle ->
                    if (vehicle.archived) return@AdminUserForm
                    val vehicleId = vehicle.id
                    val userId = draft.id
                    if (vehicleId.isNullOrEmpty() || userId == null) {
                        confirm = AdminConfirm.Vehicle("delete", vehicle)
                        return@AdminUserForm
                    }
                    scope.launch {
                        val attached = runCatching {
                            app.isVehicleAttachedToEvents(userId, vehicleId, vehicle.plateNumber)
                        }.getOrDefault(false)
                        confirm = AdminConfirm.Vehicle(if (attached) "archive" else "delete", vehicle)
                    }
                },
                onUnarchiveVehicle = { vehicle ->
                    val id = vehicle.id ?: return@AdminUserForm
                    scope.launch {
                        val error = app.unarchiveAdminVehicle(id)
                        if (error != null) {
                            formError = error
                        } else {
                            form = draft.copy(
                                vehicles = draft.vehicles.map {
                                    if (it.key == vehicle.key) it.copy(archived = false) else it
                                },
                            )
                            app.reloadAdminUsers()
                        }
                    }
                },
            )
        }
    }

    detail?.let { opened ->
        val user = ui.adminUsers.firstOrNull { it.id == opened.id } ?: opened
        val canMutate = canMutateAdminUser(actorIsSuperAdmin, user.roles)
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
                LedgerRow(FIELD_ROLES, roleLabels(user.roles).joinToString(" · "))
                LedgerRow(FIELD_VOLUNTEER_STATUS, volunteerStatusLabel(user.volunteerStatus))
                LedgerRow("זמינות", availabilityText(user, today))
                otpUserLabel(user.otpLoginEnabled, user.otpUsersPageEnabled)?.let {
                    LedgerRow("OTP", it)
                }
                LedgerRow(FIELD_VEHICLES, "${user.vehicleCount}")
                if (isInvitePending(user.active, user.invitePending)) {
                    LedgerRow("חשבון", INVITE_PENDING_LABEL)
                } else {
                    LedgerRow("חשבון", if (user.active) "פעיל" else INACTIVE_ACCOUNT_LABEL)
                }
                user.addresses.filter { it.formattedAddress.isNotBlank() }.forEach { address ->
                    LedgerRow(
                        addressKindLabel(address.kind, address.label),
                        address.formattedAddress,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (!canMutate) {
                    Text(SUPER_ADMIN_LOCK_ERROR, style = TypeScale.caption, color = FieldTheme.textMuted)
                } else {
                    GhostButton(title = OVERFLOW_EDIT, onClick = {
                        formError = null
                        form = draftFromUser(user)
                        detail = null
                    })
                    GhostButton(
                        title = otpLoginActionLabel(user.otpLoginEnabled),
                        enabled = user.otpLoginEnabled || isValidIlMobile(user.phone),
                        onClick = {
                            if (user.otpLoginEnabled) {
                                scope.launch {
                                    val error = app.setAdminUserOtp(user.id, "login", false)
                                    if (error != null) app.showToast(error, StampTone.PENDING)
                                }
                            } else if (!isValidIlMobile(user.phone)) {
                                app.showToast(OTP_PHONE_REQUIRED, StampTone.PENDING)
                            } else {
                                confirm = AdminConfirm.Otp(user, "login")
                            }
                        },
                    )
                    if (canToggleUsersPageOtp(user.roles)) {
                        GhostButton(
                            title = otpUsersPageActionLabel(user.otpUsersPageEnabled),
                            enabled = user.otpUsersPageEnabled || isValidIlMobile(user.phone),
                            onClick = {
                                if (user.otpUsersPageEnabled) {
                                    scope.launch {
                                        val error = app.setAdminUserOtp(user.id, "users_page", false)
                                        if (error != null) app.showToast(error, StampTone.PENDING)
                                    }
                                } else if (!isValidIlMobile(user.phone)) {
                                    app.showToast(OTP_PHONE_REQUIRED, StampTone.PENDING)
                                } else {
                                    confirm = AdminConfirm.Otp(user, "users_page")
                                }
                            },
                        )
                    }
                    if (isInvitePending(user.active, user.invitePending)) {
                        GhostButton(title = OVERFLOW_RESEND_INVITE, onClick = {
                            scope.launch {
                                val result = app.resendAdminInvite(user.id)
                                if (result.error != null) {
                                    app.showToast(result.error, StampTone.PENDING)
                                } else {
                                    val copied = copyInviteLink(context, result.actionLink)
                                    app.showToast(
                                        if (copied) INVITE_RESENT_COPIED else (result.message ?: OVERFLOW_RESEND_INVITE),
                                        StampTone.DONE,
                                    )
                                }
                            }
                        })
                        GhostButton(title = OVERFLOW_COPY_INVITE_LINK, onClick = {
                            scope.launch {
                                val result = app.copyAdminInviteLink(user.id)
                                if (result.error != null) {
                                    app.showToast(result.error, StampTone.PENDING)
                                } else {
                                    val copied = copyInviteLink(context, result.actionLink)
                                    app.showToast(
                                        if (copied) INVITE_LINK_COPIED else INVITE_LINK_COPY_FAILED,
                                        if (copied) StampTone.DONE else StampTone.PENDING,
                                    )
                                }
                            }
                        })
                    }
                    GhostButton(
                        title = setActiveActionLabel(!user.active),
                        danger = user.active,
                        onClick = {
                            if (user.active) {
                                confirm = AdminConfirm.Deactivate(user)
                            } else {
                                scope.launch {
                                    val error = app.setUserActive(user.id, true)
                                    if (error != null) app.showToast(error, StampTone.PENDING)
                                }
                            }
                        },
                    )
                    if (user.id != ui.userId) {
                        GhostButton(title = OVERFLOW_DELETE, danger = true, onClick = {
                            confirm = AdminConfirm.Delete(user)
                        })
                    }
                }
                TextButton(onClick = { detail = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }

    confirm?.let { opened ->
        ModalBottomSheet(onDismissRequest = { if (!saving) confirm = null }) {
            when (opened) {
                is AdminConfirm.Deactivate -> ConfirmSheet(
                    title = deactivateConfirmTitle(opened.user.fullName),
                    body = DEACTIVATE_USER_BODY,
                    action = DEACTIVATE_USER_ACTION,
                    danger = true,
                    saving = saving,
                    onCancel = { confirm = null },
                    onConfirm = {
                        scope.launch {
                            saving = true
                            val error = app.setUserActive(opened.user.id, false)
                            saving = false
                            confirm = null
                            if (error != null) app.showToast(error, StampTone.PENDING)
                        }
                    },
                )
                is AdminConfirm.Delete -> ConfirmSheet(
                    title = deleteUserConfirm(opened.user.fullName),
                    body = DELETE_USER_BODY,
                    action = DELETE_USER_ACTION,
                    danger = true,
                    saving = saving,
                    onCancel = { confirm = null },
                    onConfirm = {
                        if (opened.user.id == ui.userId) {
                            app.showToast(SELF_DELETE_ERROR, StampTone.PENDING)
                            confirm = null
                            return@ConfirmSheet
                        }
                        scope.launch {
                            saving = true
                            val error = app.deleteAdminUser(opened.user.id)
                            saving = false
                            confirm = null
                            detail = null
                            if (form?.id == opened.user.id) form = null
                            if (error != null) app.showToast(error, StampTone.PENDING)
                        }
                    },
                )
                is AdminConfirm.Otp -> ConfirmSheet(
                    title = if (opened.kind == "users_page") OTP_ENABLE_USERS_PAGE_TITLE else OTP_ENABLE_LOGIN_TITLE,
                    body = "יישלח קוד SMS ל־${formatPhone(opened.user.phone.orEmpty())} כאשר יידרש אימות.",
                    action = OTP_ENABLE_ACTION,
                    danger = false,
                    saving = saving,
                    onCancel = { confirm = null },
                    onConfirm = {
                        scope.launch {
                            saving = true
                            val error = app.setAdminUserOtp(opened.user.id, opened.kind, true)
                            saving = false
                            confirm = null
                            if (error != null) app.showToast(error, StampTone.PENDING)
                        }
                    },
                )
                is AdminConfirm.Vehicle -> ConfirmSheet(
                    title = if (opened.mode == "archive") "העברה לארכיון" else "מחיקת רכב",
                    body = if (opened.mode == "archive") VEHICLE_ARCHIVE_CONFIRM else VEHICLE_DELETE_CONFIRM,
                    action = if (opened.mode == "archive") "העברה לארכיון" else "מחיקה",
                    danger = opened.mode != "archive",
                    saving = saving,
                    onCancel = { confirm = null },
                    onConfirm = {
                        val current = form ?: return@ConfirmSheet
                        val vehicle = opened.vehicle
                        val vehicleId = vehicle.id
                        if (vehicleId.isNullOrEmpty() || current.id == null) {
                            form = current.copy(vehicles = current.vehicles.filterNot { it.key == vehicle.key })
                            confirm = null
                            return@ConfirmSheet
                        }
                        scope.launch {
                            saving = true
                            val error = if (opened.mode == "archive") {
                                app.archiveAdminVehicle(vehicleId)
                            } else {
                                app.deleteAdminVehicle(vehicleId)
                            }
                            saving = false
                            if (error != null) {
                                formError = error
                            } else {
                                form = if (opened.mode == "archive") {
                                    current.copy(
                                        vehicles = current.vehicles.map {
                                            if (it.key == vehicle.key) it.copy(archived = true) else it
                                        },
                                    )
                                } else {
                                    current.copy(vehicles = current.vehicles.filterNot { it.key == vehicle.key })
                                }
                                app.reloadAdminUsers()
                                confirm = null
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfirmSheet(
    title: String,
    body: String,
    action: String,
    danger: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = TypeScale.section, color = FieldTheme.textPrimary)
        Text(body, style = TypeScale.body, color = FieldTheme.textSecondary)
        if (danger) {
            GhostButton(title = action, danger = true, enabled = !saving, onClick = onConfirm)
        } else {
            PrimaryButton(title = action, busy = saving, onClick = onConfirm)
        }
        TextButton(onClick = onCancel, enabled = !saving, modifier = Modifier.align(Alignment.End)) {
            Text("ביטול", color = FieldTheme.accent)
        }
    }
}

@Composable
private fun AdminUserForm(
    draft: InviteDraft,
    actorUserId: String?,
    addresses: List<AdminAddressItem>,
    saving: Boolean,
    formError: String?,
    onChange: (InviteDraft) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onRemoveVehicle: (AdminVehicleDraft) -> Unit,
    onUnarchiveVehicle: (AdminVehicleDraft) -> Unit,
) {
    val editing = draft.id != null
    val emailError = if (editing) null else createUserEmailError(draft.email)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolsBackRow(if (editing) USER_EDIT_TITLE else INVITE_TITLE, onClose)
        FormField(FIELD_FULL_NAME, draft.fullName, { onChange(draft.copy(fullName = it)) })
        FormField(
            label = FIELD_EMAIL,
            value = draft.email,
            onValueChange = { onChange(draft.copy(email = it)) },
            keyboardType = KeyboardType.Email,
            ltr = true,
            enabled = !editing,
            error = emailError,
        )
        Text(
            if (editing) EMAIL_LOCKED_HINT else EMAIL_INVITE_HINT,
            style = TypeScale.caption,
            color = FieldTheme.textMuted,
        )
        FormField(FIELD_CALLSIGN, draft.callsign, { onChange(draft.copy(callsign = it)) })
        FormField(
            label = FIELD_PHONE,
            value = draft.phone,
            onValueChange = { onChange(draft.copy(phone = formatPhone(it))) },
            keyboardType = KeyboardType.Phone,
            mono = true,
            ltr = true,
        )
        Text(PHONE_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
        OptionRowSelector(
            label = FIELD_VOLUNTEER_STATUS,
            options = VolunteerStatus.entries.map { it.raw to volunteerStatusLabel(it.raw) },
            selected = draft.volunteerStatus.raw,
            onSelect = { onChange(draft.copy(volunteerStatus = VolunteerStatus.fromRaw(it))) },
        )
        Text(FIELD_ROLES, style = TypeScale.label, color = FieldTheme.textSecondary)
        Text(ROLES_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
        FieldCard {
            INVITABLE_ROLES.forEach { role ->
                val lockOwnAdmin = editing &&
                    draft.id == actorUserId &&
                    role == AppRole.ADMIN &&
                    draft.roles.contains(AppRole.ADMIN.raw)
                val implied = isAssignableRoleLocked(draft.roles, role)
                val enabled = !lockOwnAdmin && !implied
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(enabled = enabled) {
                            onChange(
                                draft.copy(
                                    roles = toggleAssignableRole(draft.roles, role, !draft.roles.contains(role.raw)),
                                ),
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        roleLabels(listOf(role.raw)).firstOrNull().orEmpty(),
                        style = TypeScale.body,
                        color = if (enabled) FieldTheme.textPrimary else FieldTheme.textMuted,
                    )
                    if (draft.roles.contains(role.raw)) {
                        Text("נבחר", style = TypeScale.caption, color = FieldTheme.accent)
                    }
                }
            }
        }
        Text(FIELD_VEHICLES, style = TypeScale.label, color = FieldTheme.textSecondary)
        draft.vehicles.forEachIndexed { index, vehicle ->
            FieldCard {
                FormField(
                    label = VEHICLE_PLATE_LABEL,
                    value = vehicle.plateNumber,
                    onValueChange = {
                        val vehicles = draft.vehicles.toMutableList()
                        vehicles[index] = vehicle.copy(plateNumber = formatPlate(it))
                        onChange(draft.copy(vehicles = vehicles))
                    },
                    enabled = !vehicle.archived,
                    mono = true,
                    ltr = true,
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(8.dp))
                FormField(
                    label = VEHICLE_MODEL_LABEL,
                    value = vehicle.model,
                    onValueChange = {
                        val vehicles = draft.vehicles.toMutableList()
                        vehicles[index] = vehicle.copy(model = it)
                        onChange(draft.copy(vehicles = vehicles))
                    },
                    enabled = !vehicle.archived,
                )
                Spacer(Modifier.height(8.dp))
                if (vehicle.archived) {
                    Text(VEHICLE_ARCHIVED_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)
                    GhostButton(title = "שחזור מהארכיון", onClick = { onUnarchiveVehicle(vehicle) })
                } else {
                    GhostButton(title = "הסרת רכב", danger = true, onClick = { onRemoveVehicle(vehicle) })
                }
            }
        }
        GhostButton(
            title = ADD_VEHICLE,
            onClick = {
                onChange(
                    draft.copy(
                        vehicles = draft.vehicles + AdminVehicleDraft(key = "new-${System.currentTimeMillis()}"),
                    ),
                )
            },
        )
        if (editing) {
            addresses.filter { it.formattedAddress.isNotBlank() }.forEach { address ->
                LedgerRow(addressKindLabel(address.kind, address.label), address.formattedAddress)
            }
        }
        formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
        PrimaryButton(title = USER_SAVE_LABEL, busy = saving, onClick = onSave)
        TextButton(onClick = onClose, enabled = !saving, modifier = Modifier.align(Alignment.End)) {
            Text("ביטול", color = FieldTheme.accent)
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
        val tags = buildList {
            add(volunteerStatusLabel(user.volunteerStatus))
            otpUserLabel(user.otpLoginEnabled, user.otpUsersPageEnabled)?.let { add("OTP · $it") }
            if (isInvitePending(user.active, user.invitePending)) add(INVITE_PENDING_LABEL)
            if (!user.active) add(INACTIVE_ACCOUNT_LABEL)
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(tags.joinToString(" · "), style = TypeScale.caption, color = FieldTheme.textMuted)
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

private fun draftFromUser(user: AdminUserListItem): InviteDraft = InviteDraft(
    id = user.id,
    fullName = user.fullName,
    email = user.email,
    callsign = user.callsign,
    phone = user.phone?.let { formatPhone(it) }.orEmpty(),
    volunteerStatus = VolunteerStatus.fromRaw(user.volunteerStatus),
    roles = withImpliedAssignableRoles(user.roles),
    vehicles = user.vehicles.map { vehicle ->
        AdminVehicleDraft(
            key = vehicle.id,
            id = vehicle.id,
            plateNumber = formatPlate(vehicle.plateNumber),
            model = vehicle.model,
            archived = vehicle.archived,
        )
    },
)

private fun copyInviteLink(context: Context, actionLink: String?): Boolean {
    if (actionLink.isNullOrBlank()) return false
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("invite", actionLink))
        true
    } catch (_: Exception) {
        false
    }
}
