package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yahpz.domain.CommitTreatedPlateResult
import com.yahpz.domain.EVENT_MEDIA_DOCS_TAB_LABEL
import com.yahpz.domain.EVENT_MEDIA_TAB_LABEL
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillBackAction
import com.yahpz.domain.FillMode
import com.yahpz.domain.SHIFT_BORN_CHIP
import com.yahpz.domain.SHIFT_BORN_COMPLETE_FORM_ERROR
import com.yahpz.domain.SHIFT_BORN_FILL_TITLE
import com.yahpz.domain.STALE_SAVE_MESSAGE
import com.yahpz.domain.ShiftBornFillDraft
import com.yahpz.domain.ShiftBornFillErrors
import com.yahpz.domain.StampDescriptor
import com.yahpz.domain.StampTone
import com.yahpz.domain.applyTreatedPlateLookup
import com.yahpz.domain.bumpShiftBornTreated
import com.yahpz.domain.commitTreatedPlate
import com.yahpz.domain.decideFillBack
import com.yahpz.domain.digitsOnly
import com.yahpz.domain.formatDate
import com.yahpz.domain.israelToday
import com.yahpz.domain.lastSavedByLabel
import com.yahpz.domain.leftoverEventMediaError
import com.yahpz.domain.lookupPlate
import com.yahpz.domain.plateDigits
import com.yahpz.domain.policeEventIdForInput
import com.yahpz.domain.removeTreatedPlate
import com.yahpz.domain.setTreatedPlateLeftWhere
import com.yahpz.domain.shiftBornCompleteErrors
import com.yahpz.domain.shiftBornFillReadOnly
import com.yahpz.domain.shiftBornReadOnlyNotice
import com.yahpz.domain.treatmentDetailCounter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShiftBornPane { DOCS, MEDIA }

private enum class ShiftBornLoad { LOADING, READY, NOT_FOUND, FAILED }

/**
 * Typed-but-unsaved shared reports, per event, for this app session. Leaving and coming
 * back keeps the text; a successful save drops it. Web keeps the same in localStorage.
 */
private val unsavedShiftBornDrafts = mutableMapOf<String, ShiftBornFillDraft>()

/**
 * The crew's shared report for a shift-born event. Port of web `ShiftBornFillPage`:
 * no plate, odometer, route, end time or KM, ever.
 */
@Composable
fun ShiftBornFillScreen(eventId: String, app: AppModel) {
    val ui by app.state.collectAsState()
    val scope = rememberCoroutineScope()
    var load by remember(eventId) { mutableStateOf(ShiftBornLoad.LOADING) }
    var context by remember(eventId) { mutableStateOf<ShiftBornFillContext?>(null) }
    var draft by remember(eventId) { mutableStateOf(ShiftBornFillDraft()) }
    var expectedAt by remember(eventId) { mutableStateOf<String?>(null) }
    var errors by remember(eventId) { mutableStateOf(ShiftBornFillErrors()) }
    var formError by remember(eventId) { mutableStateOf<String?>(null) }
    var conflict by remember(eventId) { mutableStateOf(false) }
    var restoredFromDevice by remember(eventId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }
    var platePending by remember(eventId) { mutableStateOf("") }
    var plateError by remember(eventId) { mutableStateOf<String?>(null) }
    var plateScanOpen by remember { mutableStateOf(false) }
    var plateLookupGeneration by remember { mutableIntStateOf(0) }
    var pane by remember(eventId) { mutableStateOf(ShiftBornPane.DOCS) }
    var unfinishedMediaDrafts by remember(eventId) { mutableIntStateOf(0) }
    var dropUnfinishedTick by remember(eventId) { mutableIntStateOf(0) }
    var mediaError by remember(eventId) { mutableStateOf<String?>(null) }

    suspend fun fetch() {
        load = ShiftBornLoad.LOADING
        val next = try {
            YahpazAPI.fetchShiftBornFillContext(eventId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            load = ShiftBornLoad.FAILED
            return
        }
        if (next == null) {
            load = ShiftBornLoad.NOT_FOUND
            return
        }
        context = next
        val unsaved = unsavedShiftBornDrafts[eventId]
        if (unsaved != null && unsaved != next.draft) {
            draft = unsaved
            restoredFromDevice = true
        } else {
            draft = next.draft
        }
        expectedAt = next.expectedUpdatedAt
        load = ShiftBornLoad.READY
    }

    LaunchedEffect(eventId) { fetch() }

    val ctx = context
    val today = israelToday()
    val canManage = ui.canManageUnit
    val eventDone = ctx?.status == EventStatus.DONE
    val readOnly = ctx == null || shiftBornFillReadOnly(eventDone, canManage, ctx.shiftDate, today)
    val readOnlyNotice = ctx?.let { shiftBornReadOnlyNotice(eventDone, canManage, it.shiftDate, today) }
    val assigned = ctx != null && ui.userId != null && ui.userId in ctx.responderIds

    LaunchedEffect(draft, ctx?.draft) {
        val loaded = ctx ?: return@LaunchedEffect
        if (readOnly) return@LaunchedEffect
        if (draft == loaded.draft) unsavedShiftBornDrafts.remove(eventId) else unsavedShiftBornDrafts[eventId] = draft
    }

    fun edit(next: ShiftBornFillDraft, clear: (ShiftBornFillErrors) -> ShiftBornFillErrors = { it }) {
        draft = next
        errors = clear(errors)
        formError = null
    }

    fun commitPendingPlate(pendingOverride: String? = null) {
        when (val result = commitTreatedPlate(pendingOverride ?: platePending, draft.treatedPlates)) {
            is CommitTreatedPlateResult.Error -> plateError = result.message
            is CommitTreatedPlateResult.Ok -> {
                edit(draft.copy(treatedPlates = result.plates))
                platePending = ""
                plateError = null
                plateLookupGeneration += 1
                val generation = plateLookupGeneration
                val plateNumber = result.plate.plateNumber
                scope.launch {
                    val hit = withContext(Dispatchers.IO) { lookupPlate(plateNumber) } ?: return@launch
                    if (generation > plateLookupGeneration) return@launch
                    draft = draft.copy(
                        treatedPlates = applyTreatedPlateLookup(draft.treatedPlates, plateDigits(plateNumber), hit),
                    )
                }
            }
        }
    }

    suspend fun persist(complete: Boolean): Boolean {
        val token = expectedAt ?: return false
        when (val result = YahpazAPI.saveShiftBornFill(eventId, token, draft, complete)) {
            is ShiftBornSave.Ok -> {
                expectedAt = result.updatedAt
                context = context?.copy(draft = draft, expectedUpdatedAt = result.updatedAt)
                conflict = false
                unsavedShiftBornDrafts.remove(eventId)
                app.refreshAfterShiftBornSave()
                return true
            }
            is ShiftBornSave.Failed -> {
                formError = result.message
                app.showToast(result.message, StampTone.PENDING)
                if (result.message == STALE_SAVE_MESSAGE) {
                    // Keep what the user typed; take the new token so a deliberate re-save wins.
                    val next = runCatching { YahpazAPI.fetchShiftBornFillContext(eventId) }.getOrNull()
                    if (next != null) {
                        context = next
                        expectedAt = next.expectedUpdatedAt
                        conflict = true
                    }
                }
                return false
            }
        }
    }

    fun onSave() {
        scope.launch {
            saving = true
            val ok = persist(complete = false)
            saving = false
            if (ok) app.showToast("הטיוטה נשמרה")
        }
    }

    fun onComplete() {
        val loaded = context ?: return
        val fieldErrors = shiftBornCompleteErrors(draft, loaded.isCancelled)
        if (!fieldErrors.isEmpty) {
            errors = fieldErrors
            app.showToast(SHIFT_BORN_COMPLETE_FORM_ERROR, StampTone.PENDING)
            return
        }
        errors = ShiftBornFillErrors()
        leftoverEventMediaError(unfinishedMediaDrafts, FillMode.COMPLETE)?.let { leftover ->
            mediaError = leftover
            pane = ShiftBornPane.MEDIA
            app.showToast(leftover, StampTone.PENDING)
            return
        }
        scope.launch {
            completing = true
            val ok = persist(complete = true)
            completing = false
            if (ok) {
                app.showToast("הדיווח הושלם")
                app.closeFill()
            }
        }
    }

    fun onBack() {
        when (decideFillBack(pane == ShiftBornPane.MEDIA, unfinishedMediaDrafts)) {
            FillBackAction.DROP_UNFINISHED_PHOTO -> dropUnfinishedTick += 1
            FillBackAction.SHOW_DOCS -> pane = ShiftBornPane.DOCS
            FillBackAction.LEAVE -> app.closeFill()
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onBack() }, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("חזרה", color = FieldTheme.accent, style = TypeScale.body)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(SHIFT_BORN_FILL_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
                lastSavedByLabel(ctx?.lastSavedName)?.let {
                    Text(it, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            StampChip(StampDescriptor(SHIFT_BORN_CHIP, StampTone.DRAFT))
        }
        when (load) {
            ShiftBornLoad.LOADING -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent)
                Spacer(Modifier.height(12.dp))
                Text("טוען את הדיווח…", style = TypeScale.body, color = FieldTheme.textSecondary)
            }
            ShiftBornLoad.NOT_FOUND -> EmptyState(
                title = FILL_NOT_FOUND_TITLE,
                actionTitle = "חזרה",
                onAction = { app.closeFill() },
            )
            ShiftBornLoad.FAILED -> EmptyState(
                title = "טעינת הדיווח נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { fetch() } },
            )
            ShiftBornLoad.READY -> {
                val loaded = ctx ?: return@Column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FillPaneTab(
                        label = EVENT_MEDIA_DOCS_TAB_LABEL,
                        selected = pane == ShiftBornPane.DOCS,
                        modifier = Modifier.weight(1f),
                        onClick = { pane = ShiftBornPane.DOCS },
                    )
                    FillPaneTab(
                        label = EVENT_MEDIA_TAB_LABEL,
                        selected = pane == ShiftBornPane.MEDIA,
                        modifier = Modifier.weight(1f),
                        onClick = { pane = ShiftBornPane.MEDIA },
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (pane == ShiftBornPane.DOCS) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (conflict) {
                                ShiftBornBanner(
                                    "מישהו שמר את האירוע בזמן שמילאתם. הפרטים שהזנתם נשמרו כאן ולא נמחקו. " +
                                        "בדקו אותם ולחצו שוב על שמירה כדי לשמור את הגרסה שלכם.",
                                    background = FieldTheme.alertTint,
                                )
                            }
                            readOnlyNotice?.let { ShiftBornBanner(it) }
                            if (restoredFromDevice && !readOnly) {
                                ShiftBornBanner("שוחזרו פרטים שהוזנו במכשיר ולא נשמרו. בדקו אותם ולחצו על שמירה.")
                            }
                            FieldCard {
                                LedgerRow("תאריך", formatDate(loaded.eventDate))
                                LedgerRow("סוג אירוע", loaded.eventTypeName.orEmpty())
                            }
                            Text("פרטי הטיפול", style = TypeScale.section, color = FieldTheme.textPrimary)
                            FormField(
                                label = "מספר אירוע",
                                value = draft.policeEventId,
                                onValueChange = { edit(draft.copy(policeEventId = policeEventIdForInput(it))) },
                                keyboardType = KeyboardType.Number,
                                mono = true,
                                enabled = !readOnly,
                            )
                            if (readOnly) {
                                FormField(
                                    label = "כביש",
                                    value = loaded.roads.firstOrNull { it.id == draft.roadId }?.name.orEmpty(),
                                    onValueChange = {},
                                    enabled = false,
                                )
                            } else {
                                LookupPickerField(
                                    label = "כביש",
                                    options = loaded.roads,
                                    selectedId = draft.roadId,
                                    onSelect = { id -> edit(draft.copy(roadId = id)) { it.copy(roadId = null) } },
                                    placeholder = "בחירת כביש",
                                    searchPlaceholder = "חיפוש כביש",
                                    error = errors.roadId,
                                )
                            }
                            FormField(
                                label = "מיקום",
                                value = draft.location,
                                onValueChange = { value -> edit(draft.copy(location = value)) { it.copy(location = null) } },
                                error = errors.location,
                                enabled = !readOnly,
                                placeholder = "למשל: מחלף שורק, לכיוון צפון",
                            )
                            val counter = treatmentDetailCounter(draft.treatmentDetail)
                            FormArea(
                                label = "פירוט הטיפול",
                                value = draft.treatmentDetail,
                                onValueChange = { value ->
                                    edit(draft.copy(treatmentDetail = value)) { it.copy(treatmentDetail = null) }
                                },
                                error = errors.treatmentDetail,
                                enabled = !readOnly,
                                counter = counter.text.takeIf { !readOnly && !loaded.isCancelled },
                                counterMet = counter.met,
                            )
                            TreatedPlatesSection(
                                plates = draft.treatedPlates,
                                pending = platePending,
                                error = plateError,
                                readOnly = readOnly,
                                onPendingChange = {
                                    platePending = digitsOnly(it)
                                    plateError = null
                                },
                                onCommit = { commitPendingPlate() },
                                onScanClick = { plateScanOpen = true },
                                onRemove = { key ->
                                    edit(draft.copy(treatedPlates = removeTreatedPlate(draft.treatedPlates, plateDigitsKey = key)))
                                },
                                onLeftWhereChange = { key, value ->
                                    edit(
                                        draft.copy(
                                            treatedPlates = setTreatedPlateLeftWhere(
                                                draft.treatedPlates,
                                                plateDigitsKey = key,
                                                leftWhere = value,
                                            ),
                                        ),
                                    )
                                },
                            )
                            Text("רכבים שטופלו", style = TypeScale.label, color = FieldTheme.textSecondary)
                            loaded.vehicleKinds.forEach { kind ->
                                val quantity = draft.treated.firstOrNull { it.vehicleKindId == kind.id }?.quantity ?: 0
                                if (readOnly) {
                                    if (quantity > 0) LedgerRow(kind.name, quantity.toString())
                                } else {
                                    TreatedVehicleStepper(
                                        label = kind.name,
                                        value = quantity,
                                        onDelta = { delta ->
                                            edit(draft.copy(treated = bumpShiftBornTreated(draft.treated, kind.id, delta)))
                                        },
                                    )
                                }
                            }
                            FormArea(
                                label = "הערות",
                                value = draft.treatmentNotes,
                                onValueChange = { edit(draft.copy(treatmentNotes = it)) },
                                minHeight = 80,
                                enabled = !readOnly,
                            )
                            formError?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                    FillMediaTab(
                        eventId = loaded.eventId,
                        viewerId = ui.userId,
                        canWrite = assigned && !loaded.isCancelled,
                        leftoverError = mediaError,
                        modifier = if (pane == ShiftBornPane.MEDIA) Modifier.fillMaxSize() else Modifier.size(0.dp),
                        dropUnfinishedTick = dropUnfinishedTick,
                        onUnfinishedChange = {
                            unfinishedMediaDrafts = it
                            if (it == 0) mediaError = null
                        },
                        onToast = { text, tone -> app.showToast(text, tone) },
                    )
                }
                if (!readOnly && pane == ShiftBornPane.DOCS) {
                    Column(
                        modifier = Modifier
                            .background(FieldTheme.raised)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrimaryButton(title = "סיום דיווח", busy = completing, onClick = { onComplete() })
                        GhostButton(
                            title = "שמירת טיוטה",
                            enabled = !saving && !completing,
                            onClick = { onSave() },
                        )
                    }
                }
            }
        }
    }

    if (plateScanOpen) {
        ExperimentalPlateScanDialog(
            onDismiss = { plateScanOpen = false },
            onPlateScanned = { digits ->
                plateScanOpen = false
                commitPendingPlate(pendingOverride = digits)
            },
        )
    }
}

@Composable
private fun ShiftBornBanner(text: String, background: Color = FieldTheme.accentSubtle) {
    Text(
        text,
        style = TypeScale.body,
        color = FieldTheme.textPrimary,
        modifier = Modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(12.dp)
            .fillMaxWidth(),
    )
}
