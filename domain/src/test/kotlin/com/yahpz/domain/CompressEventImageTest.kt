package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompressEventImageTest {
    @Test
    fun rejectEmptyVideoAndOversized() {
        assertEquals(EVENT_MEDIA_BAD_TYPE, rejectOriginalFile("image/jpeg", 0))
        assertEquals(EVENT_MEDIA_BAD_TYPE, rejectOriginalFile("video/mp4", 1000))
        assertEquals(EVENT_MEDIA_BAD_TYPE, rejectOriginalFile("application/pdf", 1000))
        assertEquals(
            EVENT_MEDIA_TOO_LARGE,
            rejectOriginalFile("image/jpeg", EVENT_MEDIA_MAX_ORIGINAL_BYTES + 1),
        )
    }

    @Test
    fun allowCommonImageTypesUnderCap() {
        assertNull(rejectOriginalFile("image/jpeg", 1000))
        assertNull(rejectOriginalFile("image/png", 1000))
        assertNull(rejectOriginalFile("image/webp", 1000))
        assertNull(rejectOriginalFile("image/heic", 1000))
        assertNull(rejectOriginalFile("image/heif", 1000))
    }

    @Test
    fun neverUpscale() {
        assertEquals(800 to 600, targetDimensions(800, 600))
    }

    @Test
    fun fitLongEdgeTo1600() {
        assertEquals(1600 to 1200, targetDimensions(3200, 2400))
        assertEquals(600 to 1600, targetDimensions(1200, 3200))
        assertEquals(1600, EVENT_MEDIA_MAX_LONG_EDGE)
    }

    @Test
    fun nextJpegQualityStopsWhenSmallEnough() {
        assertNull(nextJpegQuality(500_000, 0))
    }

    @Test
    fun nextJpegQualityStepsThenNull() {
        assertEquals(0.6, nextJpegQuality(800_000, 0))
        assertEquals(0.5, nextJpegQuality(800_000, 1))
        assertNull(nextJpegQuality(800_000, 2))
    }

    @Test
    fun jpegQualityPercentMatchesAndroidCompressScale() {
        assertEquals(72, jpegQualityPercent(0.72))
        assertEquals(60, jpegQualityPercent(0.6))
        assertEquals(50, jpegQualityPercent(0.5))
    }
}
