package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.yahpz.domain.availabilityLabel
import com.yahpz.domain.availabilityReturnCaption
import com.yahpz.domain.effectiveAvailability
import com.yahpz.domain.israelToday
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.returnDateToInput
import kotlinx.coroutines.launch

@Composable
fun AvailabilityScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(ui.profile?.availability ?: AvailabilityStatus.AVAILABLE) }
    var returnDate by remember { mutableStateOf(returnDateToInput(ui.profile?.availableFrom.orEmpty())) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

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
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("זמינות", style = TypeScale.title, color = FieldTheme.textPrimary)
        Text("הסטטוס מוצג לאחמ״ש בשיבוץ לאירוע.", style = TypeScale.body, color = FieldTheme.textSecondary)
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
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = status == AvailabilityStatus.AVAILABLE,
                onClick = { status = AvailabilityStatus.AVAILABLE },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("זמין") }
            SegmentedButton(
                selected = status == AvailabilityStatus.UNAVAILABLE,
                onClick = { status = AvailabilityStatus.UNAVAILABLE },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("לא זמין") }
        }
        if (status == AvailabilityStatus.UNAVAILABLE) {
            ReturnDateField(
                label = "תאריך חזרה (לא חובה)",
                value = returnDate,
                onValueChange = { returnDate = it },
            )
            Text("בחרו תאריך מהמחר או השאירו ריק.", style = TypeScale.caption, color = FieldTheme.textMuted)
        }
        error?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
        PrimaryButton(
            title = "שמירת זמינות",
            busy = busy,
            onClick = {
                scope.launch {
                    busy = true
                    error = app.saveAvailability(
                        status,
                        if (status == AvailabilityStatus.UNAVAILABLE && returnDate.isNotBlank()) returnDate else null,
                    )
                    busy = false
                }
            },
        )
    }
}
