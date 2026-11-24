package com.yahpz.responder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import com.yahpz.domain.LIST_DETAIL_EMPTY_HINT
import com.yahpz.domain.LIST_PANE_WIDTH_DP
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.yahpz.domain.AdaptiveLayout
import com.yahpz.domain.SHEET_MAX_WIDTH_DP
import com.yahpz.domain.adaptiveLayoutFor

@Composable
internal fun currentAdaptiveLayout(): AdaptiveLayout =
    adaptiveLayoutFor(LocalConfiguration.current.screenWidthDp)

/** Caps a single-column screen near 600dp and centres it. On a phone the content fills the width. */
@Composable
internal fun AdaptiveContentFrame(
    layout: AdaptiveLayout,
    content: @Composable () -> Unit,
) {
    val maxWidthDp = layout.contentMaxWidthDp
    if (maxWidthDp == null) {
        content()
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidthDp.dp).fillMaxHeight()) { content() }
    }
}

/** A bottom sheet that stays [SHEET_MAX_WIDTH_DP] wide on big screens instead of stretching. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptiveBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = SHEET_MAX_WIDTH_DP.dp,
        containerColor = containerColor,
        content = content,
    )
}

internal const val LIST_PANE_TAG = "list-pane"
internal const val DETAIL_PANE_TAG = "detail-pane"

/**
 * On expanded width the list keeps [LIST_PANE_WIDTH_DP] at the start and the opened item shows beside it.
 * Otherwise only the list is drawn and the caller opens the item its own way. Back closes the pane first.
 */
@Composable
internal fun ListDetailLayout(
    layout: AdaptiveLayout,
    detailPane: (@Composable () -> Unit)?,
    onCloseDetail: () -> Unit,
    list: @Composable () -> Unit,
) {
    if (!layout.listDetail) {
        list()
        return
    }
    BackHandler(enabled = detailPane != null, onBack = onCloseDetail)
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(LIST_PANE_WIDTH_DP.dp).fillMaxHeight().testTag(LIST_PANE_TAG)) { list() }
        Box(Modifier.weight(1f).fillMaxHeight().background(FieldTheme.raised)) {
            if (detailPane == null) {
                Text(
                    LIST_DETAIL_EMPTY_HINT,
                    style = TypeScale.body,
                    color = FieldTheme.textMuted,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                Box(Modifier.fillMaxSize().testTag(DETAIL_PANE_TAG)) { detailPane() }
            }
        }
    }
}
