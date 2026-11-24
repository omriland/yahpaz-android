package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillAutosaveChangeTest {
    private val opened = ResponderFillDraft(route = "כביש 1", treatmentDetail = "מהשרת")

    @Test
    fun openingAReportWithNoChangeDoesNotAutosave() {
        assertFalse(shouldAutosaveFill(draft = opened, lastSavedOrLoaded = opened))
    }

    @Test
    fun anEditedFieldAutosaves() {
        assertTrue(shouldAutosaveFill(draft = opened.copy(route = "כביש 6"), lastSavedOrLoaded = opened))
    }

    @Test
    fun anAddedTreatedPlateAutosaves() {
        val withPlate = opened.copy(treatedPlatePending = "1234567")
        assertTrue(shouldAutosaveFill(draft = withPlate, lastSavedOrLoaded = opened))
    }

    @Test
    fun anEditAlreadySavedDoesNotAutosaveAgain() {
        val edited = opened.copy(route = "כביש 6")
        assertFalse(shouldAutosaveFill(draft = edited, lastSavedOrLoaded = edited))
    }
}
