package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors web `shiftBornEvents.test.ts` and `shiftBornFill.test.ts`. */
class ShiftBornTest {
    private fun snapshot(
        status: EventStatus = EventStatus.IN_PROGRESS,
        policeEventId: String? = null,
        treatmentDetail: String? = null,
        treatmentNotes: String? = null,
        roadId: String? = null,
        location: String? = null,
        treatedCount: Int = 0,
    ) = ShiftBornEventSnapshot(status, policeEventId, treatmentDetail, treatmentNotes, roadId, location, treatedCount)

    private val forty = "א".repeat(40)

    @Test
    fun emptyWhenEverySharedFieldIsBlank() {
        assertTrue(isShiftBornEventEmpty(snapshot()))
        assertTrue(isShiftBornEventEmpty(snapshot(policeEventId = "   ")))
    }

    @Test
    fun filledWhenAnySharedFieldHasContent() {
        assertFalse(isShiftBornEventEmpty(snapshot(policeEventId = "12")))
        assertFalse(isShiftBornEventEmpty(snapshot(treatmentDetail = "חילוץ")))
        assertFalse(isShiftBornEventEmpty(snapshot(treatmentNotes = "הערה")))
        assertFalse(isShiftBornEventEmpty(snapshot(treatedCount = 1)))
        assertFalse(isShiftBornEventEmpty(snapshot(roadId = "road-1")))
        assertFalse(isShiftBornEventEmpty(snapshot(location = "צומת")))
    }

    @Test
    fun doneIsCompletedOnlyWithTreatmentDetail() {
        assertEquals(
            StampDescriptor("הושלם", StampTone.DONE),
            shiftBornFillStamp(snapshot(status = EventStatus.DONE, policeEventId = "1", treatmentDetail = "חילוץ")),
        )
    }

    @Test
    fun missingTreatmentDetailStaysPendingNeverDone() {
        val pending = StampDescriptor("ממתין לתיעוד", StampTone.PENDING)
        assertEquals(pending, shiftBornFillStamp(snapshot(status = EventStatus.DONE, policeEventId = "1")))
        assertEquals(pending, shiftBornFillStamp(snapshot()))
    }

    @Test
    fun crewmateDraftShowsDraftSaved() {
        assertEquals(
            StampDescriptor("טיוטה נשמרה", StampTone.DRAFT),
            shiftBornFillStamp(snapshot(treatmentDetail = "טיפול")),
        )
    }

    @Test
    fun mineOpenUntilDoneWithTreatmentDetail() {
        assertTrue(mineShiftBornIsOpen(EventStatus.DONE, null))
        assertTrue(mineShiftBornIsOpen(EventStatus.DONE, "  "))
        assertFalse(mineShiftBornIsOpen(EventStatus.DONE, "חילוץ"))
        assertTrue(mineShiftBornIsOpen(EventStatus.IN_PROGRESS, "חילוץ"))
    }

    @Test
    fun futureShiftBornIsNotPendingUntilTheShiftDate() {
        assertFalse(mineShiftBornIsPending(EventStatus.IN_PROGRESS, null, "2026-11-04", "2026-11-03"))
        assertTrue(mineShiftBornIsPending(EventStatus.IN_PROGRESS, null, "2026-11-03", "2026-11-03"))
        assertTrue(mineShiftBornIsPending(EventStatus.IN_PROGRESS, null, "2026-11-02", "2026-11-03"))
        assertFalse(mineShiftBornIsPending(EventStatus.DONE, "חילוץ", "2026-11-02", "2026-11-03"))
    }

    @Test
    fun lastSavedByCopy() {
        assertEquals("נשמר ע״י עמרי", lastSavedByLabel("עמרי"))
        assertNull(lastSavedByLabel(null))
        assertNull(lastSavedByLabel("  "))
    }

    @Test
    fun lockedHebrewErrors() {
        assertEquals("מישהו שמר לפניך — רעננו", STALE_SAVE_MESSAGE)
        assertEquals("לא ניתן להקטין — קיימים אירועים שמולאו", COUNT_DECREASE_BLOCKED)
        assertEquals("ניתן לתעד החל מתאריך המשמרת.", SHIFT_BORN_TOO_EARLY)
    }

    @Test
    fun emptyRecordFailsAllThreeRequiredFields() {
        val errors = shiftBornCompleteErrors(ShiftBornFillDraft())
        assertEquals("יש לבחור כביש", errors.roadId)
        assertEquals("יש להזין מיקום", errors.location)
        assertEquals("יש להזין לפחות 40 תווים בפירוט הטיפול.", errors.treatmentDetail)
    }

    @Test
    fun treatmentDetailNeedsFortyTrimmedCharacters() {
        val base = ShiftBornFillDraft(roadId = "r1", location = "מחלף שורק")
        assertEquals(
            TREATMENT_DETAIL_MIN_ERROR,
            shiftBornCompleteErrors(base.copy(treatmentDetail = "א".repeat(39))).treatmentDetail,
        )
        assertNull(shiftBornCompleteErrors(base.copy(treatmentDetail = "  $forty  ")).treatmentDetail)
        assertNull(shiftBornCompleteErrors(base.copy(treatmentDetail = "א".repeat(80))).treatmentDetail)
    }

    @Test
    fun cancelledDropsTheMinimumButNotTheField() {
        val base = ShiftBornFillDraft(roadId = "r1", location = "מחלף שורק")
        assertNull(shiftBornCompleteErrors(base.copy(treatmentDetail = "בוטל"), isCancelled = true).treatmentDetail)
        assertEquals(
            "יש להזין פירוט טיפול",
            shiftBornCompleteErrors(base.copy(treatmentDetail = "   "), isCancelled = true).treatmentDetail,
        )
    }

    @Test
    fun completeRecordPassesWithoutEndTimeOrKm() {
        val errors = shiftBornCompleteErrors(
            ShiftBornFillDraft(roadId = "r1", location = "x", treatmentDetail = forty),
        )
        assertTrue(errors.isEmpty)
    }

    @Test
    fun whitespaceIsEmpty() {
        val errors = shiftBornCompleteErrors(
            ShiftBornFillDraft(roadId = "r1", location = "   ", treatmentDetail = "\n\t"),
        )
        assertTrue(errors.location != null)
        assertTrue(errors.treatmentDetail != null)
        assertNull(errors.roadId)
    }

    @Test
    fun messagesNameTheActionWithoutExclamation() {
        val errors = shiftBornCompleteErrors(ShiftBornFillDraft())
        listOfNotNull(errors.roadId, errors.location, errors.treatmentDetail).forEach { message ->
            assertFalse(message.contains("!"))
            assertTrue(message.startsWith("יש "))
        }
    }

    @Test
    fun crewReadOnlyBeforeTheShiftDateWithTheShiftDateNotice() {
        assertTrue(shiftBornFillReadOnly(eventDone = false, canManage = false, shiftDate = "2026-11-04", today = "2026-11-03"))
        assertEquals(
            SHIFT_BORN_TOO_EARLY,
            shiftBornReadOnlyNotice(eventDone = false, canManage = false, shiftDate = "2026-11-04", today = "2026-11-03"),
        )
        assertFalse(shiftBornFillReadOnly(eventDone = false, canManage = false, shiftDate = "2026-11-03", today = "2026-11-03"))
        assertNull(shiftBornReadOnlyNotice(eventDone = false, canManage = false, shiftDate = "2026-11-03", today = "2026-11-03"))
    }

    @Test
    fun leadEditsAnyDateAndAfterDone() {
        assertFalse(shiftBornFillReadOnly(eventDone = false, canManage = true, shiftDate = "2026-11-09", today = "2026-11-03"))
        assertFalse(shiftBornFillReadOnly(eventDone = true, canManage = true, shiftDate = "2026-11-01", today = "2026-11-03"))
    }

    @Test
    fun doneIsReadOnlyForCrew() {
        assertTrue(shiftBornFillReadOnly(eventDone = true, canManage = false, shiftDate = "2026-11-01", today = "2026-11-03"))
        assertEquals(
            SHIFT_BORN_DONE_LOCKED,
            shiftBornReadOnlyNotice(eventDone = true, canManage = false, shiftDate = "2026-11-01", today = "2026-11-03"),
        )
    }

    @Test
    fun treatedCounterClampsAndDropsZero() {
        val one = bumpShiftBornTreated(emptyList(), "k1", 1)
        assertEquals(listOf(ShiftBornTreated("k1", 1)), one)
        assertEquals(emptyList<ShiftBornTreated>(), bumpShiftBornTreated(one, "k1", -1))
        assertEquals(emptyList<ShiftBornTreated>(), bumpShiftBornTreated(emptyList(), "k1", -1))
        assertEquals(99, bumpShiftBornTreated(listOf(ShiftBornTreated("k1", 99)), "k1", 1).single().quantity)
    }

    @Test
    fun rpcHebrewMessagesPassThroughOthersAreGeneric() {
        assertEquals(STALE_SAVE_MESSAGE, mapShiftBornFillError("ERROR: מישהו שמר לפניך — רעננו (P0001)"))
        assertEquals(SHIFT_BORN_TOO_EARLY, mapShiftBornFillError("ניתן לתעד החל מתאריך המשמרת."))
        assertEquals(SHIFT_BORN_DONE_LOCKED, mapShiftBornFillError("x התיעוד כבר הושלם. רק אחמ״ש יכול לערוך. y"))
        assertEquals(SHIFT_BORN_NO_PERMISSION, mapShiftBornFillError("אין הרשאה"))
        assertEquals(SHIFT_BORN_SAVE_FAILED, mapShiftBornFillError("timeout"))
        assertEquals(SHIFT_BORN_SAVE_FAILED, mapShiftBornFillError(null))
    }

    @Test
    fun ctaFollowsTheSharedReportNotTheOwnRow() {
        assertEquals("השלמת התיעוד שלי", shiftBornFillCtaLabel(snapshot()))
        assertEquals("המשך התיעוד", shiftBornFillCtaLabel(snapshot(location = "צומת")))
        assertNull(shiftBornFillCtaLabel(snapshot(status = EventStatus.DONE, treatmentDetail = "חילוץ")))
        // Done without פירוט הטיפול is still open, so the way in stays.
        assertEquals("השלמת התיעוד שלי", shiftBornFillCtaLabel(snapshot(status = EventStatus.DONE)))
    }
}
