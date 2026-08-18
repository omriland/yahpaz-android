package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun `force when current is below min`() {
        assertTrue(needsForceUpdate(1, 2))
        assertTrue(needsForceUpdate(2, 10))
    }

    @Test
    fun `allow when current meets or exceeds min`() {
        assertFalse(needsForceUpdate(2, 2))
        assertFalse(needsForceUpdate(3, 2))
    }
}
