package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnlinkedPhotosNoteTest {
    @Test
    fun oneUnlinkedPhoto() {
        assertEquals(
            "יש לך תמונה אחת ללא שיוך לרכב. כדי לשייך: חזרה לעריכה, פתיחת התמונה ובחירת הרכב.",
            unlinkedPhotosNote(1),
        )
    }

    @Test
    fun noneMeansNoNote() {
        assertNull(unlinkedPhotosNote(0))
    }

    @Test
    fun countsOnlyTheViewersPhotosWithNoVehicle() {
        val photos = listOf(
            Triple("me", false, 0),
            Triple("me", false, 1),
            Triple("me", true, 0),
            Triple("someone", false, 0),
        )
        assertEquals(1, unlinkedOwnPhotoCount(photos, "me"))
        assertEquals(0, unlinkedOwnPhotoCount(photos, null))
    }
}
