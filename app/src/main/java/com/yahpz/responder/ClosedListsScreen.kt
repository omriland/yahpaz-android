package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yahpz.domain.BROADCAST_CAPTION
import com.yahpz.domain.BROADCAST_TITLE
import com.yahpz.domain.CLOSED_LISTS
import com.yahpz.domain.CLOSED_LISTS_SEARCH_PLACEHOLDER
import com.yahpz.domain.CLOSED_LISTS_TITLE
import com.yahpz.domain.CLOSED_LIST_ADD
import com.yahpz.domain.CLOSED_LIST_CREATED
import com.yahpz.domain.CLOSED_LIST_DELETED
import com.yahpz.domain.CLOSED_LIST_EDIT
import com.yahpz.domain.CLOSED_LIST_EMPTY
import com.yahpz.domain.CLOSED_LIST_LOAD_FAILED
import com.yahpz.domain.CLOSED_LIST_LOAD_FAILED_CAPTION
import com.yahpz.domain.CLOSED_LIST_NAME_LABEL
import com.yahpz.domain.CLOSED_LIST_NO_RESULTS
import com.yahpz.domain.CLOSED_LIST_REMOVE
import com.yahpz.domain.CLOSED_LIST_SYSTEM_BADGE
import com.yahpz.domain.CLOSED_LIST_UPDATED
import com.yahpz.domain.ClosedListItem
import com.yahpz.domain.ClosedListKey
import com.yahpz.domain.ClosedListMutationResult
import com.yahpz.domain.SETTINGS_LIST_GROUP_LABEL
import com.yahpz.domain.SYSTEM_DISTRICT_LOCKED_ERROR
import com.yahpz.domain.StampTone
import com.yahpz.domain.canMutateClosedListItem
import com.yahpz.domain.closedListMeta
import com.yahpz.domain.filterClosedListItems
import kotlinx.coroutines.launch

private sealed class ClosedListEditor {
    data object Create : ClosedListEditor()
    data class Edit(val item: ClosedListItem) : ClosedListEditor()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosedListsScreen(app: AppModel, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var selectedKey by remember { mutableStateOf<ClosedListKey?>(null) }
    var items by remember { mutableStateOf<List<ClosedListItem>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<ClosedListEditor?>(null) }
    var draftName by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var actionItem by remember { mutableStateOf<ClosedListItem?>(null) }

    BackHandler(enabled = selectedKey != null) {
        selectedKey = null
        editor = null
        banner = null
        query = ""
        actionItem = null
    }

    LaunchedEffect(selectedKey, reloadKey) {
        val key = selectedKey ?: run {
            items = null
            failed = false
            return@LaunchedEffect
        }
        items = null
        failed = false
        banner = null
        editor = null
        runCatching { YahpazAPI.fetchClosedListItems(key) }
            .onSuccess { items = it }
            .onFailure { failed = true }
    }

    val meta = selectedKey?.let { closedListMeta(it) }
    val filtered = filterClosedListItems(items.orEmpty(), query)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.page)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedKey == null) {
            if (onBack != null) {
                ToolsBackRow("הגדרות", onBack)
            } else {
                Text("הגדרות", style = TypeScale.title, color = FieldTheme.textPrimary)
            }
            Text(SETTINGS_LIST_GROUP_LABEL, style = TypeScale.label, color = FieldTheme.textSecondary)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CLOSED_LISTS.forEach { list ->
                    ClosedListPaneCard(
                        title = list.label,
                        caption = list.description,
                        onClick = { selectedKey = list.key },
                    )
                }
                Text(
                    "תפוצה",
                    style = TypeScale.label,
                    color = FieldTheme.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ClosedListPaneCard(
                    title = BROADCAST_TITLE,
                    caption = BROADCAST_CAPTION,
                    onClick = { app.setToolsDestination(ToolsDestination.BROADCAST) },
                )
                Spacer(Modifier.height(24.dp))
            }
        } else {
            ToolsBackRow(meta?.label ?: CLOSED_LISTS_TITLE) {
                selectedKey = null
                editor = null
                banner = null
                query = ""
            }
            meta?.description?.let { description ->
                Text(description, style = TypeScale.caption, color = FieldTheme.textMuted)
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = CLOSED_LISTS_SEARCH_PLACEHOLDER,
            )
            GhostButton(
                title = CLOSED_LIST_ADD,
                enabled = editor == null,
                onClick = {
                    banner = null
                    formError = null
                    draftName = ""
                    editor = ClosedListEditor.Create
                },
            )
            banner?.let {
                Text(it, style = TypeScale.caption, color = FieldTheme.accent)
            }
            when {
                items == null && !failed -> LoadingBlock("טוען רשימה…")
                failed -> EmptyState(
                    title = CLOSED_LIST_LOAD_FAILED,
                    caption = CLOSED_LIST_LOAD_FAILED_CAPTION,
                    actionTitle = "רענון",
                    onAction = { reloadKey += 1 },
                )
                items != null && items!!.isEmpty() && editor !is ClosedListEditor.Create -> EmptyState(
                    title = CLOSED_LIST_EMPTY,
                    actionTitle = CLOSED_LIST_ADD,
                    onAction = {
                        formError = null
                        draftName = ""
                        editor = ClosedListEditor.Create
                    },
                )
                items != null && filtered.isEmpty() && editor == null -> EmptyState(
                    title = if (query.isBlank()) CLOSED_LIST_EMPTY else CLOSED_LIST_NO_RESULTS,
                    actionTitle = if (query.isBlank()) null else "ניקוי חיפוש",
                    onAction = if (query.isBlank()) null else ({ query = "" }),
                )
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (editor is ClosedListEditor.Create) {
                        ClosedListInlineEditor(
                            draftName = draftName,
                            onDraftChange = { draftName = it },
                            formError = formError,
                            saving = saving,
                            onSave = {
                                val key = selectedKey ?: return@ClosedListInlineEditor
                                scope.launch {
                                    saving = true
                                    formError = null
                                    when (val result = YahpazAPI.createClosedListItem(key, draftName)) {
                                        is ClosedListMutationResult.Ok -> {
                                            app.showToast(CLOSED_LIST_CREATED, StampTone.DONE)
                                            editor = null
                                            draftName = ""
                                            reloadKey += 1
                                        }
                                        is ClosedListMutationResult.Err -> formError = result.error
                                    }
                                    saving = false
                                }
                            },
                            onCancel = {
                                editor = null
                                draftName = ""
                                formError = null
                            },
                        )
                    }
                    filtered.forEach { item ->
                        val key = selectedKey!!
                        val editing = editor is ClosedListEditor.Edit &&
                            (editor as ClosedListEditor.Edit).item.id == item.id
                        if (editing) {
                            ClosedListInlineEditor(
                                draftName = draftName,
                                onDraftChange = { draftName = it },
                                formError = formError,
                                saving = saving,
                                onSave = {
                                    scope.launch {
                                        saving = true
                                        formError = null
                                        when (
                                            val result = YahpazAPI.updateClosedListItem(key, item.id, draftName)
                                        ) {
                                            is ClosedListMutationResult.Ok -> {
                                                app.showToast(CLOSED_LIST_UPDATED, StampTone.DONE)
                                                editor = null
                                                draftName = ""
                                                reloadKey += 1
                                            }
                                            is ClosedListMutationResult.Err -> formError = result.error
                                        }
                                        saving = false
                                    }
                                },
                                onCancel = {
                                    editor = null
                                    draftName = ""
                                    formError = null
                                },
                            )
                        } else {
                            ClosedListItemRow(
                                item = item,
                                locked = !canMutateClosedListItem(key, item),
                                onClick = {
                                    if (!canMutateClosedListItem(key, item)) {
                                        app.showToast(SYSTEM_DISTRICT_LOCKED_ERROR, StampTone.PENDING)
                                    } else {
                                        actionItem = item
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    actionItem?.let { opened ->
        val key = selectedKey ?: return@let
        ModalBottomSheet(onDismissRequest = { actionItem = null }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(opened.name, style = TypeScale.section, color = FieldTheme.textPrimary)
                GhostButton(
                    title = CLOSED_LIST_EDIT,
                    onClick = {
                        actionItem = null
                        formError = null
                        draftName = opened.name
                        editor = ClosedListEditor.Edit(opened)
                    },
                )
                GhostButton(
                    title = CLOSED_LIST_REMOVE,
                    onClick = {
                        scope.launch {
                            when (val result = YahpazAPI.deleteClosedListItem(key, opened.id)) {
                                is ClosedListMutationResult.Ok -> {
                                    app.showToast(CLOSED_LIST_DELETED, StampTone.DONE)
                                    actionItem = null
                                    reloadKey += 1
                                }
                                is ClosedListMutationResult.Err -> {
                                    if (result.inUse) {
                                        banner = result.error
                                        actionItem = null
                                    } else {
                                        app.showToast(result.error, StampTone.PENDING)
                                    }
                                }
                            }
                        }
                    },
                )
                TextButton(onClick = { actionItem = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("סגירה", color = FieldTheme.accent)
                }
            }
        }
    }
}

@Composable
private fun ClosedListPaneCard(title: String, caption: String?, onClick: () -> Unit) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = TypeScale.section, color = FieldTheme.textPrimary)
                if (!caption.isNullOrBlank()) {
                    Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = FieldTheme.accent,
            )
        }
    }
}

@Composable
private fun ClosedListItemRow(item: ClosedListItem, locked: Boolean, onClick: () -> Unit) {
    FieldCard(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = TypeScale.body, color = FieldTheme.textPrimary)
                if (locked) {
                    Text(CLOSED_LIST_SYSTEM_BADGE, style = TypeScale.caption, color = FieldTheme.textMuted)
                }
            }
            if (!locked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = FieldTheme.accent,
                )
            }
        }
    }
}

@Composable
private fun ClosedListInlineEditor(
    draftName: String,
    onDraftChange: (String) -> Unit,
    formError: String?,
    saving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    FieldCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(
                label = CLOSED_LIST_NAME_LABEL,
                value = draftName,
                onValueChange = onDraftChange,
                error = formError,
                enabled = !saving,
                onSubmit = onSave,
            )
            PrimaryButton(
                title = if (saving) "שומר…" else "שמירה",
                enabled = !saving,
                busy = saving,
                onClick = onSave,
            )
            GhostButton(
                title = "ביטול",
                enabled = !saving,
                onClick = onCancel,
            )
        }
    }
}
