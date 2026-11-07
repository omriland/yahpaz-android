package com.yahpz.responder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yahpz.domain.CONTACTS_EMPTY_TITLE
import com.yahpz.domain.CONTACTS_FAILED_TITLE
import com.yahpz.domain.CONTACTS_NO_RESULTS_TITLE
import com.yahpz.domain.CONTACTS_SEARCH_PLACEHOLDER
import com.yahpz.domain.CONTACTS_TITLE
import com.yahpz.domain.StampTone
import com.yahpz.domain.filterContacts
import com.yahpz.domain.formatPhone
import com.yahpz.domain.telHref
import com.yahpz.domain.whatsAppHref
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(app: AppModel, ui: AppUiState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.contacts.isEmpty()) app.reloadContacts()
    }

    val filtered = filterContacts(ui.contacts, query) { it.searchFields }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                app.reloadContacts()
                refreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(CONTACTS_TITLE, style = TypeScale.title, color = FieldTheme.textPrimary)
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = CONTACTS_SEARCH_PLACEHOLDER,
            )
            when {
                ui.contactsFailed -> EmptyState(
                    title = CONTACTS_FAILED_TITLE,
                    actionTitle = "רענון",
                    onAction = { scope.launch { app.reloadContacts() } },
                )
                ui.contactsLoading && ui.contacts.isEmpty() -> LoadingBlock("טוען אנשי קשר…")
                filtered.isEmpty() -> EmptyState(
                    title = if (query.isBlank()) CONTACTS_EMPTY_TITLE else CONTACTS_NO_RESULTS_TITLE,
                    actionTitle = if (query.isBlank()) null else "ניקוי חיפוש",
                    onAction = if (query.isBlank()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filtered.forEach { contact ->
                        ContactRow(
                            contact = contact,
                            onCall = {
                                openContactLink(context, telHref(contact.phone)) {
                                    app.showToast(it, StampTone.PENDING)
                                }
                            },
                            onWhatsApp = {
                                openContactLink(context, whatsAppHref(contact.phone)) {
                                    app.showToast(it, StampTone.PENDING)
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: UnitContact, onCall: () -> Unit, onWhatsApp: () -> Unit) {
    val hasPhone = !contact.phone.isNullOrBlank()
    FieldCard(modifier = Modifier.heightIn(min = 44.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    contact.fullName.ifEmpty { "מתנדב" },
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(
                    listOfNotNull(
                        contact.callsign.takeIf { it.isNotEmpty() },
                        contact.phone?.let { formatPhone(it) }?.takeIf { it.isNotEmpty() },
                    ).joinToString(" · ").ifEmpty { "—" },
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onCall,
                    enabled = hasPhone && telHref(contact.phone) != null,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "התקשרות",
                        tint = if (hasPhone) FieldTheme.accent else FieldTheme.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = onWhatsApp,
                    enabled = hasPhone && whatsAppHref(contact.phone) != null,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = "וואטסאפ",
                        tint = if (hasPhone) FieldTheme.accent else FieldTheme.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** Compact M3 search: icon pill, placeholder inside, clear button, Search key. "חיפוש" is the spoken label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = FieldTheme.raised,
        unfocusedContainerColor = FieldTheme.raised,
        focusedBorderColor = FieldTheme.accent,
        unfocusedBorderColor = FieldTheme.strong,
        focusedLeadingIconColor = FieldTheme.textSecondary,
        unfocusedLeadingIconColor = FieldTheme.textSecondary,
        focusedTrailingIconColor = FieldTheme.textSecondary,
        unfocusedTrailingIconColor = FieldTheme.textSecondary,
    )
    val textStyle = TypeScale.body.copy(fontSize = 16.sp, color = FieldTheme.textPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(FieldTheme.accent),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "חיפוש" },
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = { Text(placeholder, style = textStyle, color = FieldTheme.textMuted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (value.isEmpty()) {
                    null
                } else {
                    {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "ניקוי חיפוש")
                        }
                    }
                },
                colors = colors,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 8.dp, bottom = 8.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape,
                    )
                },
            )
        },
    )
}

@Composable
fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = FieldTheme.accent)
        Spacer(Modifier.height(12.dp))
        Text(text, style = TypeScale.body, color = FieldTheme.textSecondary)
    }
}

private fun openContactLink(context: Context, href: String?, onError: (String) -> Unit) {
    if (href == null) {
        onError("אין מספר טלפון זמין לאיש הקשר הזה.")
        return
    }
    try {
        val action = if (href.startsWith("tel:")) Intent.ACTION_DIAL else Intent.ACTION_VIEW
        context.startActivity(Intent(action, Uri.parse(href)))
    } catch (_: ActivityNotFoundException) {
        onError("לא נמצאה אפליקציה שיכולה לפתוח את הקישור.")
    }
}
