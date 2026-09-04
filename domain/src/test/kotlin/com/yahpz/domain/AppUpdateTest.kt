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

    @Test
    fun `optional when current meets min but is behind latest`() {
        assertTrue(needsOptionalUpdate(24, 24, 26))
        assertTrue(needsOptionalUpdate(25, 24, 26))
    }

    @Test
    fun `not optional when current is at latest or below min`() {
        assertFalse(needsOptionalUpdate(26, 24, 26))
        assertFalse(needsOptionalUpdate(27, 24, 26))
        assertFalse(needsOptionalUpdate(23, 24, 26))
    }
}
