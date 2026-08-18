package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.LookupOption
import com.yahpz.domain.filterAssignableProfiles
import com.yahpz.domain.textIncludesQuery

private val fieldShape = RoundedCornerShape(4.dp)

/** Read-only field that opens a sheet. Keeps the tap target at 44dp like every other row. */
@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(FieldTheme.raised, fieldShape)
                .border(1.dp, if (error == null) FieldTheme.strong else FieldTheme.alert, fieldShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                style = TypeScale.body,
                color = if (value.isEmpty()) FieldTheme.textMuted else FieldTheme.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = FieldTheme.accent)
        }
        if (error != null) {
            Text(error, style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

@Composable
private fun ChoiceRow(title: String, caption: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (selected) TypeScale.bodyStrong else TypeScale.body,
                color = FieldTheme.textPrimary,
            )
            if (caption != null) {
                Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
            }
        }
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = FieldTheme.accent)
        }
    }
}

/** Inline selector for short closed lists (shift kind, vehicle type). */
@Composable
fun OptionRowSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    error: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TypeScale.label, color = FieldTheme.textSecondary)
        FieldCard {
            options.forEachIndexed { index, (value, title) ->
                ChoiceRow(
                    title = title,
                    caption = null,
                    selected = selected == value,
                    onClick = { onSelect(value) },
                )
                if (index < options.lastIndex) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(FieldTheme.hairline),
                    )
                }
            }
        }
        if (error != null) {
            Text(error, style = TypeScale.caption, color = FieldTheme.alert)
        }
    }
}

/** Searchable single-select over a long closed list (roads, event types, שלוחות). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupPickerField(
    label: String,
    options: List<LookupOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    placeholder: String,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    allowClear: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name.orEmpty()

    PickerField(
        label = label,
        value = selectedName,
        placeholder = placeholder,
        error = error,
        onClick = {
            query = ""
            open = true
        },
        modifier = modifier,
    )

    if (open) {
        val trimmed = query.trim()
        val visible = if (trimmed.isEmpty()) {
            options
        } else {
            options.filter { textIncludesQuery(it.name, trimmed) }
        }
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label, style = TypeScale.section, color = FieldTheme.textPrimary)
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = searchPlaceholder,
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (allowClear) {
                        ChoiceRow(
                            title = "ללא",
                            caption = null,
                            selected = selectedId.isEmpty(),
                            onClick = {
                                onSelect("")
                                open = false
                            },
                        )
                    }
                    if (visible.isEmpty()) {
                        Text(
                            "לא נמצאו פריטים תואמים",
                            style = TypeScale.body,
                            color = FieldTheme.textMuted,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                    visible.forEach { option ->
                        ChoiceRow(
                            title = option.name,
                            caption = null,
                            selected = option.id == selectedId,
                            onClick = {
                                onSelect(option.id)
                                open = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Searchable multi-select over the assignable crew. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewPickerField(
    label: String,
    profiles: List<AssignableProfile>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    placeholder: String,
    caption: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedNames = selectedIds.mapNotNull { id ->
        profiles.firstOrNull { it.id == id }?.display
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PickerField(
            label = label,
            value = selectedNames.joinToString(", "),
            placeholder = placeholder,
            error = error,
            onClick = {
                query = ""
                open = true
            },
        )
        Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
    }

    if (open) {
        val visible = filterAssignableProfiles(profiles, query)
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label, style = TypeScale.section, color = FieldTheme.textPrimary)
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "חיפוש לפי שם או או״ק",
                )
                Text(caption, style = TypeScale.caption, color = FieldTheme.textMuted)
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (visible.isEmpty()) {
                        Text(
                            "לא נמצאו כוננים תואמים",
                            style = TypeScale.body,
                            color = FieldTheme.textMuted,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                    visible.forEach { profile ->
                        ChoiceRow(
                            title = profile.fullName.ifEmpty { "כונן" },
                            caption = profile.callsign.takeIf { it.isNotEmpty() },
                            selected = selectedIds.contains(profile.id),
                            onClick = { onToggle(profile.id) },
                        )
                    }
                }
                PrimaryButton(title = "סיום", onClick = { open = false })
            }
        }
    }
}
