package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventVehiclePhotosTest {
    private fun media(vararg plateIds: String) = EventMedia(
        id = "m-${plateIds.joinToString()}",
        eventId = "e1",
        uploadedBy = "u1",
        uploaderName = "אחר",
        treatedPlateIds = plateIds.toList(),
        caption = null,
        takenWhen = EventMediaTakenWhen.DURING_AFTER_TREATMENT,
        storagePath = "p",
        mimeType = "image/jpeg",
        byteSize = 1,
        width = 1,
        height = 1,
        createdAt = "2026-09-10T00:00:00Z",
        signedUrl = null,
    )

    @Test
    fun `shared photo covers every attached plate`() {
        val plates = listOf(
            PhotoCoveragePlate("p1", "1234567"),
            PhotoCoveragePlate("p2", "7654321"),
        )
        val covered = coveredPlateDigits(listOf(media("p1", "p2")), plates)
        assertEquals(setOf("1234567", "7654321"), covered)
    }

    @Test
    fun `same plate number from another responder counts`() {
        val myPlates = listOf(TreatedPlate(plateNumber = "12-345-67", model = "טוסון", color = "לבן"))
        val covered = setOf("1234567")
        assertTrue(vehiclesMissingPhotos(myPlates, covered).isEmpty())
    }

    @Test
    fun `lists vehicles still missing a photo`() {
        val myPlates = listOf(
            TreatedPlate(plateNumber = "12-345-67", model = "טוסון", color = "לבן"),
            TreatedPlate(plateNumber = "76-543-21", model = "קורולה", color = "כסף"),
        )
        val missing = vehiclesMissingPhotos(myPlates, setOf("1234567"))
        assertEquals(listOf(myPlates[1]), missing)
        assertEquals(VEHICLE_PHOTOS_TITLE, "לא צורפו תמונות לרכבים הבאים:")
        assertEquals(VEHICLE_PHOTOS_PROCEED, "שמירה ללא תמונות נוספות")
    }
}
