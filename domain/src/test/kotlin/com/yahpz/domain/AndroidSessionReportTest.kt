package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSessionReportTest {
    @Test
    fun `rpc params use version code and name`() {
        val params = androidSessionRpcParams(17, " 0.3.6 ")
        assertEquals(17, params.versionCode)
        assertEquals("0.3.6", params.versionName)
    }

    @Test
    fun `reports immediately when never succeeded`() {
        assertTrue(shouldReportAndroidSession(lastSuccessAtMs = null, nowMs = 1_000L))
    }

    @Test
    fun `throttles inside fifteen minutes`() {
        val first = 0L
        val fourteenMin = 14 * 60 * 1000L
        assertFalse(shouldReportAndroidSession(lastSuccessAtMs = first, nowMs = fourteenMin))
        assertTrue(shouldReportAndroidSession(lastSuccessAtMs = first, nowMs = 15 * 60 * 1000L))
    }
}
