package com.yahpz.responder

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.applyReturnDateKeystroke

private val fieldShape = RoundedCornerShape(4.dp)
private val cardShape = RoundedCornerShape(8.dp)

@Composable
fun StampChip(stamp: StampDescriptor, modifier: Modifier = Modifier) {
    Text(
        text = stamp.label,
        style = TypeScale.stamp,
        color = stamp.tone.ink,
        modifier = modifier
            .background(stamp.tone.tint, RoundedCornerShape(3.dp))
            .border(1.dp, stamp.tone.ink.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun PrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    enabled: Boolean = true,
    command: Boolean = false,
) {
    val background = when {
        !enabled -> FieldTheme.textMuted
        command -> CommandTheme.accentFill
        else -> FieldTheme.accent
    }
    TextButton(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(background, fieldShape),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = FieldTheme.textOnAccent,
                strokeWidth = 2.dp,
            )
        } else {
            Text(title, style = TypeScale.bodyStrong, color = FieldTheme.textOnAccent)
        }
    }
}

@Composable
fun GhostButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(1.dp, FieldTheme.strong, fieldShape),
    ) {
        Text(
            title,
            style = TypeScale.bodyStrong,
            color = if (enabled) FieldTheme.accent else FieldTheme.textMuted,
        )
    }
}

@Composable
fun PrivacyPolicyLink(command: Boolean = false) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("${AppConfig.appOrigin}/privacy")),
            )
        },
    ) {
        Text(
            "מדיניות פרטיות",
            style = TypeScale.caption,
            color = if (command) CommandTheme.textSecondary else FieldTheme.textMuted,
        )
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    mono: Boolean = false,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    password: Boolean = false,
    onSubmit: (() -> Unit)? = null,
    enabled: Boolean = true,
    placeholder: String? = null,
    ltr: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        CompositionLocalProvider(
            LocalLayoutDirection provides if (ltr) LayoutDirection.Ltr else LocalLayoutDirection.current,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = (if (mono) TypeScale.numeric else TypeScale.body).let { style ->
                    if (ltr) style.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left) else style
                },
                placeholder = placeholder?.let { hint ->
                    { Text(hint, style = if (mono) TypeScale.numeric else TypeScale.body, color = FieldTheme.textMuted) }
                },
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onSubmit?.invoke() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldTheme.raised,
                    unfocusedContainerColor = FieldTheme.raised,
                    disabledContainerColor = FieldTheme.raised,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = FieldTheme.textPrimary,
                    unfocusedTextColor = FieldTheme.textPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .border(1.dp, if (error == null) FieldTheme.strong else FieldTheme.alert, fieldShape),
            )
        }
        if (error != null) {
            Text(error, style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

@Composable
fun ReturnDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var field by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (value != field.text) {
            field = TextFieldValue(value, TextRange(value.length))
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            TextField(
                value = field,
                onValueChange = { incoming ->
                    val formatted = applyReturnDateKeystroke(field.text, incoming.text)
                    field = TextFieldValue(formatted, TextRange(formatted.length))
                    onValueChange(formatted)
                },
                singleLine = true,
                textStyle = TypeScale.numeric.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left,
                ),
                placeholder = {
                    Text("30/12/2026", style = TypeScale.numeric, color = FieldTheme.textMuted)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldTheme.raised,
                    unfocusedContainerColor = FieldTheme.raised,
                    disabledContainerColor = FieldTheme.raised,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = FieldTheme.textPrimary,
                    unfocusedTextColor = FieldTheme.textPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .border(1.dp, FieldTheme.strong, fieldShape),
            )
        }
    }
}

@Composable
fun FormArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Int = 120,
    error: String? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TypeScale.body,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldTheme.raised,
                unfocusedContainerColor = FieldTheme.raised,
                disabledContainerColor = FieldTheme.raised,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = FieldTheme.textPrimary,
                unfocusedTextColor = FieldTheme.textPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp)
                .border(1.dp, if (error == null) FieldTheme.strong else FieldTheme.alert, fieldShape),
        )
        if (error != null) {
            Text(error, style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

@Composable
fun LedgerRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = value.ifEmpty { "—" },
                style = TypeScale.body,
                color = FieldTheme.textPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FieldTheme.hairline),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = FieldTheme.textMuted,
            modifier = Modifier.size(28.dp),
        )
        Text(title, style = TypeScale.body, color = FieldTheme.textPrimary, textAlign = TextAlign.Center)
        if (caption != null) {
            Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted, textAlign = TextAlign.Center)
        }
        if (actionTitle != null && onAction != null) {
            GhostButton(title = actionTitle, onClick = onAction, modifier = Modifier.width(240.dp))
        }
    }
}

@Composable
fun ToastBanner(text: String, tone: StampTone) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, cardShape)
            .clip(cardShape)
            .background(FieldTheme.raised)
            .border(1.dp, FieldTheme.hairline, cardShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(tone.ink)
                .align(Alignment.TopCenter),
        )
        Text(
            text = text,
            style = TypeScale.body,
            color = FieldTheme.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun FieldCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FieldTheme.raised, cardShape)
            .border(1.dp, FieldTheme.hairline, cardShape)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun CarLogo(slug: String?) {
    val context = LocalContext.current
    val bitmap = remember(slug) {
        val trimmed = slug?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            null
        } else {
            runCatching {
                context.assets.open("car-logos/$trimmed.png").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
