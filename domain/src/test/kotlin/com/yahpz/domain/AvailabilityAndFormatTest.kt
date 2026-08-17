package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailabilityAndFormatTest {
    @Test
    fun availableWriteClearsReturnDate() {
        val write = buildAvailabilityWrite(AvailabilityStatus.AVAILABLE, "2026-09-01", "2026-08-17")
        val ok = write as AvailabilityWrite.Ok
        assertEquals(AvailabilityStatus.AVAILABLE, ok.availability)
        assertNull(ok.availableFrom)
    }

    @Test
    fun unavailableWithFutureDate() {
        val write = buildAvailabilityWrite(AvailabilityStatus.UNAVAILABLE, "2026-08-18", "2026-08-17")
        val ok = write as AvailabilityWrite.Ok
        assertEquals(AvailabilityStatus.UNAVAILABLE, ok.availability)
        assertEquals("2026-08-18", ok.availableFrom)
    }

    @Test
    fun unavailableWithTodayOrPastIsRejected() {
        val write = buildAvailabilityWrite(AvailabilityStatus.UNAVAILABLE, "2026-08-17", "2026-08-17")
        val error = write as AvailabilityWrite.Error
        assertEquals("בחרו תאריך מהמחר או השאירו ריק.", error.message)
    }

    @Test
    fun effectiveAvailabilityReturnsWhenDateArrives() {
        assertEquals(
            AvailabilityStatus.AVAILABLE,
            effectiveAvailability(AvailabilityStatus.UNAVAILABLE, "2026-08-17", "2026-08-17"),
        )
    }

    @Test
    fun formatPlateSevenAndEightDigits() {
        assertEquals("12-345-67", formatPlate("1234567"))
        assertEquals("123-45-678", formatPlate("12345678"))
        assertEquals("1234567", plateDigits("12-345-67"))
    }

    @Test
    fun passwordStrengthRequiresLengthUppercaseAndSymbol() {
        assertTrue(passwordStrengthError("short") != null)
        assertTrue(passwordStrengthError("longenough1") != null)
        assertNull(passwordStrengthError("Longenough!"))
    }

    @Test
    fun shouldEmitPingOnFirstFixOrMoveOrInterval() {
        assertTrue(shouldEmitPing(null, LatLngAt(32.0, 34.8, 1_000)))
        val last = LatLngAt(32.0, 34.8, 1_000)
        assertFalse(shouldEmitPing(last, LatLngAt(32.00001, 34.8, 2_000)))
        assertTrue(shouldEmitPing(last, LatLngAt(32.0, 34.8, 12_000)))
    }

    @Test
    fun parseTrackTokenFromYahpazUrl() {
        assertEquals("abc", parseTrackToken("https://yahpz.com/?track_token=abc"))
        assertEquals("xyz", parseTrackToken("yahpaz://track?token=xyz"))
        assertNull(parseTrackToken("https://yahpz.com/events"))
    }
}
