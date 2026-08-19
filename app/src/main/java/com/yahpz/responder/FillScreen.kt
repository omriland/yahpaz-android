package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yahpz.domain.EVENT_MEDIA_DOCS_TAB_LABEL
import com.yahpz.domain.EVENT_MEDIA_TAB_LABEL
import com.yahpz.domain.CommitTreatedPlateResult
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillMode
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.ResponderFillErrors
import com.yahpz.domain.TreatedPlate
import com.yahpz.domain.applyTreatedPlateLookup
import com.yahpz.domain.commitTreatedPlate
import com.yahpz.domain.digitsOnly
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatDateTime
import com.yahpz.domain.lookupPlate
import com.yahpz.domain.participationStamp
import com.yahpz.domain.plateDigits
import com.yahpz.domain.removeTreatedPlate
import com.yahpz.domain.setTreatedPlateLeftWhere
import com.yahpz.domain.treatedPlateCaption
import com.yahpz.domain.validateResponderFillDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class FillPane { DOCS, MEDIA }

@Composable
fun FillScreen(eventId: String, app: AppModel) {
    val ui by app.state.collectAsState()
    val scope = rememberCoroutineScope()
    var context by remember { mutableStateOf<FillContext?>(null) }
    var draft by remember { mutableStateOf(ResponderFillDraft()) }
    var errors by remember { mutableStateOf(ResponderFillErrors()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var savingDraft by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }
    var plateLookupGeneration by remember { mutableIntStateOf(0) }
    var pane by remember { mutableStateOf(FillPane.DOCS) }
    var unfinishedMediaDrafts by remember { mutableIntStateOf(0) }
    var plateScanOpen by remember { mutableStateOf(false) }

    fun commitPendingPlate(pendingOverride: String? = null) {
        when (
            val result = commitTreatedPlate(
                pending = pendingOverride ?: draft.treatedPlatePending,
                plates = draft.treatedPlates,
            )
        ) {
            is CommitTreatedPlateResult.Error -> {
                errors = errors.copy(treatedPlates = result.message)
            }
            is CommitTreatedPlateResult.Ok -> {
                draft = draft.copy(
                    treatedPlates = result.plates,
                    treatedPlatePending = "",
                )
                errors = errors.copy(treatedPlates = null)
                plateLookupGeneration += 1
                val generation = plateLookupGeneration
                val plateNumber = result.plate.plateNumber
                scope.launch {
                    val hit = withContext(Dispatchers.IO) { lookupPlate(plateNumber) } ?: return@launch
                    if (generation > plateLookupGeneration) return@launch
                    val key = plateDigits(plateNumber)
                    draft = draft.copy(
                        treatedPlates = applyTreatedPlateLookup(draft.treatedPlates, key, hit),
                    )
                }
            }
        }
    }

    suspend fun load() {
        loading = true
        failed = false
        try {
            val next = YahpazAPI.fetchFillContext(eventId)
            context = next
            if (next == null) failed = true else draft = next.draft
        } catch (_: Exception) {
            failed = true
        }
        loading = false
    }

    LaunchedEffect(eventId) { load() }

    BackHandler { app.closeFill() }

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
            TextButton(
                onClick = { app.closeFill() },
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text("חזרה", color = FieldTheme.accent, style = TypeScale.body)
            }
            Text("השלמת הפרטים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
        }
        when {
            loading -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = FieldTheme.accent)
                Spacer(Modifier.height(12.dp))
                Text("טוען את הדיווח…", style = TypeScale.body, color = FieldTheme.textSecondary)
            }
            failed || context == null -> EmptyState(
                title = "טעינת הדיווח נכשלה. בדקו את החיבור ונסו שוב.",
                actionTitle = "רענון",
                onAction = { scope.launch { load() } },
            )
            else -> {
                val fill = context!!
                val readOnly = fill.participationStatus == ParticipationStatus.DONE ||
                    fill.eventStatus == EventStatus.DONE || fill.isCancelled
                val mediaWritable = !fill.isCancelled
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FillPaneTab(
                            label = EVENT_MEDIA_DOCS_TAB_LABEL,
                            selected = pane == FillPane.DOCS,
                            modifier = Modifier.weight(1f),
                            onClick = { pane = FillPane.DOCS },
                        )
                        FillPaneTab(
                            label = EVENT_MEDIA_TAB_LABEL,
                            selected = pane == FillPane.MEDIA,
                            modifier = Modifier.weight(1f),
                            onClick = { pane = FillPane.MEDIA },
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        if (pane == FillPane.DOCS) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FieldCard {
                            LedgerRow("תאריך", formatDate(fill.eventDate))
                            LedgerRow("מספר אירוע", fill.policeEventId.orEmpty())
                            LedgerRow("סוג אירוע", fill.eventTypeName.orEmpty())
                            LedgerRow("כביש", fill.roadName.orEmpty())
                            LedgerRow("מיקום", fill.location.orEmpty())
                            LedgerRow("אחמ״ש", fill.shiftLeadName.orEmpty())
                        }
                        if (fill.isCancelled) {
                            Text(
                                "האירוע נסגר. לא ניתן לערוך את הדיווח.",
                                style = TypeScale.body,
                                color = FieldTheme.textPrimary,
                                modifier = Modifier
                                    .background(FieldTheme.accentSubtle, RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                            )
                        }
                        if (readOnly) {
                            StampChip(participationStamp(fill.participationStatus, true))
                            fill.updatedAt?.let {
                                Text(
                                    "הדיווח הושלם ב־${formatDateTime(it)}. רק אחמ״ש יכול לערוך לאחר סיום.",
                                    style = TypeScale.caption,
                                    color = FieldTheme.textMuted,
                                )
                            }
                        }
                        Text("הפרטים שלי", style = TypeScale.section, color = FieldTheme.textPrimary)
                        if (fill.vehicles.isEmpty()) {
                            Text("לא מקושר רכב למשתמש. פנו למנהל המערכת.", style = TypeScale.caption, color = FieldTheme.alert)
                        } else {
                            val selectedLabel = fill.vehicles.firstOrNull { it.plate == draft.vehiclePlate }?.label ?: "בחירת רכב"
                            FormField(
                                label = "לוחית רישוי",
                                value = selectedLabel,
                                onValueChange = {},
                                enabled = false,
                                error = errors.vehiclePlate,
                            )
                        }
                        FormField("מד אוץ התחלה", draft.odometerStart, { draft = draft.copy(odometerStart = it) }, keyboardType = KeyboardType.Number, mono = true, error = errors.odometerStart, enabled = !readOnly)
                        FormField("מד אוץ סיום", draft.odometerEnd, { draft = draft.copy(odometerEnd = it) }, keyboardType = KeyboardType.Number, mono = true, error = errors.odometerEnd, enabled = !readOnly)
                        FormArea("נתיב נסיעה", draft.route, { draft = draft.copy(route = it) }, error = errors.route, enabled = !readOnly)
                        FormArea("פירוט הטיפול", draft.treatmentDetail, { draft = draft.copy(treatmentDetail = it) }, error = errors.treatmentDetail, enabled = !readOnly)
                        TreatedPlatesSection(
                            plates = draft.treatedPlates,
                            pending = draft.treatedPlatePending,
                            error = errors.treatedPlates,
                            readOnly = readOnly,
                            onPendingChange = { draft = draft.copy(treatedPlatePending = digitsOnly(it)) },
                            onCommit = { commitPendingPlate() },
                            onScanClick = { plateScanOpen = true },
                            onRemove = { key ->
                                draft = draft.copy(
                                    treatedPlates = removeTreatedPlate(draft.treatedPlates, plateDigitsKey = key),
                                )
                            },
                            onLeftWhereChange = { key, value ->
                                draft = draft.copy(
                                    treatedPlates = setTreatedPlateLeftWhere(
                                        draft.treatedPlates,
                                        plateDigitsKey = key,
                                        leftWhere = value,
                                    ),
                                )
                            },
                        )
                        FormArea("הערות לטיפול", draft.treatmentNotes, { draft = draft.copy(treatmentNotes = it) }, minHeight = 80, enabled = !readOnly)
                        formError?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
                        Spacer(Modifier.height(80.dp))
                    }
                        }
                        FillMediaTab(
                            eventId = fill.eventId,
                            viewerId = ui.userId,
                            canWrite = mediaWritable,
                            leftoverError = errors.eventMedia,
                            modifier = if (pane == FillPane.MEDIA) Modifier.fillMaxSize() else Modifier.size(0.dp),
                            onUnfinishedChange = { unfinishedMediaDrafts = it },
                            onToast = { text, tone -> app.showToast(text, tone) },
                        )
                    }
                    if (!readOnly && pane == FillPane.DOCS) {
                        Column(
                            modifier = Modifier
                                .background(FieldTheme.raised)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PrimaryButton(title = "סיום דיווח", busy = completing, onClick = {
                                scope.launch {
                                    formError = null
                                    errors = validateResponderFillDraft(
                                        draft,
                                        FillMode.COMPLETE,
                                        fill.vehicles.map { it.plate },
                                        fill.totalKm,
                                        unfinishedMediaDrafts,
                                    )
                                    if (errors.eventMedia != null) pane = FillPane.MEDIA
                                    completing = true
                                    val error = YahpazAPI.saveFill(fill, draft, true, unfinishedMediaDrafts)
                                    completing = false
                                    if (error != null) {
                                        formError = error
                                        if (error == errors.eventMedia) pane = FillPane.MEDIA
                                        app.showToast(error, com.yahpz.domain.StampTone.PENDING)
                                    } else {
                                        app.showToast("הדיווח הושלם")
                                        app.reloadEvents()
                                        app.closeFill()
                                    }
                                }
                            })
                            GhostButton(
                                title = "שמירת טיוטה",
                                enabled = !savingDraft && !completing,
                                onClick = {
                                    scope.launch {
                                        formError = null
                                        errors = validateResponderFillDraft(
                                            draft,
                                            FillMode.DRAFT,
                                            fill.vehicles.map { it.plate },
                                            fill.totalKm,
                                            unfinishedMediaDrafts,
                                        )
                                        savingDraft = true
                                        val error = YahpazAPI.saveFill(fill, draft, false, unfinishedMediaDrafts)
                                        savingDraft = false
                                        if (error != null) {
                                            formError = error
                                            app.showToast(error, com.yahpz.domain.StampTone.PENDING)
                                        } else {
                                            app.showToast("הטיוטה נשמרה")
                                            app.reloadEvents()
                                            app.closeFill()
                                        }
                                    }
                                },
                            )
                        }
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
private fun FillPaneTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (selected) FieldTheme.accentSubtle else FieldTheme.raised, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) FieldTheme.accent else FieldTheme.strong, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = if (selected) TypeScale.bodyStrong else TypeScale.body,
            color = if (selected) FieldTheme.accent else FieldTheme.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TreatedPlatesSection(
    plates: List<TreatedPlate>,
    pending: String,
    error: String?,
    readOnly: Boolean,
    onPendingChange: (String) -> Unit,
    onCommit: () -> Unit,
    onScanClick: () -> Unit,
    onRemove: (String) -> Unit,
    onLeftWhereChange: (String, String) -> Unit,
) {
    if (readOnly && plates.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("מספרי כלי רכב", style = TypeScale.label, color = FieldTheme.textSecondary)
        plates.forEach { row ->
            TreatedPlateCard(
                row = row,
                removable = !readOnly,
                onRemove = onRemove,
                onLeftWhereChange = onLeftWhereChange,
            )
        }
        if (!readOnly) {
            TreatedPlateAddRow(
                pending = pending,
                error = error,
                onPendingChange = onPendingChange,
                onCommit = onCommit,
                onScanClick = onScanClick,
            )
        }
    }
}

@Composable
private fun TreatedPlateAddRow(
    pending: String,
    error: String?,
    onPendingChange: (String) -> Unit,
    onCommit: () -> Unit,
    onScanClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun commitFromKeyboard() {
        focusManager.clearFocus()
        keyboard?.hide()
        onCommit()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TextField(
                    value = pending,
                    onValueChange = onPendingChange,
                    singleLine = true,
                    textStyle = TypeScale.numeric,
                    placeholder = {
                        Text("xx-xxx-xx", style = TypeScale.numeric, color = FieldTheme.textMuted)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitFromKeyboard() }),
                    colors = treatedPlateFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .height(FormControlHeight)
                        .border(
                            1.dp,
                            if (error == null) FieldTheme.strong else FieldTheme.alert,
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
            TextButton(
                onClick = onCommit,
                modifier = Modifier
                    .height(FormControlHeight)
                    .border(1.dp, FieldTheme.strong, RoundedCornerShape(4.dp)),
            ) {
                Text("הוספה", style = TypeScale.bodyStrong, color = FieldTheme.accent)
            }
        }
        TextButton(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(FormControlHeight)
                .border(1.dp, FieldTheme.strong, RoundedCornerShape(4.dp)),
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = FieldTheme.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("סריקה ניסיונית", style = TypeScale.bodyStrong, color = FieldTheme.accent)
        }
        error?.let { Text(it, style = TypeScale.caption, color = FieldTheme.alert) }
    }
}

@Composable
private fun TreatedPlateCard(
    row: TreatedPlate,
    removable: Boolean,
    onRemove: (String) -> Unit,
    onLeftWhereChange: (String, String) -> Unit,
) {
    val caption = treatedPlateCaption(model = row.model, color = row.color)
    val leftWhere = row.leftWhere?.trim().orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.raised, RoundedCornerShape(8.dp))
            .border(1.dp, FieldTheme.hairline, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LicensePlate(plate = row.plateNumber)
            CarLogo(slug = row.logoSlug)
            Spacer(Modifier.weight(1f))
            if (removable) {
                IconButton(
                    onClick = { onRemove(row.plateNumber) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "הסרת מספר ${row.plateNumber}",
                        tint = FieldTheme.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (removable) {
            TextField(
                value = leftWhere,
                onValueChange = { onLeftWhereChange(row.plateNumber, it) },
                singleLine = true,
                textStyle = TypeScale.body,
                placeholder = {
                    Text("איפה הרכב הושאר", style = TypeScale.body, color = FieldTheme.textMuted)
                },
                colors = treatedPlateFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FormControlHeight)
                    .border(1.dp, FieldTheme.strong, RoundedCornerShape(4.dp)),
            )
        } else if (leftWhere.isNotEmpty()) {
            Text(
                text = leftWhere,
                style = TypeScale.body,
                color = FieldTheme.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun treatedPlateFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldTheme.raised,
    unfocusedContainerColor = FieldTheme.raised,
    disabledContainerColor = FieldTheme.raised,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = FieldTheme.textPrimary,
    unfocusedTextColor = FieldTheme.textPrimary,
)
