package com.yahpz.responder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import com.yahpz.domain.EVENT_MEDIA_COMPRESS_FAIL
import com.yahpz.domain.EVENT_MEDIA_HEIC_FAIL
import com.yahpz.domain.EVENT_MEDIA_MAX_OUTPUT_BYTES
import com.yahpz.domain.EVENT_MEDIA_QUALITY_STEPS
import com.yahpz.domain.jpegQualityPercent
import com.yahpz.domain.nextJpegQuality
import com.yahpz.domain.rejectOriginalFile
import com.yahpz.domain.resolvePickedMimeType
import com.yahpz.domain.targetDimensions
import java.io.ByteArrayOutputStream

data class CompressedEventImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

sealed class CompressEventImageResult {
    data class Ok(val image: CompressedEventImage) : CompressEventImageResult()
    data class Error(val message: String) : CompressEventImageResult()
}

/** The picked file's type, falling back to its file name when the provider reports none. */
fun pickedMimeType(context: Context, uri: Uri): String? {
    val reported = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment
    return resolvePickedMimeType(reported, name)
}

fun compressEventImage(context: Context, uri: Uri): CompressEventImageResult {
    val mime = pickedMimeType(context, uri) ?: "image/jpeg"
    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    rejectOriginalFile(mime, size)?.let { return CompressEventImageResult.Error(it) }

    val orientation = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            ExifInterface(pfd.fileDescriptor).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: return CompressEventImageResult.Error(EVENT_MEDIA_HEIC_FAIL)

    val oriented = applyExifOrientation(decoded, orientation)
    if (oriented !== decoded) decoded.recycle()

    val (targetW, targetH) = targetDimensions(oriented.width, oriented.height)
    val resized = if (oriented.width == targetW && oriented.height == targetH) {
        oriented
    } else {
        Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
            if (it !== oriented) oriented.recycle()
        }
    }
    // JPEG has no transparency: a see-through PNG or screenshot would turn black without this.
    val scaled = if (resized.hasAlpha()) onWhite(resized).also { resized.recycle() } else resized

    return try {
        var qualityIndex = 0
        var bytes: ByteArray? = null
        while (qualityIndex < EVENT_MEDIA_QUALITY_STEPS.size) {
            val quality = EVENT_MEDIA_QUALITY_STEPS[qualityIndex]
            val out = ByteArrayOutputStream()
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, jpegQualityPercent(quality), out)) {
                return CompressEventImageResult.Error(EVENT_MEDIA_COMPRESS_FAIL)
            }
            bytes = out.toByteArray()
            val next = nextJpegQuality(bytes.size, qualityIndex)
            if (next == null) break
            qualityIndex += 1
        }
        val jpeg = bytes
        if (jpeg == null || jpeg.size > EVENT_MEDIA_MAX_OUTPUT_BYTES) {
            CompressEventImageResult.Error(EVENT_MEDIA_COMPRESS_FAIL)
        } else {
            CompressEventImageResult.Ok(CompressedEventImage(jpeg, targetW, targetH))
        }
    } catch (_: Exception) {
        CompressEventImageResult.Error(EVENT_MEDIA_COMPRESS_FAIL)
    } finally {
        scaled.recycle()
    }
}

private fun onWhite(bitmap: Bitmap): Bitmap {
    val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(opaque).apply {
        drawColor(Color.WHITE)
        drawBitmap(bitmap, 0f, 0f, null)
    }
    return opaque
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
