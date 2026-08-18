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
        assertEquals("יש לבחור תאריך עתידי", error.message)
    }

    @Test
    fun returnDateTypingFillsDayThenMonthThenYear() {
        assertEquals("30", formatReturnDateInput("30"))
        assertEquals("30/12", formatReturnDateInput("3012"))
        assertEquals("30/12/2026", formatReturnDateInput("30122026"))
        assertEquals("30/12/2026", formatReturnDateInput("30/12/2026"))
    }

    @Test
    fun storedIsoReturnDateShowsAsDayMonthYear() {
        assertEquals("18/08/2026", returnDateToInput("2026-08-18"))
        assertEquals("", returnDateToInput(""))
    }

    @Test
    fun typedReturnDateParsesToIso() {
        assertEquals("2026-12-30", parseReturnDateInput("30122026"))
        assertEquals("2026-12-30", parseReturnDateInput("30/12/2026"))
        assertNull(parseReturnDateInput("32/13/2026"))
        assertNull(parseReturnDateInput("3012"))
    }

    @Test
    fun unavailableWriteAcceptsTypedDayMonthYear() {
        val write = buildAvailabilityWrite(AvailabilityStatus.UNAVAILABLE, "30/12/2026", "2026-08-17")
        val ok = write as AvailabilityWrite.Ok
        assertEquals("2026-12-30", ok.availableFrom)
    }

    @Test
    fun returnDateKeystrokeTypesAndDeletesDigitsInOrder() {
        var value = ""
        "30122026".forEach { digit ->
            value = applyReturnDateKeystroke(value, value + digit)
        }
        assertEquals("30/12/2026", value)
        value = applyReturnDateKeystroke(value, "30/12/202")
        assertEquals("30/12/202", value)
        value = applyReturnDateKeystroke("30/12", "3012")
        assertEquals("30/1", value)
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
    fun findDuplicatePlateReturnsTheRepeatedDigits() {
        assertEquals("1234567", findDuplicatePlate(listOf("12-345-67", "1234567")))
        assertNull(findDuplicatePlate(listOf("1111111", "2222222")))
        assertNull(findDuplicatePlate(listOf("", "abc")))
    }

    @Test
    fun availabilitySearchLabelFollowsEffectiveStatus() {
        assertEquals("זמין", availabilitySearchLabel(AvailabilityStatus.AVAILABLE, null, "2026-08-17"))
        assertEquals("לא זמין", availabilitySearchLabel(AvailabilityStatus.UNAVAILABLE, null, "2026-08-17"))
        assertEquals("לא זמין", availabilitySearchLabel(AvailabilityStatus.UNAVAILABLE, "2026-08-20", "2026-08-17"))
        assertEquals("זמין", availabilitySearchLabel(AvailabilityStatus.UNAVAILABLE, "2026-08-17", "2026-08-17"))
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
