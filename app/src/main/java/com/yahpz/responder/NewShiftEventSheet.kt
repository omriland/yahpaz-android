package com.yahpz.responder

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yahpz.domain.LookupOption
import com.yahpz.domain.NEW_EVENT_HINT
import com.yahpz.domain.NEW_EVENT_MORE_TYPES_TITLE
import com.yahpz.domain.NEW_EVENT_SHEET_CLOSE
import com.yahpz.domain.NEW_EVENT_SHEET_TITLE
import com.yahpz.domain.orderEventTypesForNewEvent

private val TypeTileShape = RoundedCornerShape(4.dp)
private val TypeTileMinHeight = 56.dp
private val TypeChipMinHeight = 48.dp
private const val PRIMARY_TILES_PER_ROW = 2

/** One tap on an event type opens the event; the primary types are big tiles, the rest smaller chips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewShiftEventSheet(
    types: List<LookupOption>,
    errorMessage: String?,
    onPick: (LookupOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val ordered = orderEventTypesForNewEvent(types) { it.name }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(NEW_EVENT_SHEET_TITLE, style = TypeScale.section, color = FieldTheme.textPrimary)
            PrimaryTypeTiles(ordered.primary, onPick)
            if (ordered.more.isNotEmpty()) MoreTypeChips(ordered.more, onPick)
            errorMessage?.let { Text(it, style = TypeScale.body, color = FieldTheme.alert) }
            Text(NEW_EVENT_HINT, style = TypeScale.caption, color = FieldTheme.textMuted)
            GhostButton(title = NEW_EVENT_SHEET_CLOSE, onClick = onDismiss, modifier = Modifier.heightIn(min = TypeChipMinHeight))
        }
    }
}

@Composable
private fun PrimaryTypeTiles(types: List<LookupOption>, onPick: (LookupOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(PRIMARY_TILES_PER_ROW).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTypes.forEach { type ->
                    TypeButton(type, TypeTileMinHeight, onPick, Modifier.weight(1f))
                }
                if (rowTypes.size < PRIMARY_TILES_PER_ROW) Box(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreTypeChips(types: List<LookupOption>, onPick: (LookupOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(NEW_EVENT_MORE_TYPES_TITLE, style = TypeScale.label, color = FieldTheme.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            types.forEach { type -> TypeButton(type, TypeChipMinHeight, onPick, Modifier) }
        }
    }
}

@Composable
private fun TypeButton(
    type: LookupOption,
    minHeight: Dp,
    onPick: (LookupOption) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .border(1.dp, FieldTheme.strong, TypeTileShape)
            .clickable(role = Role.Button) { onPick(type) }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(type.name, style = TypeScale.bodyStrong, color = FieldTheme.textPrimary)
    }
}
