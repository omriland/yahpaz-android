package com.yahpz.responder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Small disc (+ optional chip label) markers — not the default Google teardrop. */
object MapPinBitmaps {
    private val cache = object : LruCache<String, BitmapDescriptor>(128) {}

    fun disc(
        context: Context,
        fill: Color,
        stroke: Color = FieldTheme.raised,
        label: String? = null,
        labelColor: Color = FieldTheme.textPrimary,
    ): BitmapDescriptor {
        val densityDpi = context.resources.displayMetrics.densityDpi
        val key = "d:${fill.toArgb()}:${stroke.toArgb()}:${label.orEmpty()}:${labelColor.toArgb()}:$densityDpi"
        cache.get(key)?.let { return it }
        val density = context.resources.displayMetrics.density
        val drawn = drawLabeled(density, fill.toArgb(), stroke.toArgb(), label, labelColor.toArgb())
        val descriptor = BitmapDescriptorFactory.fromBitmap(drawn.bitmap)
        cache.put(key, descriptor)
        return descriptor
    }

    fun cluster(context: Context, count: Int): BitmapDescriptor {
        val densityDpi = context.resources.displayMetrics.densityDpi
        val key = "c:$count:$densityDpi"
        cache.get(key)?.let { return it }
        val density = context.resources.displayMetrics.density
        val descriptor = BitmapDescriptorFactory.fromBitmap(
            drawBadge(density, FieldTheme.accent.toArgb(), FieldTheme.raised.toArgb(), count.toString()),
        )
        cache.put(key, descriptor)
        return descriptor
    }

    fun anchorVForLabeled(hasLabel: Boolean): Float = if (hasLabel) 0.22f else 0.5f

    private data class Drawn(val bitmap: Bitmap)

    private fun drawBadge(density: Float, fillArgb: Int, strokeArgb: Int, text: String): Bitmap {
        val size = 18f * density
        val strokeW = 1.5f * density
        val pad = 2f * density
        val dim = (size + strokeW * 2 + pad * 2).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = dim / 2f
        val cy = dim / 2f
        val radius = size / 2f
        canvas.drawCircle(
            cx,
            cy,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillArgb
            },
        )
        canvas.drawCircle(
            cx,
            cy,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = strokeArgb
            },
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FieldTheme.textOnAccent.toArgb()
            textSize = if (text.length >= 3) 8f * density else 10f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, cx, textY, textPaint)
        return bitmap
    }

    private fun drawLabeled(
        density: Float,
        fillArgb: Int,
        strokeArgb: Int,
        label: String?,
        labelArgb: Int,
    ): Drawn {
        val dot = 9f * density
        val strokeW = 1.5f * density
        val pad = 2f * density
        val gap = 2f * density

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelArgb
            textSize = 10f * density
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val chipPadX = 5f * density
        val chipPadY = 2.5f * density
        val chipRadius = 3.5f * density
        val hasLabel = !label.isNullOrBlank()
        val textWidth = if (hasLabel) textPaint.measureText(label) else 0f
        val chipW = if (hasLabel) textWidth + chipPadX * 2 else 0f
        val chipH = if (hasLabel) textPaint.textSize + chipPadY * 2 else 0f

        val contentW = maxOf(dot + strokeW * 2, chipW)
        val width = (contentW + pad * 2).toInt().coerceAtLeast(1)
        val height = (
            pad + dot + strokeW * 2 +
                (if (hasLabel) gap + chipH else 0f) +
                pad
            ).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = width / 2f
        val cy = pad + strokeW + dot / 2f

        canvas.drawCircle(
            cx,
            cy,
            dot / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillArgb
            },
        )
        canvas.drawCircle(
            cx,
            cy,
            dot / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = strokeArgb
            },
        )

        if (hasLabel) {
            val chipTop = cy + dot / 2f + strokeW + gap
            val chipLeft = (width - chipW) / 2f
            val chipRect = RectF(chipLeft, chipTop, chipLeft + chipW, chipTop + chipH)
            canvas.drawRoundRect(
                chipRect,
                chipRadius,
                chipRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = FieldTheme.raised.toArgb()
                },
            )
            canvas.drawRoundRect(
                chipRect,
                chipRadius,
                chipRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = density
                    color = FieldTheme.hairline.toArgb()
                },
            )
            val textY = chipRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label!!, cx, textY, textPaint)
        }

        return Drawn(bitmap)
    }
}
