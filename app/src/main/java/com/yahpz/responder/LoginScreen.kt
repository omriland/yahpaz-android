package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    email: String,
    password: String,
    mode: LoginMode,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CommandTheme.page),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("אבן דרך", style = TypeScale.brand, color = CommandTheme.textPrimary)
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(52.dp)
                        .background(CommandTheme.hairline),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("היחידה הארצית", style = TypeScale.label, color = CommandTheme.textSecondary)
                    Text("לפינוי צירים", style = TypeScale.label, color = CommandTheme.textSecondary)
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier
                    .background(FieldTheme.raised, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(20.dp),
            ) {
                Text(
                    if (mode == LoginMode.SIGNIN) "כניסה למערכת" else "איפוס סיסמה",
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        error,
                        style = TypeScale.body,
                        color = FieldTheme.alert,
                        modifier = Modifier
                            .background(FieldTheme.alertTint, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .padding(12.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                when (mode) {
                    LoginMode.RESET_SENT -> {
                        Text(
                            "אם קיים חשבון לכתובת זו, נשלח קישור לאיפוס הסיסמה.",
                            style = TypeScale.body,
                            color = FieldTheme.textSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        GhostButton(title = "חזרה לכניסה", onClick = onToggleMode)
                    }
                    else -> {
                        FormField(
                            label = "דוא״ל",
                            value = email,
                            onValueChange = onEmail,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                            ltr = true,
                            imeAction = if (mode == LoginMode.SIGNIN) {
                                androidx.compose.ui.text.input.ImeAction.Next
                            } else {
                                androidx.compose.ui.text.input.ImeAction.Go
                            },
                            onSubmit = if (mode == LoginMode.RESET) onSubmit else null,
                        )
                        if (mode == LoginMode.SIGNIN) {
                            Spacer(Modifier.height(16.dp))
                            FormField(
                                label = "סיסמה",
                                value = password,
                                onValueChange = onPassword,
                                password = true,
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                ltr = true,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                                onSubmit = onSubmit,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            title = if (mode == LoginMode.SIGNIN) "כניסה" else "שליחת קישור",
                            onClick = onSubmit,
                            busy = busy,
                            enabled = email.isNotBlank(),
                        )
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onToggleMode,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                if (mode == LoginMode.SIGNIN) "שכחתי סיסמה" else "חזרה לכניסה",
                                style = TypeScale.body,
                                color = FieldTheme.accent,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PrivacyPolicyLink(onOpen = onOpenPrivacy, command = true)
        }
        if (busy && mode != LoginMode.RESET_SENT) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                color = CommandTheme.accent,
            )
        }
    }
}

enum class LoginMode { SIGNIN, RESET, RESET_SENT }
