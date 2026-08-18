package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yahpz.domain.BROADCAST_CAPTION
import com.yahpz.domain.BROADCAST_LOAD_FAILED
import com.yahpz.domain.BROADCAST_LOADING_RECIPIENTS
import com.yahpz.domain.BROADCAST_LOG_EMPTY
import com.yahpz.domain.BROADCAST_TITLE
import com.yahpz.domain.BroadcastAudience
import com.yahpz.domain.BroadcastChannel
import com.yahpz.domain.BroadcastDraft
import com.yahpz.domain.BroadcastDraftErrors
import com.yahpz.domain.BroadcastLogEntry
import com.yahpz.domain.broadcastAudienceLabel
import com.yahpz.domain.broadcastChannelLabel
import com.yahpz.domain.broadcastPreviewCaption
import com.yahpz.domain.formatDateTime
import com.yahpz.domain.needsBroadcastSubject
import com.yahpz.domain.previewUnitBroadcast
import com.yahpz.domain.validateBroadcastDraft
import kotlinx.coroutines.launch

/**
 * תפוצה: pick a channel and an audience, see how many are reachable, send through the
 * `unit-broadcast` edge function. Push is added server-side for anyone with the app.
 */
@Composable
fun BroadcastScreen(app: AppModel, ui: AppUiState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var channel by remember { mutableStateOf(BroadcastChannel.BOTH) }
    var audience by remember { mutableStateOf(BroadcastAudience.ALL) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(BroadcastDraftErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(ui.userId) {
        if (ui.userId != null && ui.broadcastCandidates.isEmpty()) app.reloadBroadcast()
    }

    val preview = remember(ui.broadcastCandidates, channel, audience) {
        previewUnitBroadcast(ui.broadcastCandidates, channel, audience)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolsBackRow(BROADCAST_TITLE, onBack)
        Text(BROADCAST_CAPTION, style = TypeScale.caption, color = FieldTheme.textMuted)
        when {
            ui.broadcastFailed && ui.broadcastCandidates.isEmpty() -> EmptyState(
                title = BROADCAST_LOAD_FAILED,
                actionTitle = "רענון",
                onAction = { scope.launch { app.reloadBroadcast() } },
            )
            ui.broadcastCandidates.isEmpty() -> LoadingBlock(BROADCAST_LOADING_RECIPIENTS)
            else -> {
                OptionRowSelector(
                    label = "ערוץ",
                    options = BroadcastChannel.entries.map { it.raw to broadcastChannelLabel(it) },
                    selected = channel.raw,
                    onSelect = { channel = BroadcastChannel.fromRaw(it) },
                )
                OptionRowSelector(
                    label = "קהל",
                    options = BroadcastAudience.entries.map { it.raw to broadcastAudienceLabel(it) },
                    selected = audience.raw,
                    onSelect = { audience = BroadcastAudience.fromRaw(it) },
                )
                if (needsBroadcastSubject(channel)) {
                    FormField(
                        label = "נושא",
                        value = subject,
                        onValueChange = { subject = it },
                        placeholder = "נושא ההודעה",
                        error = errors.subject,
                    )
                }
                FormArea(
                    label = "תוכן ההודעה",
                    value = body,
                    onValueChange = { body = it },
                    minHeight = 140,
                    error = errors.body,
                )
                Text(
                    broadcastPreviewCaption(preview, channel, audience),
                    style = TypeScale.body,
                    color = FieldTheme.textSecondary,
                )
                formError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(
                    title = "שליחת התפוצה",
                    busy = sending,
                    enabled = preview.canSend && !sending,
                    onClick = {
                        val draft = BroadcastDraft(
                            channel = channel,
                            audience = audience,
                            subject = subject,
                            body = body,
                        )
                        val next = validateBroadcastDraft(draft)
                        errors = next
                        if (!next.isEmpty) {
                            formError = next.firstMessage
                            return@PrimaryButton
                        }
                        formError = null
                        scope.launch {
                            sending = true
                            val error = app.sendBroadcast(draft)
                            sending = false
                            formError = error
                            if (error == null) {
                                subject = ""
                                body = ""
                            }
                        }
                    },
                )
                Text("תפוצות אחרונות", style = TypeScale.section, color = FieldTheme.textPrimary)
                if (ui.broadcastLog.isEmpty()) {
                    Text(BROADCAST_LOG_EMPTY, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
                ui.broadcastLog.forEach { entry -> BroadcastLogCard(entry) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BroadcastLogCard(entry: BroadcastLogEntry) {
    FieldCard(modifier = Modifier.heightIn(min = 44.dp)) {
        Text(
            entry.subject.ifEmpty { entry.body.take(60) },
            style = TypeScale.bodyStrong,
            color = FieldTheme.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(entry.summary, style = TypeScale.caption, color = FieldTheme.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            listOf(formatDateTime(entry.createdAt), entry.senderDisplay)
                .filter { it.isNotEmpty() }
                .joinToString(" · "),
            style = TypeScale.caption,
            color = FieldTheme.textMuted,
        )
    }
}
