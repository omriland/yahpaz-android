package com.yahpz.responder

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yahpz.domain.EVENT_MEDIA_ADDED
import com.yahpz.domain.EVENT_MEDIA_CAP
import com.yahpz.domain.EVENT_MEDIA_CAP_ERROR
import com.yahpz.domain.EVENT_MEDIA_DELETED
import com.yahpz.domain.EVENT_MEDIA_EMPTY
import com.yahpz.domain.EVENT_MEDIA_NETWORK
import com.yahpz.domain.EVENT_MEDIA_TITLE
import com.yahpz.domain.EVENT_MEDIA_UPDATED
import com.yahpz.domain.EventMedia
import com.yahpz.domain.EventMediaPlateOption
import com.yahpz.domain.EventMediaTakenWhen
import com.yahpz.domain.StampTone
import com.yahpz.domain.canAddMoreMedia
import com.yahpz.domain.captionError
import com.yahpz.domain.eventMediaTakenWhenLabel
import com.yahpz.domain.formatDateTime
import com.yahpz.domain.groupMediaByTakenWhen
import com.yahpz.domain.slotsRemaining
import com.yahpz.domain.togglePlateId
import com.yahpz.domain.treatedPlateCaption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val fieldShape = RoundedCornerShape(4.dp)
private val cardShape = RoundedCornerShape(8.dp)

private data class MediaDraft(
    val key: String,
    val uri: Uri,
    val takenWhen: EventMediaTakenWhen? = null,
    val treatedPlateIds: List<String> = emptyList(),
    val caption: String = "",
    val uploading: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillMediaTab(
    eventId: String,
    viewerId: String?,
    canWrite: Boolean,
    leftoverError: String?,
    modifier: Modifier = Modifier,
    dropUnfinishedTick: Int = 0,
    onUnfinishedChange: (Int) -> Unit,
    onToast: (String, StampTone) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember(eventId) { mutableStateOf<List<EventMedia>>(emptyList()) }
    var plates by remember(eventId) { mutableStateOf<List<EventMediaPlateOption>>(emptyList()) }
    var drafts by remember(eventId) { mutableStateOf<List<MediaDraft>>(emptyList()) }
    var loading by remember(eventId) { mutableStateOf(true) }
    var viewer by remember { mutableStateOf<EventMedia?>(null) }
    var immersive by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editTakenWhen by remember { mutableStateOf(EventMediaTakenWhen.BEFORE_TREATMENT) }
    var editPlateIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var editCaption by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    var savingEdit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val inFlight = drafts.count { it.uploading }
    val addEnabled = canWrite && canAddMoreMedia(items.size, inFlight)
    val remaining = slotsRemaining(items.size, inFlight)

    LaunchedEffect(eventId) {
        loading = true
        items = withContext(Dispatchers.IO) { YahpazAPI.listEventMedia(eventId) }
        plates = withContext(Dispatchers.IO) { YahpazAPI.listEventMediaPlates(eventId) }
        loading = false
    }
    LaunchedEffect(drafts) {
        onUnfinishedChange(drafts.count { it.takenWhen == null })
    }
    LaunchedEffect(dropUnfinishedTick) {
        if (dropUnfinishedTick <= 0) return@LaunchedEffect
        val last = drafts.lastOrNull { it.takenWhen == null && !it.uploading } ?: return@LaunchedEffect
        drafts = drafts.filterNot { it.key == last.key }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = EVENT_MEDIA_CAP),
    ) { uris ->
        if (uris.isEmpty() || !addEnabled) return@rememberLauncherForActivityResult
        drafts = drafts + uris.take(remaining).map { uri ->
            MediaDraft(key = UUID.randomUUID().toString(), uri = uri)
        }
        scope.launch {
            plates = withContext(Dispatchers.IO) { YahpazAPI.listEventMediaPlates(eventId) }
        }
    }

    fun patchDraft(key: String, patch: MediaDraft.() -> MediaDraft) {
        drafts = drafts.map { if (it.key == key) it.patch() else it }
    }

    fun startUpload(key: String, takenWhen: EventMediaTakenWhen) {
        val draft = drafts.firstOrNull { it.key == key } ?: return
        patchDraft(key) { copy(uploading = true, error = null, takenWhen = takenWhen) }
        scope.launch {
            val compressed = withContext(Dispatchers.IO) { compressEventImage(context, draft.uri) }
            if (compressed is CompressEventImageResult.Error) {
                patchDraft(key) { copy(uploading = false, error = compressed.message) }
                return@launch
            }
            val image = (compressed as CompressEventImageResult.Ok).image
            val latest = drafts.firstOrNull { it.key == key } ?: draft
            val result = withContext(Dispatchers.IO) {
                YahpazAPI.uploadEventMedia(
                    eventId = eventId,
                    jpegBytes = image.bytes,
                    width = image.width,
                    height = image.height,
                    takenWhen = takenWhen,
                    treatedPlateIds = latest.treatedPlateIds,
                    caption = latest.caption.trim().ifEmpty { null },
                )
            }
            when (result) {
                is EventMediaWriteResult.Uploaded -> {
                    drafts = drafts.filterNot { it.key == key }
                    items = items + result.media
                    onToast(EVENT_MEDIA_ADDED, StampTone.DONE)
                }
                is EventMediaWriteResult.Error -> {
                    patchDraft(key) { copy(uploading = false, error = result.message) }
                }
                EventMediaWriteResult.Done -> {
                    patchDraft(key) { copy(uploading = false, error = EVENT_MEDIA_NETWORK) }
                }
            }
        }
    }

    val grouped = groupMediaByTakenWhen(items)
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(EVENT_MEDIA_TITLE, style = TypeScale.label, color = FieldTheme.textSecondary)
                if (canWrite) {
                    Text(
                        "${items.size}/$EVENT_MEDIA_CAP",
                        style = TypeScale.caption,
                        color = FieldTheme.textMuted,
                    )
                }
            }
            leftoverError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = FieldTheme.accent, modifier = Modifier.size(28.dp))
                }
            }
            if (!loading && items.isEmpty() && drafts.isEmpty() && !canWrite) {
                Text(EVENT_MEDIA_EMPTY, style = TypeScale.body, color = FieldTheme.textMuted)
            }
            MediaBand(
                heading = eventMediaTakenWhenLabel(EventMediaTakenWhen.BEFORE_TREATMENT),
                items = grouped.before,
                onOpen = { item ->
                    viewer = item
                    editing = false
                    confirmDelete = false
                    editTakenWhen = item.takenWhen
                    editPlateIds = item.treatedPlateIds
                    editCaption = item.caption.orEmpty()
                    editError = null
                },
            )
            MediaBand(
                heading = eventMediaTakenWhenLabel(EventMediaTakenWhen.DURING_AFTER_TREATMENT),
                items = grouped.during,
                onOpen = { item ->
                    viewer = item
                    editing = false
                    confirmDelete = false
                    editTakenWhen = item.takenWhen
                    editPlateIds = item.treatedPlateIds
                    editCaption = item.caption.orEmpty()
                    editError = null
                },
            )
            drafts.forEach { draft ->
                MediaDraftCard(
                    draft = draft,
                    plates = plates,
                    onCaption = { patchDraft(draft.key) { copy(caption = it) } },
                    onPlates = { patchDraft(draft.key) { copy(treatedPlateIds = it) } },
                    onTakenWhen = { startUpload(draft.key, it) },
                    onRetry = { draft.takenWhen?.let { startUpload(draft.key, it) } },
                    onRemove = { drafts = drafts.filterNot { it.key == draft.key } },
                )
            }
        }
        if (canWrite) {
            Column(
                modifier = Modifier
                    .background(FieldTheme.raised)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = addEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .border(1.dp, FieldTheme.strong, fieldShape),
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (addEnabled) FieldTheme.accent else FieldTheme.textMuted,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        "הוספת תמונות",
                        style = TypeScale.bodyStrong,
                        color = if (addEnabled) FieldTheme.accent else FieldTheme.textMuted,
                    )
                }
                if (!addEnabled) {
                    Text(EVENT_MEDIA_CAP_ERROR, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
        }
    }

    viewer?.let { current ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val own = canWrite && viewerId != null && current.uploadedBy == viewerId
        val linked = plates.filter { it.id in current.treatedPlateIds }
        ModalBottomSheet(
            onDismissRequest = {
                if (!deleting && !savingEdit && !immersive) {
                    viewer = null
                    editing = false
                    confirmDelete = false
                    immersive = false
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (confirmDelete) "למחוק את התמונה?" else eventMediaTakenWhenLabel(current.takenWhen),
                    style = TypeScale.section,
                    color = FieldTheme.textPrimary,
                )
                if (confirmDelete) {
                    Text("לא ניתן לשחזר.", style = TypeScale.body, color = FieldTheme.textPrimary)
                    PrimaryButton(title = "מחיקה", busy = deleting, onClick = {
                        scope.launch {
                            deleting = true
                            val result = withContext(Dispatchers.IO) {
                                YahpazAPI.deleteEventMedia(current.id, current.storagePath)
                            }
                            deleting = false
                            when (result) {
                                EventMediaWriteResult.Done -> {
                                    items = items.filterNot { it.id == current.id }
                                    immersive = false
                                    viewer = null
                                    confirmDelete = false
                                    onToast(EVENT_MEDIA_DELETED, StampTone.DONE)
                                }
                                is EventMediaWriteResult.Error -> onToast(result.message, StampTone.PENDING)
                                is EventMediaWriteResult.Uploaded -> Unit
                            }
                        }
                    })
                    GhostButton(title = "ביטול", enabled = !deleting, onClick = { confirmDelete = false })
                } else {
                    if (current.signedUrl != null) {
                        AsyncImage(
                            model = current.signedUrl,
                            contentDescription = current.caption,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(cardShape)
                                .background(FieldTheme.sunken)
                                .clickable { immersive = true },
                        )
                    } else {
                        Text(EVENT_MEDIA_NETWORK, style = TypeScale.body, color = FieldTheme.textMuted)
                    }
                    if (editing) {
                        TakenWhenPicker(value = editTakenWhen, enabled = !savingEdit, onChange = { editTakenWhen = it })
                        MediaPlateChecklist(
                            plates = plates,
                            selected = editPlateIds,
                            enabled = !savingEdit,
                            onChange = { editPlateIds = it },
                        )
                        FormField(
                            label = "תיאור",
                            value = editCaption,
                            onValueChange = {
                                editCaption = it
                                editError = null
                            },
                            error = editError,
                            enabled = !savingEdit,
                            placeholder = "למשל: פגיעה בגלגל קדמי",
                        )
                        PrimaryButton(title = "שמירה", busy = savingEdit, onClick = {
                            val captionIssue = captionError(editCaption)
                            if (captionIssue != null) {
                                editError = captionIssue
                                return@PrimaryButton
                            }
                            scope.launch {
                                savingEdit = true
                                val result = withContext(Dispatchers.IO) {
                                    YahpazAPI.updateEventMedia(
                                        id = current.id,
                                        takenWhen = editTakenWhen,
                                        treatedPlateIds = editPlateIds,
                                        caption = editCaption.trim().ifEmpty { null },
                                    )
                                }
                                savingEdit = false
                                when (result) {
                                    EventMediaWriteResult.Done -> {
                                        val next = current.copy(
                                            takenWhen = editTakenWhen,
                                            treatedPlateIds = editPlateIds,
                                            caption = editCaption.trim().ifEmpty { null },
                                        )
                                        items = items.map { if (it.id == next.id) next else it }
                                        viewer = next
                                        editing = false
                                        onToast(EVENT_MEDIA_UPDATED, StampTone.DONE)
                                    }
                                    is EventMediaWriteResult.Error -> {
                                        editError = result.message
                                        onToast(result.message, StampTone.PENDING)
                                    }
                                    is EventMediaWriteResult.Uploaded -> Unit
                                }
                            }
                        })
                        GhostButton(title = "ביטול", enabled = !savingEdit, onClick = { editing = false })
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("תיאור", style = TypeScale.label, color = FieldTheme.textSecondary)
                            Text(
                                current.caption?.trim()?.ifEmpty { null } ?: "—",
                                style = TypeScale.body,
                                color = FieldTheme.textPrimary,
                            )
                            Text("רכבים בתמונה", style = TypeScale.label, color = FieldTheme.textSecondary)
                            if (linked.isEmpty()) {
                                Text("—", style = TypeScale.body, color = FieldTheme.textPrimary)
                            } else {
                                linked.forEach { plate -> MediaPlateRow(plate) }
                            }
                            Text(
                                listOfNotNull(current.uploaderName, formatDateTime(current.createdAt))
                                    .joinToString(" · "),
                                style = TypeScale.caption,
                                color = FieldTheme.textMuted,
                            )
                        }
                        if (own) {
                            GhostButton(title = "עריכה", onClick = { editing = true })
                            GhostButton(title = "מחיקה", danger = true, onClick = { confirmDelete = true })
                        }
                    }
                }
            }
        }
        val signedUrl = current.signedUrl
        if (immersive && signedUrl != null) {
            ImmersiveImageViewer(
                model = signedUrl,
                contentDescription = current.caption,
                onDismiss = { immersive = false },
            )
        }
    }
}

@Composable
private fun MediaBand(
    heading: String,
    items: List<EventMedia>,
    onOpen: (EventMedia) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(heading, style = TypeScale.label, color = FieldTheme.textSecondary)
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { item ->
                    AsyncImage(
                        model = item.signedUrl,
                        contentDescription = item.caption ?: heading,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(fieldShape)
                            .border(1.dp, FieldTheme.hairline, fieldShape)
                            .clickable { onOpen(item) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MediaDraftCard(
    draft: MediaDraft,
    plates: List<EventMediaPlateOption>,
    onCaption: (String) -> Unit,
    onPlates: (List<String>) -> Unit,
    onTakenWhen: (EventMediaTakenWhen) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.raised, cardShape)
            .border(1.dp, FieldTheme.hairline, cardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            AsyncImage(
                model = draft.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(fieldShape),
            )
            if (draft.uploading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            IconButton(
                onClick = onRemove,
                enabled = !draft.uploading,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "הסרה", tint = Color.White)
            }
        }
        MediaPlateChecklist(
            plates = plates,
            selected = draft.treatedPlateIds,
            enabled = !draft.uploading,
            onChange = onPlates,
        )
        FormField(
            label = "תיאור",
            value = draft.caption,
            onValueChange = onCaption,
            enabled = !draft.uploading,
            placeholder = "למשל: פגיעה בגלגל קדמי",
        )
        TakenWhenPicker(
            value = draft.takenWhen,
            enabled = !draft.uploading,
            required = true,
            onChange = onTakenWhen,
        )
        draft.error?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
        if (draft.uploading) {
            Text("מעלה…", style = TypeScale.caption, color = FieldTheme.textMuted)
        }
        if (draft.error != null) {
            GhostButton(
                title = "נסו שוב",
                enabled = draft.takenWhen != null && !draft.uploading,
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun TakenWhenPicker(
    value: EventMediaTakenWhen?,
    enabled: Boolean,
    onChange: (EventMediaTakenWhen) -> Unit,
    required: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("מתי צולמה", style = TypeScale.label, color = FieldTheme.textSecondary)
        EventMediaTakenWhen.entries.forEach { option ->
            val selected = value == option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .border(
                        1.dp,
                        when {
                            selected -> FieldTheme.accent
                            required && value == null -> FieldTheme.alert
                            else -> FieldTheme.strong
                        },
                        fieldShape,
                    )
                    .clickable(enabled = enabled) { onChange(option) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    eventMediaTakenWhenLabel(option),
                    style = if (selected) TypeScale.bodyStrong else TypeScale.body,
                    color = if (selected) FieldTheme.accent else FieldTheme.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun MediaPlateChecklist(
    plates: List<EventMediaPlateOption>,
    selected: List<String>,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    if (plates.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("רכבים בתמונה", style = TypeScale.label, color = FieldTheme.textSecondary)
        plates.forEach { plate ->
            val checked = plate.id in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(enabled = enabled) { onChange(togglePlateId(selected, plate.id)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onChange(togglePlateId(selected, plate.id)) },
                    enabled = enabled,
                    colors = CheckboxDefaults.colors(checkedColor = FieldTheme.accent),
                )
                MediaPlateRow(plate, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MediaPlateRow(plate: EventMediaPlateOption, modifier: Modifier = Modifier) {
    val caption = treatedPlateCaption(model = plate.model, color = plate.color)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CarLogo(slug = plate.logoSlug)
        LicensePlate(plate = plate.plateNumber)
        if (caption != null) {
            Text(
                caption,
                style = TypeScale.caption,
                color = FieldTheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun ImmersiveImageViewer(
    model: Any,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val next = (scale * zoomChange).coerceIn(1f, 8f)
            scale = next
            offset = if (next <= 1.01f) Offset.Zero else offset + panChange
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .transformable(transformState),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(4.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "סגירה",
                    tint = Color.White,
                )
            }
        }
    }
}
