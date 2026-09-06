package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusTest {
    @Test
    fun `done with missing KM is חסר ק״מ for lead reporting`() {
        assertEquals(
            StampDescriptor(MISSING_KM_STAMP_LABEL, StampTone.ALERT),
            reportingDocumentationStamp(EventStatus.DONE, missingKm = true),
        )
    }

    @Test
    fun `done with KM filled stays הושלם`() {
        assertEquals(
            eventStamp(EventStatus.DONE),
            reportingDocumentationStamp(EventStatus.DONE, missingKm = false),
        )
    }

    @Test
    fun `partial with missing KM is not overridden`() {
        assertEquals(
            eventStamp(EventStatus.PARTIAL),
            reportingDocumentationStamp(EventStatus.PARTIAL, missingKm = true),
        )
    }

    @Test
    fun `overlays only a green הושלם stamp`() {
        assertEquals(
            StampDescriptor(MISSING_KM_STAMP_LABEL, StampTone.ALERT),
            overlayMissingKmOnDoneStamp(StampDescriptor("הושלם", StampTone.DONE), true),
        )
        assertEquals(
            StampDescriptor("טיוטה נשמרה", StampTone.DRAFT),
            overlayMissingKmOnDoneStamp(StampDescriptor("טיוטה נשמרה", StampTone.DRAFT), true),
        )
    }

    @Test
    fun `completed fill with no lead KM shows סיימת לתעד and notes the lead`() {
        assertEquals(LEAD_KM_PENDING_NOTE, leadKmPendingNote(ParticipationStatus.DONE, null))
        assertEquals(null, leadKmPendingNote(ParticipationStatus.DONE, 0.0))
        assertEquals(null, leadKmPendingNote(ParticipationStatus.IN_PROGRESS, null))
        assertEquals(true, mineInboxIsOpen(ParticipationStatus.DONE, null))
        assertEquals(false, mineInboxIsOpen(ParticipationStatus.DONE, 12.0))
        assertEquals(
            StampDescriptor(FILL_DONE_AWAITING_KM_LABEL, StampTone.DONE),
            mineParticipationStamp(ParticipationStatus.DONE, null),
        )
        assertEquals(
            StampDescriptor("הושלם", StampTone.DONE),
            mineParticipationStamp(ParticipationStatus.DONE, 12.0),
        )
    }
}
