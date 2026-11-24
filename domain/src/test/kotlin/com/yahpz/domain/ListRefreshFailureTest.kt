package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListRefreshFailureTest {
    @Test
    fun failedRefreshOverExistingRowsKeepsThemAndToasts() {
        val outcome = listRefreshFailure(hadRows = true, toastText = UNIT_EVENTS_REFRESH_FAILED_TOAST)
        assertTrue(outcome.keepRows)
        assertFalse(outcome.showEmptyFailure)
        assertEquals(UNIT_EVENTS_REFRESH_FAILED_TOAST, outcome.toast)
    }

    @Test
    fun failedFirstLoadShowsTheEmptyFailureWithoutToast() {
        val outcome = listRefreshFailure(hadRows = false, toastText = UNIT_EVENTS_REFRESH_FAILED_TOAST)
        assertTrue(outcome.showEmptyFailure)
        assertNull(outcome.toast)
    }
}
