package com.yahpz.responder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yahpz.domain.CurrentShift
import com.yahpz.domain.ON_SHIFT_BAR_ACTION
import com.yahpz.domain.ShiftPhase
import com.yahpz.domain.barText
import com.yahpz.domain.shiftWindowPhase
import com.yahpz.domain.showBar
import java.time.Instant

/** Shows [OnShiftBar] for the live shift on every screen but that shift's own screen. */
@Composable
fun OnShiftBarHost(shift: CurrentShift, onShiftScreen: Boolean, onOpen: () -> Unit) {
    val now = Instant.now()
    val phase = shiftWindowPhase(shift.startsAt, shift.endsAt, now)
    if (!showBar(phase, onShiftScreenOfThisShift = onShiftScreen)) return
    OnShiftBar(text = barText(shift, phase, now), phase = phase, onOpen = onOpen)
}

@Composable
fun OnShiftBar(text: String, phase: ShiftPhase, onOpen: () -> Unit) {
    // Tinted like the web bar: the accent tint while on shift, the amber tint while finishing.
    val finishing = phase == ShiftPhase.FINISHING
    val background = if (finishing) FieldTheme.partialTint else FieldTheme.accentSubtle
    val ink = if (finishing) FieldTheme.partialOnTint else FieldTheme.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = TypeScale.caption,
            color = ink,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        TextButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = ink),
        ) {
            Text(ON_SHIFT_BAR_ACTION, style = TypeScale.caption)
        }
    }
}
