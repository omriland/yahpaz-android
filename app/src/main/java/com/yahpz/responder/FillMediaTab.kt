package com.yahpz.responder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yahpz.domain.EVENT_MEDIA_ADDED
import com.yahpz.domain.EVENT_MEDIA_ASSIGN
import com.yahpz.domain.EVENT_MEDIA_ASSIGN_HINT
import com.yahpz.domain.EVENT_MEDIA_ASSIGN_SAVED
import com.yahpz.domain.EVENT_MEDIA_AUTO_LINKED
import com.yahpz.domain.EVENT_MEDIA_AUTO_LINK_UNDONE
import com.yahpz.domain.EVENT_MEDIA_PLATES_TITLE
import com.yahpz.domain.EVENT_MEDIA_BAD_TYPE
import com.yahpz.domain.EVENT_MEDIA_CAMERA_FAILED
import com.yahpz.domain.EVENT_MEDIA_VIDEO_ADDED
import com.yahpz.domain.EVENT_MEDIA_VIDEO_DELETED
import com.yahpz.domain.EventMediaKind
import com.yahpz.domain.eventMediaKind
import com.yahpz.domain.isVideoMedia
import com.yahpz.domain.rejectVideoFile
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
import com.yahpz.domain.autoLinkPlateIds
import com.yahpz.domain.canAddMoreMedia
import com.yahpz.domain.canDeleteEventMedia
import com.yahpz.domain.captionError
import com.yahpz.domain.eventMediaTakenWhenLabel
import com.yahpz.domain.formatDateTime
import com.yahpz.domain.mediaPlateLabel
import com.yahpz.domain.groupMediaByTakenWhen
import com.yahpz.domain.slotsRemaining
import com.yahpz.domain.togglePlateId
import com.yahpz.domain.treatedPlateCaption
import com.yahpz.domain.uniquePlateIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val fieldShape = RoundedCornerShape(4.dp)
private val cardShape = RoundedCornerShape(8.dp)

private data class MediaDraft(
    val key: String,
    val uri: Uri,
    val kind: EventMediaKind = EventMediaKind.IMAGE,
    /** The picked video's type; photos are always uploaded as JPEG. */
    val videoMimeType: String? = null,
    val takenWhen: EventMediaTakenWhen? = null,
    val treatedPlateIds: List<String> = emptyList(),
    val caption: String = "",
    val uploading: Boolean = false,
    val error: String? = null,
)

/** An inline line offering to undo a plate the scan linked to a just-uploaded photo. */
private data class AutoLink(val mediaId: String, val plateId: String)

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
    // Delete chosen from the tile menu: ביטול goes back to the grid, not into the viewer.
    var deleteFromTile by remember { mutableStateOf(false) }
    var editTakenWhen by remember { mutableStateOf(EventMediaTakenWhen.BEFORE_TREATMENT) }
    var editPlateIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var editCaption by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    var savingEdit by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var assignTarget by remember { mutableStateOf<EventMedia?>(null) }
    var assignPlateIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var assignError by remember { mutableStateOf<String?>(null) }
    var assigning by remember { mutableStateOf(false) }
    var autoLinks by remember(eventId) { mutableStateOf<List<AutoLink>>(emptyList()) }

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

    fun addPicked(uris: List<Uri>) {
        val picked = uris.map { uri -> uri to context.contentResolver.getType(uri) }
        val supported = picked.filter { (_, type) -> eventMediaKind(type) != null }
        if (supported.size < picked.size) onToast(EVENT_MEDIA_BAD_TYPE, StampTone.PENDING)
        drafts = drafts + supported.take(remaining).map { (uri, type) ->
            val kind = eventMediaKind(type) ?: EventMediaKind.IMAGE
            MediaDraft(
                key = UUID.randomUUID().toString(),
                uri = uri,
                kind = kind,
                videoMimeType = type.takeIf { kind == EventMediaKind.VIDEO },
            )
        }
        scope.launch {
            plates = withContext(Dispatchers.IO) { YahpazAPI.listEventMediaPlates(eventId) }
        }
    }

    // One add button opens the system chooser: the camera next to the gallery and files apps.
    // A photo taken there lands in a cache file that FileProvider shares with the camera app.
    var pendingCamera by remember { mutableStateOf<Pair<File, Uri>?>(null) }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val camera = pendingCamera
        pendingCamera = null
        val data = result.data
        val picked = buildList {
            data?.clipData?.let { clip -> for (i in 0 until clip.itemCount) add(clip.getItemAt(i).uri) }
            if (isEmpty()) data?.data?.let(::add)
        }
        val photo = camera?.takeIf { (file, _) -> file.length() > 0 }
        if (photo == null) camera?.first?.delete()
        if (result.resultCode != Activity.RESULT_OK || !addEnabled) return@rememberLauncherForActivityResult
        when {
            picked.isNotEmpty() -> addPicked(picked)
            photo != null -> addPicked(listOf(photo.second))
        }
    }
    fun openChooser(withCamera: Boolean) {
        val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/mp4", "video/quicktime", "video/webm"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val capture = if (withCamera) {
            val dir = File(context.cacheDir, "event_photos").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCamera = file to uri
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            null
        }
        val intent = Intent.createChooser(pick, "הוספת תמונות וסרטונים").apply {
            capture?.let { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it)) }
        }
        runCatching { chooser.launch(intent) }.onFailure {
            pendingCamera = null
            onToast(EVENT_MEDIA_CAMERA_FAILED, StampTone.PENDING)
        }
    }
    // CAMERA is declared for the plate scanner, so the camera app may only be offered once it is granted.
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        openChooser(withCamera = granted)
    }

    fun patchDraft(key: String, patch: MediaDraft.() -> MediaDraft) {
        drafts = drafts.map { if (it.key == key) it.patch() else it }
    }

    // After a photo uploads: read its plate, link the saved vehicles it matches. Every failure is silent.
    suspend fun autoLinkScannedPlates(mediaId: String) {
        // Nothing on the event to match: skip the scan (it is a paid model call).
        if (plates.isEmpty()) return
        val scanned = withContext(Dispatchers.IO) { YahpazAPI.scanPlatesInMedia(mediaId) }
        val current = items.firstOrNull { it.id == mediaId } ?: return
        val added = autoLinkPlateIds(scanned, plates, current.treatedPlateIds)
        if (added.isEmpty()) return
        val all = uniquePlateIds(current.treatedPlateIds + added)
        val result = withContext(Dispatchers.IO) { YahpazAPI.replaceMediaPlates(mediaId, all) }
        if (result != EventMediaWriteResult.Done) return
        items = items.map { if (it.id == mediaId) it.copy(treatedPlateIds = all) else it }
        autoLinks = autoLinks + added.map { AutoLink(mediaId, it) }
    }

    fun undoAutoLink(link: AutoLink) {
        val media = items.firstOrNull { it.id == link.mediaId } ?: return
        val rest = media.treatedPlateIds - link.plateId
        scope.launch {
            val result = withContext(Dispatchers.IO) { YahpazAPI.replaceMediaPlates(media.id, rest) }
            when (result) {
                EventMediaWriteResult.Done -> {
                    items = items.map { if (it.id == media.id) it.copy(treatedPlateIds = rest) else it }
                    autoLinks = autoLinks - link
                    onToast(EVENT_MEDIA_AUTO_LINK_UNDONE, StampTone.DONE)
                }
                is EventMediaWriteResult.Error -> onToast(result.message, StampTone.PENDING)
                is EventMediaWriteResult.Uploaded -> Unit
            }
        }
    }

    fun startUpload(key: String, takenWhen: EventMediaTakenWhen) {
        val draft = drafts.firstOrNull { it.key == key } ?: return
        patchDraft(key) { copy(uploading = true, error = null, takenWhen = takenWhen) }
        scope.launch {
            val payload = withContext(Dispatchers.IO) { readUploadPayload(context, draft) }
            if (payload is UploadPayload.Rejected) {
                patchDraft(key) { copy(uploading = false, error = payload.message) }
                return@launch
            }
            val ready = payload as UploadPayload.Ready
            val latest = drafts.firstOrNull { it.key == key } ?: draft
            val result = withContext(Dispatchers.IO) {
                YahpazAPI.uploadEventMedia(
                    eventId = eventId,
                    bytes = ready.bytes,
                    mimeType = ready.mimeType,
                    width = ready.width,
                    height = ready.height,
                    takenWhen = takenWhen,
                    treatedPlateIds = latest.treatedPlateIds,
                    caption = latest.caption.trim().ifEmpty { null },
                )
            }
            when (result) {
                is EventMediaWriteResult.Uploaded -> {
                    drafts = drafts.filterNot { it.key == key }
                    items = items + result.media
                    val added = if (draft.kind == EventMediaKind.VIDEO) EVENT_MEDIA_VIDEO_ADDED else EVENT_MEDIA_ADDED
                    onToast(added, StampTone.DONE)
                    if (draft.kind == EventMediaKind.IMAGE) {
                        scope.launch { autoLinkScannedPlates(result.media.id) }
                    }
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

    fun openViewer(item: EventMedia) {
        viewer = item
        editing = false
        confirmDelete = false
        deleteFromTile = false
        editTakenWhen = item.takenWhen
        editPlateIds = item.treatedPlateIds
        editCaption = item.caption.orEmpty()
        editError = null
    }

    fun openAssign(item: EventMedia) {
        assignTarget = item
        assignPlateIds = item.treatedPlateIds
        assignError = null
    }

    fun removeFromTile(item: EventMedia) {
        openViewer(item)
        deleteFromTile = true
        confirmDelete = true
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
                hasMenu = { canDeleteEventMedia(it, viewerId, canWrite) },
                canAssign = plates.isNotEmpty(),
                onOpen = ::openViewer,
                onAssign = ::openAssign,
                onRemove = ::removeFromTile,
            )
            MediaBand(
                heading = eventMediaTakenWhenLabel(EventMediaTakenWhen.DURING_AFTER_TREATMENT),
                items = grouped.during,
                hasMenu = { canDeleteEventMedia(it, viewerId, canWrite) },
                canAssign = plates.isNotEmpty(),
                onOpen = ::openViewer,
                onAssign = ::openAssign,
                onRemove = ::removeFromTile,
            )
            autoLinks.forEach { link ->
                val plate = plates.firstOrNull { it.id == link.plateId }
                if (plate != null && items.any { it.id == link.mediaId }) {
                    AutoLinkLine(plate = plate, onUndo = { undoAutoLink(link) })
                }
            }
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
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) openChooser(withCamera = true) else cameraPermission.launch(Manifest.permission.CAMERA)
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
                        "הוספת תמונות וסרטונים",
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

    assignTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!assigning) assignTarget = null },
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
                Text(EVENT_MEDIA_ASSIGN, style = TypeScale.section, color = FieldTheme.textPrimary)
                Text(EVENT_MEDIA_ASSIGN_HINT, style = TypeScale.body, color = FieldTheme.textSecondary)
                MediaPlateChips(
                    plates = plates,
                    selected = assignPlateIds,
                    enabled = !assigning,
                    onChange = { assignPlateIds = it },
                )
                assignError?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
                PrimaryButton(title = "שמירה", busy = assigning, onClick = {
                    scope.launch {
                        assigning = true
                        val result = withContext(Dispatchers.IO) {
                            YahpazAPI.replaceMediaPlates(target.id, assignPlateIds)
                        }
                        assigning = false
                        when (result) {
                            EventMediaWriteResult.Done -> {
                                val linkedIds = uniquePlateIds(assignPlateIds)
                                items = items.map { if (it.id == target.id) it.copy(treatedPlateIds = linkedIds) else it }
                                autoLinks = autoLinks.filterNot { it.mediaId == target.id }
                                assignTarget = null
                                onToast(EVENT_MEDIA_ASSIGN_SAVED, StampTone.DONE)
                            }
                            is EventMediaWriteResult.Error -> assignError = result.message
                            is EventMediaWriteResult.Uploaded -> Unit
                        }
                    }
                })
                GhostButton(title = "ביטול", enabled = !assigning, onClick = { assignTarget = null })
            }
        }
    }

    viewer?.let { current ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val own = canDeleteEventMedia(current, viewerId, canWrite)
        val linked = plates.filter { it.id in current.treatedPlateIds }
        val currentIsVideo = isVideoMedia(current.mimeType)
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
                    when {
                        !confirmDelete -> eventMediaTakenWhenLabel(current.takenWhen)
                        currentIsVideo -> "למחוק את הסרטון?"
                        else -> "למחוק את התמונה?"
                    },
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
                                    onToast(
                                        if (currentIsVideo) EVENT_MEDIA_VIDEO_DELETED else EVENT_MEDIA_DELETED,
                                        StampTone.DONE,
                                    )
                                }
                                is EventMediaWriteResult.Error -> onToast(result.message, StampTone.PENDING)
                                is EventMediaWriteResult.Uploaded -> Unit
                            }
                        }
                    })
                    GhostButton(title = "ביטול", enabled = !deleting, onClick = {
                        confirmDelete = false
                        if (deleteFromTile) viewer = null
                    })
                } else {
                    val videoUrl = current.signedUrl.takeIf { currentIsVideo }
                    if (videoUrl != null) {
                        PlayVideoButton(url = videoUrl, mimeType = current.mimeType)
                    } else if (current.signedUrl != null) {
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
                        MediaPlateChips(
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
                                        autoLinks = autoLinks.filterNot { it.mediaId == next.id }
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
        if (immersive && signedUrl != null && !currentIsVideo) {
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
    hasMenu: (EventMedia) -> Boolean,
    canAssign: Boolean,
    onOpen: (EventMedia) -> Unit,
    onAssign: (EventMedia) -> Unit,
    onRemove: (EventMedia) -> Unit,
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
                    val isVideo = isVideoMedia(item.mimeType)
                    val menu = hasMenu(item)
                    var menuOpen by remember(item.id) { mutableStateOf(false) }
                    // Your own media while editing: a tap offers צפייה / שיוך לרכב / הסרה; otherwise it opens the viewer.
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        val tile = Modifier
                            .fillMaxSize()
                            .clip(fieldShape)
                            .border(1.dp, FieldTheme.hairline, fieldShape)
                            .clickable(
                                onClickLabel = if (!menu) null else if (isVideo) "פעולות לסרטון" else "פעולות לתמונה",
                            ) { if (menu) menuOpen = true else onOpen(item) }
                        if (isVideo) {
                            VideoTile(contentDescription = "סרטון: ${item.caption ?: heading}", modifier = tile)
                        } else {
                            AsyncImage(
                                model = item.signedUrl,
                                contentDescription = item.caption ?: heading,
                                contentScale = ContentScale.Crop,
                                modifier = tile,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = FieldTheme.raised,
                        ) {
                            DropdownMenuItem(
                                text = { Text("צפייה", style = TypeScale.body, color = FieldTheme.textPrimary) },
                                onClick = {
                                    menuOpen = false
                                    onOpen(item)
                                },
                            )
                            if (canAssign) {
                                DropdownMenuItem(
                                    text = { Text(EVENT_MEDIA_ASSIGN, style = TypeScale.body, color = FieldTheme.textPrimary) },
                                    onClick = {
                                        menuOpen = false
                                        onAssign(item)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("הסרה", style = TypeScale.body, color = FieldTheme.alert) },
                                onClick = {
                                    menuOpen = false
                                    onRemove(item)
                                },
                            )
                        }
                    }
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
            val preview = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(fieldShape)
            if (draft.kind == EventMediaKind.VIDEO) {
                VideoTile(contentDescription = "סרטון", modifier = preview)
            } else {
                AsyncImage(
                    model = draft.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = preview,
                )
            }
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
        MediaPlateChips(
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

/** One toggle chip per saved treated vehicle: logo + plate, accent border, tint and a check when linked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaPlateChips(
    plates: List<EventMediaPlateOption>,
    selected: List<String>,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    if (plates.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(EVENT_MEDIA_PLATES_TITLE, style = TypeScale.label, color = FieldTheme.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            plates.forEach { plate ->
                val checked = plate.id in selected
                val label = mediaPlateLabel(plate)
                val state = if (checked) "נבחר" else "לא נבחר"
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(fieldShape)
                        .background(if (checked) FieldTheme.accentSubtle else FieldTheme.raised)
                        .border(
                            if (checked) 2.dp else 1.dp,
                            if (checked) FieldTheme.accent else FieldTheme.strong,
                            fieldShape,
                        )
                        .toggleable(
                            value = checked,
                            enabled = enabled,
                            role = Role.Checkbox,
                            onValueChange = { onChange(togglePlateId(selected, plate.id)) },
                        )
                        .clearAndSetSemantics {
                            contentDescription = label
                            stateDescription = state
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CarLogo(slug = plate.logoSlug)
                    LicensePlate(plate = plate.plateNumber)
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        if (checked) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = FieldTheme.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shown in the media section after the scan linked a vehicle; ביטול unlinks just that plate. */
@Composable
private fun AutoLinkLine(plate: EventMediaPlateOption, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.doneTint, fieldShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            EVENT_MEDIA_AUTO_LINKED,
            style = TypeScale.body,
            color = FieldTheme.textPrimary,
            modifier = Modifier.weight(1f, fill = false),
        )
        LicensePlate(plate = plate.plateNumber)
        Spacer(Modifier.weight(1f))
        GhostButton(title = "ביטול", onClick = onUndo, modifier = Modifier.width(88.dp))
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

private sealed class UploadPayload {
    class Ready(val bytes: ByteArray, val mimeType: String, val width: Int?, val height: Int?) : UploadPayload()
    class Rejected(val message: String) : UploadPayload()
}

/** Photos are compressed to JPEG; videos are read as picked after the 25 MB check. */
private fun readUploadPayload(context: Context, draft: MediaDraft): UploadPayload {
    if (draft.kind == EventMediaKind.VIDEO) {
        val mimeType = draft.videoMimeType ?: return UploadPayload.Rejected(EVENT_MEDIA_BAD_TYPE)
        val size = runCatching {
            context.contentResolver.openAssetFileDescriptor(draft.uri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        // A provider that cannot report a size is checked after reading, below.
        if (size > 0) rejectVideoFile(size)?.let { return UploadPayload.Rejected(it) }
        val bytes = runCatching {
            context.contentResolver.openInputStream(draft.uri)?.use { it.readBytes() }
        }.getOrNull() ?: return UploadPayload.Rejected(EVENT_MEDIA_NETWORK)
        rejectVideoFile(bytes.size.toLong())?.let { return UploadPayload.Rejected(it) }
        return UploadPayload.Ready(bytes, mimeType, width = null, height = null)
    }
    return when (val compressed = compressEventImage(context, draft.uri)) {
        is CompressEventImageResult.Error -> UploadPayload.Rejected(compressed.message)
        is CompressEventImageResult.Ok -> UploadPayload.Ready(
            compressed.image.bytes,
            "image/jpeg",
            compressed.image.width,
            compressed.image.height,
        )
    }
}

/** Videos have no stored poster frame, so the tile is a dark frame with a play badge. */
@Composable
private fun VideoTile(contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF1F2328))
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FieldTheme.raised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = FieldTheme.textPrimary)
        }
    }
}

/** Plays the video in the phone's own player; the signed link lasts an hour. */
@Composable
private fun PlayVideoButton(url: String, mimeType: String) {
    val context = LocalContext.current
    VideoTile(
        contentDescription = "הפעלת הסרטון",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(cardShape)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), mimeType)
                runCatching { context.startActivity(intent) }
            },
    )
    Text("הקישו כדי להפעיל את הסרטון", style = TypeScale.caption, color = FieldTheme.textMuted)
}
