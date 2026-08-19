package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.IMPERSONATION_AVAILABILITY_LOCKED
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.israelToday
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.returnDateToInput
import kotlinx.coroutines.launch

private val choiceShape = RoundedCornerShape(4.dp)

@Composable
fun AvailabilityScreen(app: AppModel, ui: AppUiState, onSaved: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(ui.profile?.availability ?: AvailabilityStatus.AVAILABLE) }
    var returnDate by remember { mutableStateOf(returnDateToInput(ui.profile?.availableFrom.orEmpty())) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val locked = ui.impersonating

    LaunchedEffect(ui.profile?.id, ui.profile?.availability, ui.profile?.availableFrom) {
        ui.profile?.let {
            status = it.availability
            returnDate = returnDateToInput(it.availableFrom.orEmpty())
        }
    }

    val isoReturn = if (returnDate.isEmpty()) null else normalizeReturnDate(returnDate)
    val effective = effectiveAvailability(
        status,
        isoReturn,
        israelToday(),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("זמינות", style = TypeScale.title, color = FieldTheme.textPrimary)
        Text("הסטטוס יוצג לאחמ״ש בשיבוץ לאירוע.", style = TypeScale.body, color = FieldTheme.textSecondary)
        if (locked) {
            Text(IMPERSONATION_AVAILABILITY_LOCKED, style = TypeScale.caption, color = FieldTheme.textMuted)
        }
        FieldCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (effective == AvailabilityStatus.AVAILABLE) FieldTheme.done else FieldTheme.alert,
                            CircleShape,
                        ),
                )
                Text("זמינות: ${availabilityLabel(effective)}", style = TypeScale.bodyStrong, color = FieldTheme.textPrimary)
                if (effective == AvailabilityStatus.UNAVAILABLE) {
                    availabilityReturnCaption(isoReturn)?.let {
                        Text(it, style = TypeScale.caption, color = FieldTheme.textMuted)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvailabilityChoice(
                label = "זמין",
                selected = status == AvailabilityStatus.AVAILABLE,
                enabled = !locked,
                onClick = { status = AvailabilityStatus.AVAILABLE },
                modifier = Modifier.weight(1f),
            )
            AvailabilityChoice(
                label = "לא זמין",
                selected = status == AvailabilityStatus.UNAVAILABLE,
                enabled = !locked,
                onClick = { status = AvailabilityStatus.UNAVAILABLE },
                modifier = Modifier.weight(1f),
            )
        }
        if (status == AvailabilityStatus.UNAVAILABLE) {
            ReturnDateField(
                label = "תאריך חזרה (לא חובה)",
                value = returnDate,
                onValueChange = { if (!locked) returnDate = it },
            )
            Text("ניתן לבחור רק תאריך עתידי", style = TypeScale.caption, color = FieldTheme.textMuted)
        }
        error?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
        PrimaryButton(
            title = "שמירת זמינות",
            busy = busy,
            enabled = !locked,
            onClick = {
                scope.launch {
                    busy = true
                    error = app.saveAvailability(
                        status,
                        if (status == AvailabilityStatus.UNAVAILABLE && returnDate.isNotBlank()) returnDate else null,
                    )
                    busy = false
                    if (error == null) onSaved()
                }
            },
        )
    }
}

@Composable
private fun AvailabilityChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (selected) FieldTheme.accentSubtle else FieldTheme.raised, choiceShape)
            .border(1.dp, if (selected) FieldTheme.accent else FieldTheme.strong, choiceShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (selected) TypeScale.bodyStrong else TypeScale.body,
            color = when {
                !enabled -> FieldTheme.textMuted
                selected -> FieldTheme.accent
                else -> FieldTheme.textPrimary
            },
        )
    }
}
