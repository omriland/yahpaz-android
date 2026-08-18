package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FillValidationTest {
    private val plates = listOf("1234567")

    @Test
    fun draftDoesNotRequireTotalKmOrEnd() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(odometerStart = "100"),
            FillMode.DRAFT,
            plates,
            null,
        )
        assertNull(errors.odometerEnd)
    }

    @Test
    fun completeErrorsWhenTotalKmMissingWithoutShowingTheNumber() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = "טיפול",
            ),
            FillMode.COMPLETE,
            plates,
            null,
        )
        assertEquals("האחמ״ש טרם הזין קילומטרים לאירוע. לא ניתן לסיים את הדיווח.", errors.odometerEnd)
        assertFalse(errors.toString().contains("12"))
    }

    @Test
    fun completeRequiresUserEnteredEndWhenTotalKmIsSet() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "",
                route = "כביש 1",
                treatmentDetail = "טיפול",
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
        )
        assertEquals("יש למלא מד אוץ סיום.", errors.odometerEnd)
    }

    @Test
    fun completeAcceptsUserEndWhenTotalKmPresent() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "115",
                route = "כביש 1",
                treatmentDetail = "טיפול",
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
        )
        assertTrue(errors.isEmpty)
    }

    @Test
    fun completeErrorsOnLeftoverTreatedPlatePending() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = "טיפול",
                treatedPlatePending = "123",
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
        )
        assertEquals(TREATED_PLATE_LEFTOVER_ERROR, errors.treatedPlates)
    }

    @Test
    fun draftIgnoresLeftoverTreatedPlatePending() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(treatedPlatePending = "123"),
            FillMode.DRAFT,
            plates,
            null,
        )
        assertNull(errors.treatedPlates)
    }

    @Test
    fun completeAllowsZeroTreatedPlates() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = "טיפול",
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
        )
        assertNull(errors.treatedPlates)
        assertTrue(errors.isEmpty)
    }

    @Test
    fun endMustBeGreaterThanStart() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(odometerStart = "100", odometerEnd = "100"),
            FillMode.DRAFT,
            plates,
            null,
        )
        assertEquals("מד אוץ סיום חייב להיות גדול ממד אוץ התחלה", errors.odometerEnd)
    }

    @Test
    fun completeRequiresPlateFromRoster() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "9999999",
                odometerStart = "1",
                odometerEnd = "2",
                route = "כביש",
                treatmentDetail = "טיפול",
            ),
            FillMode.COMPLETE,
            plates,
            1.0,
        )
        assertEquals("יש לבחור רכב מהרשימה המקושרת למשתמש.", errors.vehiclePlate)
    }

    @Test
    fun deriveEventStatusKeepsDraftProgressAsInProgress() {
        assertEquals(
            EventStatus.IN_PROGRESS,
            deriveEventStatusAfterParticipation(
                listOf(ParticipationStatus.PENDING, ParticipationStatus.IN_PROGRESS),
            ),
        )
    }

    @Test
    fun deriveEventStatusUsesPartialWhenSomeoneCompleted() {
        assertEquals(
            EventStatus.PARTIAL,
            deriveEventStatusAfterParticipation(
                listOf(ParticipationStatus.DONE, ParticipationStatus.PENDING),
            ),
        )
    }

    @Test
    fun deriveEventStatusDoneWhenEveryParticipationIsDone() {
        assertEquals(
            EventStatus.DONE,
            deriveEventStatusAfterParticipation(
                listOf(ParticipationStatus.DONE, ParticipationStatus.DONE),
            ),
        )
    }
}
