package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.israelToday
import com.yahpz.domain.resolveCarLogoSlug
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
                    onClick = { editingAvailability = true },
                )
                VehiclesSection(
                    vehicles = ui.vehicles,
                    loading = ui.vehiclesLoading,
                    failed = ui.vehiclesFailed,
                    onRetry = { scope.launch { app.reloadVehicles() } },
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
                FormField("סיסמה חדשה", password, { password = it }, password = true, keyboardType = KeyboardType.Password)
                FormField("אימות סיסמה", confirm, { confirm = it }, password = true, keyboardType = KeyboardType.Password)
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
        PrivacyPolicyLink()
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
            .clickable(onClickLabel = "עריכת זמינות", onClick = onClick)
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
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = FieldTheme.textMuted,
            )
        }
    }
}

@Composable
private fun VehiclesSection(
    vehicles: List<ProfileVehicle>,
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("הרכבים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
        when {
            failed && vehicles.isEmpty() -> EmptyState(
                title = "טעינת הרכבים נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = onRetry,
            )
            loading && vehicles.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent, modifier = Modifier.size(28.dp))
            }
            vehicles.isEmpty() -> Text(
                "לא מקושר רכב. פנו למנהל המערכת.",
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
            )
            else -> FieldCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    vehicles.forEach { vehicle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CarLogo(slug = resolveCarLogoSlug(vehicle.model))
                            LicensePlate(plate = vehicle.plate)
                            if (vehicle.model.isNotEmpty()) {
                                Text(
                                    text = vehicle.model,
                                    style = TypeScale.body,
                                    color = FieldTheme.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
