package com.yahpz.responder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
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
        phoneInLabel: Boolean = false,
    ): BitmapDescriptor {
        val densityDpi = context.resources.displayMetrics.densityDpi
        val key = "d:${fill.toArgb()}:${stroke.toArgb()}:${label.orEmpty()}:${labelColor.toArgb()}:$phoneInLabel:$densityDpi"
        cache.get(key)?.let { return it }
        val density = context.resources.displayMetrics.density
        val drawn = drawLabeled(
            density,
            fill.toArgb(),
            stroke.toArgb(),
            label,
            labelColor.toArgb(),
            phoneInLabel,
        )
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
        phoneInLabel: Boolean,
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
        val phoneSize = if (hasLabel && phoneInLabel) 10f * density else 0f
        val phoneGap = if (phoneSize > 0f) 3f * density else 0f
        val textWidth = if (hasLabel) textPaint.measureText(label) else 0f
        val chipW = if (hasLabel) textWidth + chipPadX * 2 + phoneSize + phoneGap else 0f
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
            if (phoneSize > 0f) {
                val iconRight = chipRect.right - chipPadX
                drawPhone(
                    canvas,
                    left = iconRight - phoneSize,
                    top = chipRect.centerY() - phoneSize / 2f,
                    size = phoneSize,
                    color = labelArgb,
                )
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(label!!, iconRight - phoneSize - phoneGap, textY, textPaint)
            } else {
                canvas.drawText(label!!, cx, textY, textPaint)
            }
        }

        return Drawn(bitmap)
    }

    private fun drawPhone(canvas: Canvas, left: Float, top: Float, size: Float, color: Int) {
        val path = PathParser.createPathFromPathData(PHONE_PATH)
        val matrix = Matrix()
        val scale = size / 24f
        matrix.setScale(scale, scale)
        matrix.postTranslate(left, top)
        path.transform(matrix)
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.color = color
            },
        )
    }

    private const val PHONE_PATH =
        "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"
}
