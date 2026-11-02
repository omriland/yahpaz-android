package com.yahpz.responder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_BODY
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_CANCEL
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_EMPTY_ERROR
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_FIELD
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_HINT
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_SEND
import com.yahpz.domain.PERSONAL_VEHICLE_PROMPT_TITLE
import com.yahpz.domain.personalVehicleWhatsAppHref

/** Asks a volunteer with no vehicle for their plate and opens WhatsApp to support. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalVehiclePromptSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var vehicleNumber by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(PERSONAL_VEHICLE_PROMPT_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
            Text(PERSONAL_VEHICLE_PROMPT_BODY, style = TypeScale.body, color = FieldTheme.textPrimary)
            Text(PERSONAL_VEHICLE_PROMPT_HINT, style = TypeScale.body, color = FieldTheme.textPrimary)
            FormField(
                label = PERSONAL_VEHICLE_PROMPT_FIELD,
                value = vehicleNumber,
                onValueChange = {
                    vehicleNumber = it
                    error = null
                },
                error = error,
            )
            PrimaryButton(title = PERSONAL_VEHICLE_PROMPT_SEND, onClick = {
                val href = personalVehicleWhatsAppHref(vehicleNumber)
                if (href == null) {
                    error = PERSONAL_VEHICLE_PROMPT_EMPTY_ERROR
                    return@PrimaryButton
                }
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href))) }
                onDismiss()
            })
            GhostButton(title = PERSONAL_VEHICLE_PROMPT_CANCEL, onClick = onDismiss)
        }
    }
}
