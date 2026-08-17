package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillMode
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.ResponderFillErrors
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatDateTime
import com.yahpz.domain.participationStamp
import com.yahpz.domain.validateResponderFillDraft
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillScreen(eventId: String, app: AppModel) {
    val scope = rememberCoroutineScope()
    var context by remember { mutableStateOf<FillContext?>(null) }
    var draft by remember { mutableStateOf(ResponderFillDraft()) }
    var errors by remember { mutableStateOf(ResponderFillErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var savingDraft by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        failed = false
        try {
            val next = YahpazAPI.fetchFillContext(eventId)
            context = next
            if (next == null) failed = true else draft = next.draft
        } catch (_: Exception) {
            failed = true
        }
        loading = false
    }

    LaunchedEffect(eventId) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { app.closeFill() }) {
                Text("חזרה", color = FieldTheme.accent, style = TypeScale.body)
            }
            Text("השלמת הפרטים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
        }
        when {
            loading -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent)
                Spacer(Modifier.height(12.dp))
                Text("טוען את הדיווח…", style = TypeScale.body, color = FieldTheme.textSecondary)
            }
            failed || context == null -> EmptyState(
                title = "טעינת הדיווח נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { load() } },
            )
            else -> {
                val fill = context!!
                val readOnly = fill.participationStatus == ParticipationStatus.DONE ||
                    fill.eventStatus == EventStatus.DONE || fill.isCancelled
                Column(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FieldCard {
                            LedgerRow("תאריך", formatDate(fill.eventDate))
                            LedgerRow("מספר אירוע", fill.policeEventId.orEmpty())
                            LedgerRow("סוג אירוע", fill.eventTypeName.orEmpty())
                            LedgerRow("כביש", fill.roadName.orEmpty())
                            LedgerRow("מיקום", fill.location.orEmpty())
                            LedgerRow("אחמ״ש", fill.shiftLeadName.orEmpty())
                        }
                        if (fill.isCancelled) {
                            Text(
                                "האירוע נסגר. לא ניתן לערוך את הדיווח.",
                                style = TypeScale.body,
                                color = FieldTheme.textPrimary,
                                modifier = Modifier
                                    .background(FieldTheme.accentSubtle, RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                            )
                        }
                        if (readOnly) {
                            StampChip(participationStamp(fill.participationStatus, true))
                            fill.updatedAt?.let {
                                Text(
                                    "הדיווח הושלם ב־${formatDateTime(it)}. רק אחמ״ש יכול לערוך לאחר סיום.",
                                    style = TypeScale.caption,
                                    color = FieldTheme.textMuted,
                                )
                            }
                        }
                        Text("הפרטים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
                        if (fill.vehicles.isEmpty()) {
                            Text("לא מקושר רכב למשתמש. פנו למנהל המערכת.", style = TypeScale.caption, color = FieldTheme.alert)
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            val selectedLabel = fill.vehicles.firstOrNull { it.plate == draft.vehiclePlate }?.label ?: "בחירת רכב"
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("לוחית רישוי", style = TypeScale.label, color = FieldTheme.textSecondary)
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!readOnly) expanded = it }) {
                                    TextField(
                                        value = selectedLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = !readOnly,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (errors.vehiclePlate == null) FieldTheme.strong else FieldTheme.alert,
                                                RoundedCornerShape(4.dp),
                                            ),
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("בחירת רכב") }, onClick = {
                                            draft = draft.copy(vehiclePlate = "")
                                            expanded = false
                                        })
                                        fill.vehicles.forEach { vehicle ->
                                            DropdownMenuItem(text = { Text(vehicle.label) }, onClick = {
                                                draft = draft.copy(vehiclePlate = vehicle.plate)
                                                expanded = false
                                            })
                                        }
                                    }
                                }
                                errors.vehiclePlate?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                            }
                        }
                        FormField("מד אוץ התחלה", draft.odometerStart, { draft = draft.copy(odometerStart = it) }, keyboardType = KeyboardType.Number, mono = true, error = errors.odometerStart, enabled = !readOnly)
                        FormField("מד אוץ סיום", draft.odometerEnd, { draft = draft.copy(odometerEnd = it) }, keyboardType = KeyboardType.Number, mono = true, error = errors.odometerEnd, enabled = !readOnly)
                        FormArea("נתיב נסיעה", draft.route, { draft = draft.copy(route = it) }, error = errors.route, enabled = !readOnly)
                        FormArea("פירוט הטיפול", draft.treatmentDetail, { draft = draft.copy(treatmentDetail = it) }, error = errors.treatmentDetail, enabled = !readOnly)
                        FormArea("הערות לטיפול", draft.treatmentNotes, { draft = draft.copy(treatmentNotes = it) }, minHeight = 80, enabled = !readOnly)
                        formError?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
                        Spacer(Modifier.height(80.dp))
                    }
                    if (!readOnly) {
                        Column(
                            modifier = Modifier
                                .background(FieldTheme.raised)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PrimaryButton(title = "סיום דיווח", busy = completing, onClick = {
                                scope.launch {
                                    formError = null
                                    errors = validateResponderFillDraft(
                                        draft,
                                        FillMode.COMPLETE,
                                        fill.vehicles.map { it.plate },
                                        fill.totalKm,
                                    )
                                    completing = true
                                    val error = YahpazAPI.saveFill(fill, draft, true)
                                    completing = false
                                    if (error != null) {
                                        formError = error
                                        app.showToast(error, com.yahpz.domain.StampTone.PENDING)
                                    } else {
                                        app.showToast("הדיווח הושלם")
                                        app.reloadEvents()
                                        app.closeFill()
                                    }
                                }
                            })
                            GhostButton(
                                title = "שמירת טיוטה",
                                enabled = !savingDraft && !completing,
                                onClick = {
                                    scope.launch {
                                        formError = null
                                        errors = validateResponderFillDraft(
                                            draft,
                                            FillMode.DRAFT,
                                            fill.vehicles.map { it.plate },
                                            fill.totalKm,
                                        )
                                        savingDraft = true
                                        val error = YahpazAPI.saveFill(fill, draft, false)
                                        savingDraft = false
                                        if (error != null) {
                                            formError = error
                                            app.showToast(error, com.yahpz.domain.StampTone.PENDING)
                                        } else {
                                            app.showToast("הטיוטה נשמרה")
                                            app.reloadEvents()
                                            app.closeFill()
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
