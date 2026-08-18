package com.yahpz.responder

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                    Text(
                        "לחיצה מתקשרת · לחיצה ארוכה שולחת וואטסאפ",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                    filtered.forEach { contact ->
                        ContactRow(
                            contact = contact,
                            onCall = { openContactLink(context, telHref(contact.phone)) { app.showToast(it, StampTone.PENDING) } },
                            onWhatsApp = { openContactLink(context, whatsAppHref(contact.phone)) { app.showToast(it, StampTone.PENDING) } },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(contact: UnitContact, onCall: () -> Unit, onWhatsApp: () -> Unit) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .combinedClickable(onClick = onCall, onLongClick = onWhatsApp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    contact.fullName.ifEmpty { "כונן" },
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                Text(
                    listOf(contact.callsign, contact.email).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = TypeScale.caption,
                    color = FieldTheme.textMuted,
                )
            }
            Text(
                text = contact.phone?.let { formatPhone(it) }?.ifEmpty { "—" } ?: "—",
                style = TypeScale.numeric,
                color = if (contact.phone == null) FieldTheme.textMuted else FieldTheme.accent,
            )
        }
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    FormField(
        label = "חיפוש",
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
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
