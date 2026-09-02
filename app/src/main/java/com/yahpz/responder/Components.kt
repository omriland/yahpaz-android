package com.yahpz.responder

import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.yahpz.domain.EventFreezeFlags
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.applyReturnDateKeystroke
import com.yahpz.domain.applyTimeKeystroke

private val fieldShape = RoundedCornerShape(4.dp)
private val cardShape = RoundedCornerShape(8.dp)

/** Shared control height so paired form fields line up. */
val FormControlHeight = 56.dp

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
fun FrozenEventMark(flags: EventFreezeFlags, modifier: Modifier = Modifier) {
    val tip = flags.tooltipHe ?: return
    Icon(
        imageVector = Icons.Outlined.AcUnit,
        contentDescription = tip,
        tint = FieldTheme.pending,
        modifier = modifier.size(20.dp),
    )
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}

@Composable
fun PrimaryCreateFab(label: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = FieldTheme.accent,
        contentColor = FieldTheme.textOnAccent,
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Text(label, style = TypeScale.bodyStrong)
    }
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
    danger: Boolean = false,
) {
    val color = when {
        !enabled -> FieldTheme.textMuted
        danger -> FieldTheme.alert
        else -> FieldTheme.accent
    }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(1.dp, if (danger) FieldTheme.alert else FieldTheme.strong, fieldShape),
    ) {
        Text(
            title,
            style = TypeScale.bodyStrong,
            color = color,
        )
    }
}

@Composable
fun PrivacyPolicyLink(onOpen: () -> Unit, command: Boolean = false) {
    TextButton(onClick = onOpen) {
        Text(
            "מדיניות פרטיות",
            style = TypeScale.caption,
            color = if (command) CommandTheme.textSecondary else FieldTheme.textMuted,
        )
    }
}

@Composable
fun FormFieldRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
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
    textAlignEnd: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun dismissKeyboard() {
        focusManager.clearFocus()
        keyboard?.hide()
        onSubmit?.invoke()
    }
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
                    when {
                        ltr -> style.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left)
                        textAlignEnd -> style.copy(textAlign = TextAlign.End)
                        else -> style
                    }
                },
                placeholder = placeholder?.let { hint ->
                    {
                        Text(
                            hint,
                            style = (if (mono) TypeScale.numeric else TypeScale.body).let { style ->
                                if (textAlignEnd) style.copy(textAlign = TextAlign.End) else style
                            },
                            color = FieldTheme.textMuted,
                            modifier = if (textAlignEnd) Modifier.fillMaxWidth() else Modifier,
                            textAlign = if (textAlignEnd) TextAlign.End else TextAlign.Unspecified,
                        )
                    }
                },
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onDone = { dismissKeyboard() },
                    onGo = { dismissKeyboard() },
                    onSearch = { dismissKeyboard() },
                    onSend = { dismissKeyboard() },
                    onNext = { dismissKeyboard() },
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
                    disabledTextColor = FieldTheme.textMuted,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FormControlHeight)
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
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
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
                    .height(FormControlHeight)
                    .border(1.dp, FieldTheme.strong, fieldShape),
            )
        }
    }
}

@Composable
fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "08:00",
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
                    val formatted = applyTimeKeystroke(field.text, incoming.text)
                    field = TextFieldValue(formatted, TextRange(formatted.length))
                    onValueChange(formatted)
                },
                singleLine = true,
                textStyle = TypeScale.numeric.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left,
                ),
                placeholder = {
                    Text(placeholder, style = TypeScale.numeric, color = FieldTheme.textMuted)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
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
                    .height(FormControlHeight)
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
            minLines = 3,
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
    val reduceMotion = rememberReducedMotion()
    var visible by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) { visible = true }
    val enter = if (reduceMotion) {
        fadeIn(tween(150))
    } else {
        fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 }
    }
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = fadeOut(tween(150)),
    ) {
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
