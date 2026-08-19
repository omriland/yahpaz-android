package com.yahpz.responder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yahpz.domain.formatPlate

/** Plate yellow + IL euroband. Band must be #17416E (not Field accent #1D4E89). */
private val PlateFieldYellow = Color(0xFFF5C400)
private val PlateBandBlue = Color(0xFF17416E)

/** Read-only Israeli civil plate mark (IL band + serial). Height 36dp, Field plate colors. */
@Composable
fun LicensePlate(plate: String, modifier: Modifier = Modifier) {
    val serial = formatPlate(plate)
    if (serial.isEmpty()) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PlateFieldYellow)
                .border(1.dp, FieldTheme.textPrimary, RoundedCornerShape(4.dp))
                .semantics { contentDescription = serial },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(PlateBandBlue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IsraelFlagMark(band = PlateBandBlue)
                Text(
                    text = "IL",
                    style = TypeScale.numeric.copy(fontSize = 12.sp, color = Color.White),
                )
            }
            Text(
                text = serial,
                style = TypeScale.numeric.copy(fontSize = 16.sp, color = FieldTheme.textPrimary),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun IsraelFlagMark(band: Color) {
    Canvas(
        modifier = Modifier
            .width(16.dp)
            .height(12.dp),
    ) {
        val w = size.width
        val h = size.height
        drawRect(Color.White, size = Size(w, h))
        val stripeH = h * (2.2f / 16f)
        drawRect(band, topLeft = Offset(0f, h * (2f / 16f)), size = Size(w, stripeH))
        drawRect(band, topLeft = Offset(0f, h * (11.8f / 16f)), size = Size(w, stripeH))
        val up = Path().apply {
            moveTo(w * 0.5f, h * (4.1f / 16f))
            lineTo(w * (14.4f / 22f), h * (10.1f / 16f))
            lineTo(w * (7.6f / 22f), h * (10.1f / 16f))
            close()
        }
        drawPath(up, band, style = Stroke(width = 1.dp.toPx()))
        val down = Path().apply {
            moveTo(w * 0.5f, h * (11.9f / 16f))
            lineTo(w * (7.6f / 22f), h * (5.9f / 16f))
            lineTo(w * (14.4f / 22f), h * (5.9f / 16f))
            close()
        }
        drawPath(down, band, style = Stroke(width = 1.dp.toPx()))
    }
}
