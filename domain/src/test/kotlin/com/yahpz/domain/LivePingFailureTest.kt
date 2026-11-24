package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePingFailureTest {
    private val previous = LatLngAt(32.0, 34.8, 1_000L)
    private val attempted = LatLngAt(32.0, 34.8, 2_000L)

    @Test
    fun failedPingIsNotSharingAndShowsAnError() {
        val status = sharingStatusAfterPing(delivered = false)
        assertFalse(status.sharing)
        assertEquals(LIVE_PING_FAILED_TEXT, status.failed)
        assertEquals(LIVE_PING_FAILED_TEXT, status.statusText)
    }

    @Test
    fun deliveredPingIsSharingWithNoError() {
        val status = sharingStatusAfterPing(delivered = true)
        assertTrue(status.sharing)
        assertNull(status.failed)
    }

    @Test
    fun failedPingDoesNotAdvanceTheBaselineSoTheNextUpdateRetries() {
        val baseline = pingBaselineAfterAttempt(previous, attempted, delivered = false)
        assertEquals(previous, baseline)
    }

    @Test
    fun updateTenSecondsAfterTheKeptBaselineIsEmitted() {
        val baseline = pingBaselineAfterAttempt(previous, attempted, delivered = false)
        val nextUpdate = LatLngAt(32.0, 34.8, previous.atMs + LIVE_PING_MIN_INTERVAL_MS)
        assertTrue(shouldEmitPing(baseline, nextUpdate))
    }

    @Test
    fun failedFirstPingKeepsNoBaselineSoItIsRetried() {
        val baseline = pingBaselineAfterAttempt(null, attempted, delivered = false)
        assertNull(baseline)
        assertTrue(shouldEmitPing(baseline, attempted))
    }

    @Test
    fun deliveredPingAdvancesTheBaseline() {
        assertEquals(attempted, pingBaselineAfterAttempt(previous, attempted, delivered = true))
    }
}
