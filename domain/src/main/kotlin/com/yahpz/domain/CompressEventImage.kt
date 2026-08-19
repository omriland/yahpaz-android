package com.yahpz.domain

const val EVENT_MEDIA_MAX_ORIGINAL_BYTES = 20L * 1024 * 1024
const val EVENT_MEDIA_MAX_OUTPUT_BYTES = (1.5 * 1024 * 1024).toInt()
const val EVENT_MEDIA_MAX_LONG_EDGE = 1600
const val EVENT_MEDIA_TARGET_BYTES = 700 * 1024
val EVENT_MEDIA_QUALITY_STEPS = listOf(0.72, 0.6, 0.5)

fun rejectOriginalFile(mimeType: String, byteSize: Long): String? {
    if (byteSize <= 0L) return EVENT_MEDIA_BAD_TYPE
    if (!mimeType.startsWith("image/")) return EVENT_MEDIA_BAD_TYPE
    if (byteSize > EVENT_MEDIA_MAX_ORIGINAL_BYTES) return EVENT_MEDIA_TOO_LARGE
    return null
}

fun targetDimensions(
    width: Int,
    height: Int,
    maxLongEdge: Int = EVENT_MEDIA_MAX_LONG_EDGE,
): Pair<Int, Int> {
    val longEdge = maxOf(width, height)
    if (longEdge <= maxLongEdge) return width to height
    val scale = maxLongEdge.toDouble() / longEdge.toDouble()
    return maxOf(1, kotlin.math.round(width * scale).toInt()) to
        maxOf(1, kotlin.math.round(height * scale).toInt())
}

fun nextJpegQuality(byteSize: Int, qualityIndex: Int): Double? {
    if (byteSize <= EVENT_MEDIA_TARGET_BYTES) return null
    return EVENT_MEDIA_QUALITY_STEPS.getOrNull(qualityIndex + 1)
}

fun jpegQualityPercent(quality: Double): Int =
    (quality * 100.0).toInt().coerceIn(1, 100)
