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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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

/**
 * Shows [OnShiftBar] for the live shift on every screen but that shift's own screen.
 * Returns whether the bar is shown, so the screen below can skip the status-bar room the bar already takes.
 * [statusBarPadding] is false when a banner above already sits under the status bar.
 */
@Composable
fun OnShiftBarHost(
    shift: CurrentShift,
    onShiftScreen: Boolean,
    onOpen: () -> Unit,
    statusBarPadding: Boolean = true,
): Boolean {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CLOCK_TICK_MS)
            now = Instant.now()
        }
    }
    val phase = shiftWindowPhase(shift.startsAt, shift.endsAt, now)
    if (!showBar(phase, onShiftScreenOfThisShift = onShiftScreen)) return false
    OnShiftBar(text = barText(shift, phase, now), phase = phase, onOpen = onOpen, statusBarPadding = statusBarPadding)
    return true
}

@Composable
fun OnShiftBar(text: String, phase: ShiftPhase, onOpen: () -> Unit, statusBarPadding: Boolean = true) {
    // Tinted like the web bar: the accent tint while on shift, the amber tint while finishing.
    val finishing = phase == ShiftPhase.FINISHING
    val background = if (finishing) FieldTheme.partialTint else FieldTheme.accentSubtle
    val ink = if (finishing) FieldTheme.partialOnTint else FieldTheme.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .then(if (statusBarPadding) Modifier.statusBarsPadding() else Modifier)
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
