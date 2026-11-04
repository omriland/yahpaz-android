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
    fun completeAcceptsOdometersWhenLeadTotalKmMissing() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = FULL_DETAIL,
            ),
            FillMode.COMPLETE,
            plates,
            null,
        )
        assertTrue(errors.isEmpty)
    }

    @Test
    fun completeRequiresUserEnteredEndWhenTotalKmIsSet() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "",
                route = "כביש 1",
                treatmentDetail = FULL_DETAIL,
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
                treatmentDetail = FULL_DETAIL,
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
                treatmentDetail = FULL_DETAIL,
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
    fun completeErrorsOnUnfinishedMediaDrafts() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = FULL_DETAIL,
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
            unfinishedMediaDraftCount = 1,
        )
        assertEquals(EVENT_MEDIA_LEFTOVER_ERROR, errors.eventMedia)
    }

    @Test
    fun draftIgnoresUnfinishedMediaDrafts() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(),
            FillMode.DRAFT,
            plates,
            null,
            unfinishedMediaDraftCount = 2,
        )
        assertNull(errors.eventMedia)
    }

    @Test
    fun completeAllowsZeroTreatedPlates() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "1234567",
                odometerStart = "100",
                odometerEnd = "112",
                route = "כביש 1",
                treatmentDetail = FULL_DETAIL,
            ),
            FillMode.COMPLETE,
            plates,
            12.0,
        )
        assertNull(errors.treatedPlates)
        assertTrue(errors.isEmpty)
    }

    @Test
    fun equalOdometersAreAllowedIncludingZeroInBoth() {
        assertNull(
            validateResponderFillDraft(
                ResponderFillDraft(odometerStart = "100", odometerEnd = "100"),
                FillMode.DRAFT,
                plates,
                null,
            ).odometerEnd,
        )
        assertNull(
            validateResponderFillDraft(
                ResponderFillDraft(odometerStart = "0", odometerEnd = "0"),
                FillMode.DRAFT,
                plates,
                null,
            ).odometerEnd,
        )
    }

    @Test
    fun endMustNotBeSmallerThanStart() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(odometerStart = "100", odometerEnd = "99"),
            FillMode.DRAFT,
            plates,
            null,
        )
        assertEquals(ODOMETER_ORDER_ERROR, errors.odometerEnd)
    }

    @Test
    fun completeRequiresPlateFromRoster() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(
                vehiclePlate = "9999999",
                odometerStart = "1",
                odometerEnd = "2",
                route = "כביש",
                treatmentDetail = FULL_DETAIL,
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

    @Test
    fun completeOnAlreadyDoneAssignmentIsSuccess() {
        assertEquals(
            FillWriteGate.ALREADY_COMPLETE,
            gateResponderFillWrite(
                complete = true,
                participationStatus = ParticipationStatus.DONE,
                eventStatus = EventStatus.IN_PROGRESS,
            ),
        )
    }

    @Test
    fun draftOnAlreadyDoneAssignmentIsLocked() {
        assertEquals(
            FillWriteGate.LOCKED,
            gateResponderFillWrite(
                complete = false,
                participationStatus = ParticipationStatus.DONE,
                eventStatus = EventStatus.IN_PROGRESS,
            ),
        )
    }

    @Test
    fun writeOnDoneEventIsLocked() {
        assertEquals(
            FillWriteGate.LOCKED,
            gateResponderFillWrite(
                complete = true,
                participationStatus = ParticipationStatus.IN_PROGRESS,
                eventStatus = EventStatus.DONE,
            ),
        )
    }

    @Test
    fun cancelledFillKeepsTheMarkAndStillProceeds() {
        assertEquals("האירוע בוטל.", FILL_CANCELLED_NOTE)
        assertEquals(
            FillWriteGate.PROCEED,
            gateResponderFillWrite(
                complete = true,
                participationStatus = ParticipationStatus.IN_PROGRESS,
                eventStatus = EventStatus.IN_PROGRESS,
            ),
        )
        assertEquals(
            FillWriteGate.PROCEED,
            gateResponderFillWrite(
                complete = false,
                participationStatus = ParticipationStatus.IN_PROGRESS,
                eventStatus = EventStatus.IN_PROGRESS,
            ),
        )
    }

    @Test
    fun inProgressCompleteMayProceed() {
        assertEquals(
            FillWriteGate.PROCEED,
            gateResponderFillWrite(
                complete = true,
                participationStatus = ParticipationStatus.IN_PROGRESS,
                eventStatus = EventStatus.IN_PROGRESS,
            ),
        )
    }

    @Test
    fun `complete needs 25 trimmed characters of treatment detail`() {
        assertEquals(TREATMENT_DETAIL_MIN_ERROR, treatmentDetailError("טיפול", FillMode.COMPLETE))
        assertEquals(TREATMENT_DETAIL_MIN_ERROR, treatmentDetailError("   " + "א".repeat(24) + "   ", FillMode.COMPLETE))
        assertNull(treatmentDetailError("א".repeat(25), FillMode.COMPLETE))
        assertNull(treatmentDetailError("", FillMode.DRAFT))
    }

    @Test
    fun `treatment detail counter shows trimmed count against 25`() {
        assertEquals(TreatmentDetailCounter("0 מתוך 25", met = false), treatmentDetailCounter("  "))
        assertEquals(TreatmentDetailCounter("25 מתוך 25", met = true), treatmentDetailCounter(" " + "ב".repeat(25)))
    }

    @Test
    fun `odometer total is end minus start once both are numbers and in order`() {
        assertEquals(42.0, odometerDelta("1000", "1042")!!, 0.0)
        assertEquals(0.0, odometerDelta("5", "5")!!, 0.0)
        assertNull(odometerDelta("1000", ""))
        assertNull(odometerDelta("abc", "10"))
        assertNull(odometerDelta("1042", "1000"))
    }

    @Test
    fun `odometer range error only for a reversed pair`() {
        assertEquals(ODOMETER_ORDER_ERROR, odometerRangeError("1042", "1000"))
        assertNull(odometerRangeError("1000", "1042"))
        assertNull(odometerRangeError("", "1000"))
    }

    private companion object {
        val FULL_DETAIL = "טיפול ברכב תקוע בנתיב הימני, הוצב משולש וכוון התנועה עד לגרירה."
    }
}
