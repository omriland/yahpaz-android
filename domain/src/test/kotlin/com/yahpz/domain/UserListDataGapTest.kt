package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserListDataGapTest {
    @Test
    fun `only active volunteers and private-vehicle trainees get a mark`() {
        assertNull(userListDataGap("training", hasActiveVehicle = false, hasMapAddress = false))
        assertNull(userListDataGap(null, hasActiveVehicle = false, hasMapAddress = false))
        assertEquals(USER_GAP_BOTH, userListDataGap("active_volunteer", false, false)?.tip)
        assertEquals(USER_GAP_BOTH, userListDataGap("personal_vehicle_training", false, false)?.tip)
    }

    @Test
    fun `tip names what is missing`() {
        assertEquals(USER_GAP_NO_VEHICLE, userListDataGap("active_volunteer", false, true)?.tip)
        assertEquals(USER_GAP_NO_MAP_ADDRESS, userListDataGap("active_volunteer", true, false)?.tip)
        assertNull(userListDataGap("active_volunteer", true, true))
    }

    @Test
    fun `a map address needs a place id and finite coordinates`() {
        assertTrue(isRecognizedMapAddress("ChIJ", 32.1, 34.8))
        assertFalse(isRecognizedMapAddress(" ", 32.1, 34.8))
        assertFalse(isRecognizedMapAddress("ChIJ", null, 34.8))
        assertFalse(isRecognizedMapAddress("ChIJ", Double.NaN, 34.8))
    }
}
