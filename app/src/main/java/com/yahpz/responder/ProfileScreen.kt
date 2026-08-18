package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

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
}
