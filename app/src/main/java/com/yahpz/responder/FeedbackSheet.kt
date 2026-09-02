package com.yahpz.responder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yahpz.domain.FEEDBACK_ATTACH_ADD
import com.yahpz.domain.FEEDBACK_ATTACH_COUNT_ERROR
import com.yahpz.domain.FEEDBACK_ATTACH_HINT
import com.yahpz.domain.FEEDBACK_ATTACH_MAX
import com.yahpz.domain.FEEDBACK_ATTACH_TYPE_ERROR
import com.yahpz.domain.FEEDBACK_BODY_MAX
import com.yahpz.domain.FEEDBACK_HIDE_UNTIL_REFRESH
import com.yahpz.domain.FEEDBACK_KIND_BUG
import com.yahpz.domain.FEEDBACK_KIND_SUGGESTION
import com.yahpz.domain.FEEDBACK_LABEL
import com.yahpz.domain.FEEDBACK_MIC_ERROR
import com.yahpz.domain.FEEDBACK_RECORD_MAX_SECONDS
import com.yahpz.domain.FeedbackPickedMeta
import com.yahpz.domain.addFeedbackAttachments
import com.yahpz.domain.feedbackAttachmentError
import com.yahpz.domain.feedbackAttachmentKind
import com.yahpz.domain.feedbackSubmitError
import com.yahpz.domain.formatRecordSeconds
import com.yahpz.domain.shouldAutoStopRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FeedbackMiniFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = FieldTheme.accent,
        contentColor = FieldTheme.textOnAccent,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = FEEDBACK_LABEL)
    }
}

fun shouldShowFeedbackFab(
    hiddenUntilRefresh: Boolean,
    overlay: ToolsDestination,
    fillOpen: Boolean,
): Boolean {
    if (hiddenUntilRefresh || fillOpen) return false
    return overlay != ToolsDestination.NEW_EVENT &&
        overlay != ToolsDestination.EDIT_EVENT &&
        overlay != ToolsDestination.NEW_SHIFT &&
        overlay != ToolsDestination.EDIT_SHIFT
}

fun feedbackPagePathForUi(ui: AppUiState): String {
    val tabName = when (ui.tab) {
        AppTab.INBOX -> "inbox"
        AppTab.SHIFTS -> "my_shifts"
        AppTab.CONTACTS -> "contacts"
        AppTab.UNIT_EVENTS -> "events"
        AppTab.UNIT_SHIFTS -> "shifts"
        AppTab.TOOLS -> "users"
        AppTab.REPORTS -> "reports"
        AppTab.PROFILE -> "profile"
    }
    val overlay = if (ui.toolsDestination == ToolsDestination.HUB) {
        "HUB"
    } else {
        ui.toolsDestination.name
    }
    return com.yahpz.domain.feedbackPagePath(ui.fillEventId, tabName, overlay)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    pagePath: String,
    onDismiss: () -> Unit,
    onHideUntilRefresh: () -> Unit,
    onSubmit: suspend (
        kind: String,
        body: String,
        audioBytes: ByteArray?,
        mime: String?,
        attachments: List<FeedbackAttachmentUpload>,
    ) -> String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf<String?>(null) }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var files by remember { mutableStateOf<List<FeedbackPickedUi>>(emptyList()) }

    fun releaseRecorder() {
        recorder?.runCatching {
            stop()
            release()
        }
        recorder = null
        recording = false
    }

    fun stopRecording() {
        val rec = recorder ?: return
        try {
            rec.stop()
        } catch (_: Exception) {
        }
        rec.release()
        recorder = null
        recording = false
        val file = audioFile
        if (file == null || !file.exists() || file.length() == 0L) {
            audioFile = null
            file?.delete()
        }
    }

    fun startRecording(): String? {
        val file = File(context.cacheDir, "yahpaz-feedback-${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(96_000)
            rec.setAudioSamplingRate(44_100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            audioFile?.delete()
            audioFile = file
            recorder = rec
            elapsed = 0
            recording = true
            null
        } catch (_: Exception) {
            rec.release()
            file.delete()
            FEEDBACK_MIC_ERROR
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = FEEDBACK_ATTACH_MAX),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val next = files.toMutableList()
        var nextError: String? = null
        for (uri in uris) {
            if (next.size >= FEEDBACK_ATTACH_MAX) {
                nextError = FEEDBACK_ATTACH_COUNT_ERROR
                break
            }
            val picked = readFeedbackPicked(context, uri)
            val fileError = if (picked.size <= 0) {
                if (feedbackAttachmentKind(picked.mime, picked.name) == null) {
                    FEEDBACK_ATTACH_TYPE_ERROR
                } else {
                    null
                }
            } else {
                feedbackAttachmentError(FeedbackPickedMeta(picked.name, picked.mime, picked.size))
            }
            if (fileError != null) {
                nextError = fileError
                continue
            }
            next += picked
        }
        files = next
        error = nextError
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            error = startRecording()
        } else {
            error = FEEDBACK_MIC_ERROR
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseRecorder()
        }
    }

    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        val started = System.currentTimeMillis()
        while (true) {
            val seconds = ((System.currentTimeMillis() - started) / 1000).toInt()
            elapsed = seconds
            if (shouldAutoStopRecording(seconds)) {
                stopRecording()
                break
            }
            delay(250)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) {
                releaseRecorder()
                onDismiss()
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(FEEDBACK_LABEL, style = TypeScale.title, color = FieldTheme.textPrimary)
            Text("סוג", style = TypeScale.label, color = FieldTheme.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KindChip(FEEDBACK_KIND_BUG, kind == "bug") { kind = "bug"; error = null }
                KindChip(FEEDBACK_KIND_SUGGESTION, kind == "suggestion") { kind = "suggestion"; error = null }
            }
            Text("הערה", style = TypeScale.label, color = FieldTheme.textMuted)
            Text(
                "אפשר לכתוב, להקליט, לצרף קבצים, או לשלב.",
                style = TypeScale.caption,
                color = FieldTheme.textMuted,
            )
            TextField(
                value = body,
                onValueChange = {
                    body = it.take(FEEDBACK_BODY_MAX)
                    error = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("למשל: אחרי שמירה המסך נשאר ריק") },
                minLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldTheme.sunken,
                    unfocusedContainerColor = FieldTheme.sunken,
                    focusedIndicatorColor = FieldTheme.accent,
                    unfocusedIndicatorColor = FieldTheme.hairline,
                ),
            )
            if (recording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { stopRecording() },
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null, tint = FieldTheme.accent)
                        Text("עצירת הקלטה", color = FieldTheme.accent)
                    }
                    Text(
                        "${formatRecordSeconds(elapsed)} / ${formatRecordSeconds(FEEDBACK_RECORD_MAX_SECONDS)}",
                        style = TypeScale.numeric,
                        color = FieldTheme.textSecondary,
                    )
                }
            } else if (audioFile != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("הקלטה מוכנה", style = TypeScale.body, color = FieldTheme.textPrimary)
                    TextButton(
                        onClick = {
                            audioFile?.delete()
                            audioFile = null
                            elapsed = 0
                        },
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "מחיקת הקלטה", tint = FieldTheme.alert)
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            error = startRecording()
                        } else {
                            permission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = FieldTheme.accent)
                    Text("הקלטת הודעה", color = FieldTheme.accent)
                }
            }
            Text("קבצים", style = TypeScale.label, color = FieldTheme.textMuted)
            Text(FEEDBACK_ATTACH_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
            files.forEachIndexed { index, file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        file.name,
                        style = TypeScale.body,
                        color = FieldTheme.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            files = files.filterIndexed { itemIndex, _ -> itemIndex != index }
                            error = null
                        },
                        enabled = !busy && !recording,
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "הסרת קובץ", tint = FieldTheme.alert)
                    }
                }
            }
            if (files.size < FEEDBACK_ATTACH_MAX) {
                TextButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                        )
                    },
                    enabled = !busy && !recording,
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = FieldTheme.accent)
                    Text(FEEDBACK_ATTACH_ADD, color = FieldTheme.accent)
                }
            }
            error?.let {
                Text(it, style = TypeScale.caption, color = FieldTheme.alert)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                title = if (busy) "שולח…" else "שליחה",
                busy = busy,
                enabled = !busy && !recording,
                onClick = {
                    val next = feedbackSubmitError(kind, body, audioFile != null)
                    if (next != null || kind == null) {
                        error = next
                        return@PrimaryButton
                    }
                    scope.launch {
                        busy = true
                        val bytes = audioFile?.takeIf { it.exists() }?.readBytes()
                        val uploads = withContext(Dispatchers.IO) {
                            files.mapNotNull { picked ->
                                val fileBytes = readFeedbackBytes(context, picked.uri) ?: return@mapNotNull null
                                FeedbackAttachmentUpload(picked.name, picked.mime, fileBytes)
                            }
                        }
                        val check = addFeedbackAttachments(
                            emptyList(),
                            uploads.map { FeedbackPickedMeta(it.name, it.mime, it.bytes.size) },
                        )
                        if (check.error != null) {
                            busy = false
                            error = check.error
                            return@launch
                        }
                        val fail = onSubmit(
                            kind!!,
                            body,
                            bytes,
                            if (bytes != null) "audio/mp4" else null,
                            uploads,
                        )
                        busy = false
                        if (fail != null) {
                            error = fail
                        } else {
                            audioFile?.delete()
                            onDismiss()
                        }
                    }
                },
            )
            TextButton(
                onClick = {
                    if (busy || recording) return@TextButton
                    releaseRecorder()
                    audioFile?.delete()
                    onHideUntilRefresh()
                    onDismiss()
                },
                enabled = !busy && !recording,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
            ) {
                Text(FEEDBACK_HIDE_UNTIL_REFRESH, color = FieldTheme.textSecondary)
            }
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .border(
                width = 1.dp,
                color = if (selected) FieldTheme.accent else FieldTheme.hairline,
                shape = RoundedCornerShape(999.dp),
            ),
    ) {
        Text(
            label,
            color = if (selected) FieldTheme.accent else FieldTheme.textSecondary,
            style = TypeScale.bodyStrong,
        )
    }
}

private data class FeedbackPickedUi(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Int,
)

private fun readFeedbackPicked(context: Context, uri: Uri): FeedbackPickedUi {
    val resolver = context.contentResolver
    var name = uri.lastPathSegment ?: "קובץ"
    var size = 0
    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                size = cursor.getLong(sizeIdx).toInt().coerceAtLeast(0)
            }
        }
    }
    return FeedbackPickedUi(
        uri = uri,
        name = name,
        mime = resolver.getType(uri).orEmpty(),
        size = size,
    )
}

private fun readFeedbackBytes(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
