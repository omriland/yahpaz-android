package com.yahpz.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillSaveGateTest {
    @Test
    fun secondTapOnFinishWhileSavingIsRefused() {
        val gate = FillSaveGate()
        assertTrue(gate.tryBegin(FillSaveKind.EXPLICIT))
        assertFalse(gate.tryBegin(FillSaveKind.EXPLICIT))
    }

    @Test
    fun autosaveIsRefusedWhileAnExplicitSaveRuns() {
        val gate = FillSaveGate()
        assertTrue(gate.tryBegin(FillSaveKind.EXPLICIT))
        assertFalse(gate.tryBegin(FillSaveKind.AUTOSAVE))
    }

    @Test
    fun explicitSaveIsRefusedWhileAnAutosaveRuns() {
        val gate = FillSaveGate()
        assertTrue(gate.tryBegin(FillSaveKind.AUTOSAVE))
        assertFalse(gate.tryBegin(FillSaveKind.EXPLICIT))
    }

    @Test
    fun finishingASaveFreesTheGate() {
        val gate = FillSaveGate()
        gate.tryBegin(FillSaveKind.EXPLICIT)
        gate.finish()
        assertTrue(gate.tryBegin(FillSaveKind.AUTOSAVE))
    }

    @Test
    fun isSavingReflectsTheRunningSave() {
        val gate = FillSaveGate()
        assertFalse(gate.isSaving)
        gate.tryBegin(FillSaveKind.EXPLICIT)
        assertTrue(gate.isSaving)
        gate.finish()
        assertFalse(gate.isSaving)
    }
}
