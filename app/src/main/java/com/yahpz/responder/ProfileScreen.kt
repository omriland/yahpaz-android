package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.ADD_VEHICLE
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.DEFAULT_VEHICLE_LABEL
import com.yahpz.domain.IMPERSONATION_AVAILABILITY_LOCKED
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.SET_DEFAULT_VEHICLE_LABEL
import com.yahpz.domain.StampTone
import com.yahpz.domain.VEHICLE_ARCHIVE_CONFIRM
import com.yahpz.domain.VEHICLE_ARCHIVED_CAPTION
import com.yahpz.domain.VEHICLE_DELETE_CONFIRM
import com.yahpz.domain.VEHICLE_MODEL_LABEL
import com.yahpz.domain.VEHICLE_PLATE_LABEL
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.canChooseDefaultVehicle
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.VehicleFieldsError
import com.yahpz.domain.formatPlate
import com.yahpz.domain.isProfileVehicleEditing
import com.yahpz.domain.israelToday
import com.yahpz.domain.plateDigits
import com.yahpz.domain.resolveCarLogoSlug
import com.yahpz.domain.vehicleFieldsForSave
import com.yahpz.domain.vehicleRemoveMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editingAvailability by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("פרופיל", style = TypeScale.title, color = FieldTheme.textPrimary)
        ui.profile?.let { profile ->
            FieldCard {
                LedgerRow("שם", profile.fullName)
                LedgerRow("או״ק", profile.callsign)
                LedgerRow("דוא״ל", profile.email)
                LedgerRow("טלפון", profile.phone.orEmpty())
            }
            if (!ui.mustChangePassword) {
                AvailabilityRow(
                    availability = profile.availability,
                    availableFrom = profile.availableFrom,
                    enabled = !ui.impersonating,
                    onClick = { editingAvailability = true },
                )
                VehiclesSection(
                    app = app,
                    userId = ui.userId,
                    vehicles = ui.vehicles,
                    loading = ui.vehiclesLoading,
                    failed = ui.vehiclesFailed,
                )
            }
            FieldCard {
                Text("סיכום פעילות", style = TypeScale.section, color = FieldTheme.textPrimary)
                LedgerRow("אירועים", profile.eventCount.toString())
                LedgerRow("קילומטרים", profile.km.toInt().toString())
            }
        }
        if (ui.mustChangePassword) {
            FieldCard {
                Text("יש לבחור סיסמה חדשה", style = TypeScale.section, color = FieldTheme.textPrimary)
                FormField("סיסמה חדשה", password, { password = it }, password = true, keyboardType = KeyboardType.Password, ltr = true)
                FormField("אימות סיסמה", confirm, { confirm = it }, password = true, keyboardType = KeyboardType.Password, ltr = true)
                error?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = "שמירת סיסמה",
                    busy = busy,
                    onClick = {
                        if (password != confirm) {
                            error = "הסיסמאות אינן זהות."
                            return@PrimaryButton
                        }
                        scope.launch {
                            busy = true
                            error = app.completePasswordChange(password)
                            busy = false
                        }
                    },
                )
            }
        }
        PrivacyPolicyLink(onOpen = { app.openPrivacy() })
        GhostButton(title = "יציאה", onClick = { app.signOut() })
    }

    if (editingAvailability) {
        ModalBottomSheet(onDismissRequest = { editingAvailability = false }) {
            AvailabilityScreen(
                app = app,
                ui = ui,
                onSaved = { editingAvailability = false },
            )
        }
    }
}

@Composable
private fun AvailabilityRow(
    availability: AvailabilityStatus,
    availableFrom: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val effective = effectiveAvailability(availability, availableFrom, israelToday())
    val label = availabilityLabel(effective)
    val caption = if (effective == AvailabilityStatus.UNAVAILABLE) {
        availabilityReturnCaption(availableFrom)
    } else {
        null
    }
    FieldCard(
        modifier = Modifier
            .clickable(enabled = enabled, onClickLabel = "עריכת זמינות", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "זמינות: $label"
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (effective == AvailabilityStatus.AVAILABLE) FieldTheme.done else FieldTheme.alert,
                        CircleShape,
                    ),
            )
            Column(Modifier.weight(1f)) {
                Text("זמינות: $label", style = TypeScale.bodyStrong, color = FieldTheme.textPrimary)
                if (caption != null) {
                    Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
                if (!enabled) {
                    Text(IMPERSONATION_AVAILABILITY_LOCKED, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FieldTheme.textMuted,
                )
            }
        }
    }
}

private data class ProfileVehicleDraft(
    val key: String,
    val id: String? = null,
    val plate: String = "",
    val model: String = "",
    val archived: Boolean = false,
    val isDefault: Boolean = false,
)

private fun ProfileVehicle.toDraft(): ProfileVehicleDraft = ProfileVehicleDraft(
    key = id ?: "plate-$plate",
    id = id,
    plate = formatPlate(plate),
    model = model,
    archived = archived,
    isDefault = isDefault,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiclesSection(
    app: AppModel,
    userId: String?,
    vehicles: List<ProfileVehicle>,
    loading: Boolean,
    failed: Boolean,
) {
    val scope = rememberCoroutineScope()
    var drafts by remember { mutableStateOf(vehicles.map { it.toDraft() }) }
    var confirm by remember { mutableStateOf<ProfileVehicleDraft?>(null) }
    var confirmMode by remember { mutableStateOf("delete") }
    var saving by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    val canStar = canChooseDefaultVehicle(drafts.map {
        ProfileVehicle(it.plate, it.model, it.id, it.archived, it.isDefault)
    })

    LaunchedEffect(vehicles) {
        val savedPlates = vehicles.map { plateDigits(it.plate) }.toSet()
        val unsaved = drafts.filter {
            it.id == null && plateDigits(it.plate).let { plate -> plate.isEmpty() || plate !in savedPlates }
        }
        drafts = vehicles.map { it.toDraft() } + unsaved
    }

    fun patch(key: String, transform: (ProfileVehicleDraft) -> ProfileVehicleDraft) {
        drafts = drafts.map { if (it.key == key) transform(it) else it }
    }

    fun persist(key: String) {
        val vehicle = drafts.find { it.key == key } ?: return
        if (vehicle.archived) return
        val fields = vehicleFieldsForSave(vehicle.plate, vehicle.model)
        if (fields is VehicleFieldsError) {
            app.showToast(fields.message, StampTone.PENDING)
            return
        }
        scope.launch {
            val error = if (vehicle.id == null) {
                app.createOwnVehicle(vehicle.plate, vehicle.model)
            } else {
                app.updateOwnVehicle(vehicle.id, vehicle.plate, vehicle.model)
            }
            if (error != null) {
                app.showToast(error, StampTone.PENDING)
                return@launch
            }
            editingKey = null
            if (vehicle.id == null) {
                drafts = drafts.filterNot { it.key == key }
                app.showToast("הרכב נשמר.", StampTone.DONE)
            }
            app.reloadVehicles()
        }
    }

    fun addVehicle() {
        val emptyNew = drafts.find {
            it.id == null && it.plate.isBlank() && it.model.isBlank()
        }
        if (emptyNew != null) {
            editingKey = emptyNew.key
            return
        }
        drafts.filter { it.id == null }.forEach { persist(it.key) }
        editingKey?.let { key ->
            val current = drafts.find { it.key == key }
            if (current?.id != null && !current.archived) persist(key)
        }
        val blank = ProfileVehicleDraft(key = "new-${System.currentTimeMillis()}")
        drafts = drafts + blank
        editingKey = blank.key
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("הרכבים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
            TextButton(
                onClick = { addVehicle() },
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = FieldTheme.accent,
                )
                Text(ADD_VEHICLE, color = FieldTheme.accent)
            }
        }
        if (canStar) {
            Text(
                "לחצו על הכוכב כדי לבחור רכב ראשי לאירועים ולמשמרות.",
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
        }
        when {
            failed && vehicles.isEmpty() && drafts.isEmpty() -> EmptyState(
                title = "טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadVehicles() } },
            )
            loading && vehicles.isEmpty() && drafts.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent, modifier = Modifier.size(28.dp))
            }
            else -> {
                if (drafts.isEmpty()) {
                    Text(
                        "עדיין לא רשומים רכבים.",
                        style = TypeScale.body,
                        color = FieldTheme.textSecondary,
                    )
                }
                drafts.forEach { vehicle ->
                    val editing = isProfileVehicleEditing(vehicle.id, vehicle.key, editingKey)
                    if (!editing) {
                        FieldCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CarLogo(slug = resolveCarLogoSlug(vehicle.model))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (vehicle.archived) {
                                            "${vehicle.model} (בארכיון)"
                                        } else {
                                            vehicle.model.ifBlank { "—" }
                                        },
                                        style = TypeScale.label,
                                        color = FieldTheme.textSecondary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    LicensePlate(plate = vehicle.plate)
                                }
                                if (!vehicle.archived && canStar && vehicle.id != null) {
                                    IconButton(
                                        onClick = {
                                            if (vehicle.isDefault) return@IconButton
                                            val id = vehicle.id
                                            scope.launch {
                                                val error = app.setDefaultVehicle(id)
                                                if (error != null) {
                                                    app.showToast(error, StampTone.PENDING)
                                                    return@launch
                                                }
                                                app.showToast("הרכב הראשי עודכן.", StampTone.DONE)
                                                app.reloadVehicles()
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = if (vehicle.isDefault) {
                                                DEFAULT_VEHICLE_LABEL
                                            } else {
                                                SET_DEFAULT_VEHICLE_LABEL
                                            },
                                            tint = if (vehicle.isDefault) {
                                                FieldTheme.accent
                                            } else {
                                                FieldTheme.textMuted
                                            },
                                        )
                                    }
                                }
                                if (vehicle.archived) {
                                    IconButton(
                                        onClick = {
                                            val id = vehicle.id ?: return@IconButton
                                            scope.launch {
                                                val error = app.unarchiveAdminVehicle(id)
                                                if (error != null) {
                                                    app.showToast(error, StampTone.PENDING)
                                                } else {
                                                    app.showToast("הרכב שוחזר מהארכיון", StampTone.DONE)
                                                    app.reloadVehicles()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Unarchive,
                                            contentDescription = "שחזור מהארכיון",
                                            tint = FieldTheme.accent,
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            editingKey?.let { key ->
                                                if (key != vehicle.key) persist(key)
                                            }
                                            editingKey = vehicle.key
                                        },
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "עריכת רכב",
                                            tint = FieldTheme.accent,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                    FieldCard {
                        FormField(
                            label = VEHICLE_PLATE_LABEL,
                            value = vehicle.plate,
                            onValueChange = { patch(vehicle.key) { row -> row.copy(plate = formatPlate(it)) } },
                            enabled = !vehicle.archived,
                            mono = true,
                            ltr = true,
                            keyboardType = KeyboardType.Number,
                            onSubmit = { persist(vehicle.key) },
                        )
                        Spacer(Modifier.height(8.dp))
                        FormField(
                            label = VEHICLE_MODEL_LABEL,
                            value = vehicle.model,
                            onValueChange = { patch(vehicle.key) { row -> row.copy(model = it) } },
                            enabled = !vehicle.archived,
                            onSubmit = { persist(vehicle.key) },
                        )
                        Spacer(Modifier.height(8.dp))
                        if (vehicle.archived) {
                            Text(VEHICLE_ARCHIVED_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)
                            GhostButton(
                                title = "שחזור מהארכיון",
                                onClick = {
                                    val id = vehicle.id ?: return@GhostButton
                                    scope.launch {
                                        val error = app.unarchiveAdminVehicle(id)
                                        if (error != null) app.showToast(error, StampTone.PENDING)
                                        else {
                                            app.showToast("הרכב שוחזר מהארכיון", StampTone.DONE)
                                            app.reloadVehicles()
                                        }
                                    }
                                },
                            )
                        } else {
                            Row {
                                IconButton(
                                    onClick = { persist(vehicle.key) },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "שמירת רכב",
                                        tint = FieldTheme.accent,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (vehicle.id == null || userId == null) {
                                            confirmMode = "delete"
                                            confirm = vehicle
                                            return@IconButton
                                        }
                                        scope.launch {
                                            val attached = app.isVehicleAttachedToEvents(
                                                userId,
                                                vehicle.id,
                                                vehicle.plate,
                                            )
                                            confirmMode = vehicleRemoveMode(attached)
                                            confirm = vehicle
                                        }
                                    },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "הסרת רכב",
                                        tint = FieldTheme.alert,
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (confirm != null) {
        ModalBottomSheet(onDismissRequest = { if (!saving) confirm = null }) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (confirmMode == "archive") "העברה לארכיון" else "מחיקת רכב",
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(
                    if (confirmMode == "archive") VEHICLE_ARCHIVE_CONFIRM else VEHICLE_DELETE_CONFIRM,
                    style = TypeScale.body,
                    color = FieldTheme.textSecondary,
                )
                if (confirmMode == "archive") {
                    PrimaryButton(
                        title = "העברה לארכיון",
                        busy = saving,
                        onClick = {
                            val vehicle = confirm ?: return@PrimaryButton
                            val id = vehicle.id
                            if (id == null) {
                                drafts = drafts.filterNot { it.key == vehicle.key }
                                editingKey = null
                                confirm = null
                                return@PrimaryButton
                            }
                            scope.launch {
                                saving = true
                                val error = app.archiveAdminVehicle(id)
                                saving = false
                                if (error != null) {
                                    app.showToast(error, StampTone.PENDING)
                                    return@launch
                                }
                                app.showToast("הרכב הועבר לארכיון", StampTone.DONE)
                                editingKey = null
                                confirm = null
                                app.reloadVehicles()
                            }
                        },
                    )
                } else {
                    GhostButton(
                        title = "מחיקה",
                        danger = true,
                        enabled = !saving,
                        onClick = {
                            val vehicle = confirm ?: return@GhostButton
                            val id = vehicle.id
                            if (id == null) {
                                drafts = drafts.filterNot { it.key == vehicle.key }
                                editingKey = null
                                confirm = null
                                return@GhostButton
                            }
                            scope.launch {
                                saving = true
                                val error = app.deleteAdminVehicle(id)
                                saving = false
                                if (error != null) {
                                    app.showToast(error, StampTone.PENDING)
                                    return@launch
                                }
                                app.showToast("הרכב נמחק", StampTone.DONE)
                                editingKey = null
                                confirm = null
                                app.reloadVehicles()
                            }
                        },
                    )
                }
                TextButton(onClick = { if (!saving) confirm = null }, enabled = !saving) {
                    Text("ביטול", color = FieldTheme.accent)
                }
            }
        }
    }
}
