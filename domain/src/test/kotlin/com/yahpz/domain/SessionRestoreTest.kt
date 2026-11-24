package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRestoreTest {
    @Test
    fun networkFailureKeepsTheUserSignedInWithRetry() {
        val outcome = sessionRestoreOutcome(hasSession = true, failure = SessionLoadFailure.NETWORK)
        assertEquals(SessionRestoreOutcome.KEEP_SIGNED_IN_OFFLINE, outcome)
    }

    @Test
    fun authFailureShowsLogin() {
        val outcome = sessionRestoreOutcome(hasSession = true, failure = SessionLoadFailure.AUTH)
        assertEquals(SessionRestoreOutcome.SIGNED_OUT, outcome)
    }

    @Test
    fun noStoredSessionShowsLogin() {
        assertEquals(SessionRestoreOutcome.SIGNED_OUT, sessionRestoreOutcome(hasSession = false, failure = null))
    }

    @Test
    fun cleanRestoreSignsIn() {
        assertEquals(SessionRestoreOutcome.SIGNED_IN, sessionRestoreOutcome(hasSession = true, failure = null))
    }

    @Test
    fun bannerTextAndRetryLabelAreHebrew() {
        assertEquals("אין חיבור. נסו שוב", OFFLINE_RETRY_BANNER)
        assertEquals("נסו שוב", OFFLINE_RETRY_ACTION)
    }
}
