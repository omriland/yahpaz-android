package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors web eventSaveRules.test.ts vehiclePhotosIssue / skippedPhotoDigits. */
class VehiclePhotosPromptTest {
    private val tucson = TreatedPlate(plateNumber = "12-345-67", model = "טוסון", color = "לבן")
    private val corolla = TreatedPlate(plateNumber = "76-543-21", model = "קורולה", color = "כסף")

    @Test
    fun `names the vehicles without a photo`() {
        assertEquals(listOf(corolla), vehiclePhotosPrompt(listOf(tucson, corolla), setOf("1234567")))
    }

    @Test
    fun `never prompts when the coverage read failed`() {
        assertTrue(vehiclePhotosPrompt(listOf(tucson, corolla), covered = null).isEmpty())
    }

    @Test
    fun `no prompt when every vehicle has a photo`() {
        assertTrue(vehiclePhotosPrompt(listOf(tucson, corolla), setOf("1234567", "7654321")).isEmpty())
    }

    @Test
    fun `save anyway is remembered per vehicle, so a vehicle added later is still asked about`() {
        val first = vehiclePhotosPrompt(listOf(tucson), emptySet())
        assertEquals(listOf(tucson), first)
        val skipped = skippedPhotoDigits(first).toSet()
        assertEquals(setOf("1234567"), skipped)
        assertTrue(vehiclePhotosPrompt(listOf(tucson), emptySet(), skipped).isEmpty())
        assertEquals(listOf(corolla), vehiclePhotosPrompt(listOf(tucson, corolla), emptySet(), skipped))
    }

    @Test
    fun `prompt copy matches the web`() {
        assertEquals("לא צורפו תמונות לרכבים הבאים:", VEHICLE_PHOTOS_TITLE)
        assertEquals("שמירה ללא תמונות נוספות", VEHICLE_PHOTOS_PROCEED)
        assertEquals("חזרה לעריכה", VEHICLE_PHOTOS_BACK)
    }
}
