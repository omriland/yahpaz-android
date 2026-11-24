package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationPermissionErrorTest {
    @Test
    fun permissionTextIsTheExistingMessage() {
        assertEquals("יש לאשר מיקום בהגדרות כדי לשתף מיקום בזמן אירוע.", LOCATION_PERMISSION_REQUIRED_TEXT)
    }

    @Test
    fun grantClearsTheStalePermissionError() {
        assertNull(errorAfterPermissionResult(LOCATION_PERMISSION_REQUIRED_TEXT, granted = true))
    }

    @Test
    fun grantKeepsAnUnrelatedError() {
        assertEquals("קישור המעקב אינו תקף.", errorAfterPermissionResult("קישור המעקב אינו תקף.", granted = true))
    }

    @Test
    fun denialShowsThePermissionError() {
        assertEquals(LOCATION_PERMISSION_REQUIRED_TEXT, errorAfterPermissionResult(null, granted = false))
    }
}
