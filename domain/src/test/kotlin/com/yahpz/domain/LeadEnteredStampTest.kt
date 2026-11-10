package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Mirrors web eventIncomplete.test.ts "lead-facing הוזן בהצלחה" and status.test.ts leadEnteredStamp. */
class LeadEnteredStampTest {
    private fun responder(
        id: String = "u1",
        status: ParticipationStatus = ParticipationStatus.PENDING,
        totalKm: Double? = 12.0,
    ) = IncompleteResponderSnapshot(totalKm = totalKm, responderId = id, status = status)

    private fun complete(
        status: EventStatus = EventStatus.IN_PROGRESS,
        endedAt: String? = "2026-09-04T07:00:00+03:00",
        policeEventId: String? = "12345",
        responders: List<IncompleteResponderSnapshot> = listOf(responder()),
        isCancelled: Boolean = false,
        origin: String? = "manual",
    ) = IncompleteEventSnapshot(
        origin = origin,
        policeEventId = policeEventId,
        patrolCallsign = "ניידת 1",
        startedAt = "2026-09-04T06:00:00+03:00",
        endedAt = endedAt,
        hasDistrict = true,
        hasEventType = true,
        hasRoad = true,
        location = "מחלף אייל",
        responders = responders,
        status = status,
        isCancelled = isCancelled,
        shiftLeadId = "lead",
        secondaryLeadIds = listOf("sec"),
    )

    @Test
    fun `tells the lead their part is saved and one volunteer still owes a fill`() {
        assertEquals(
            StampDescriptor("הוזן בהצלחה", StampTone.DONE, "ממתין למתנדב"),
            leadEnteredAwaitingStamp(complete(), "lead"),
        )
        assertEquals("הוזן בהצלחה", leadEnteredAwaitingStamp(complete(), "sec")?.label)
    }

    @Test
    fun `counts open volunteers, including a saved draft, and skips finished ones`() {
        val several = complete(
            status = EventStatus.PARTIAL,
            responders = listOf(
                responder("u1", ParticipationStatus.PENDING),
                responder("u2", ParticipationStatus.IN_PROGRESS),
                responder("u3", ParticipationStatus.DONE),
            ),
        )
        assertEquals("ממתין ל־2 מתנדבים", leadEnteredAwaitingStamp(several, "lead")?.caption)
    }

    @Test
    fun `stays off while the lead still owes a field`() {
        assertNull(leadEnteredAwaitingStamp(complete(endedAt = null), "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(policeEventId = null), "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(responders = listOf(responder(totalKm = null))), "lead"))
    }

    @Test
    fun `is viewer-relative - volunteers and admins who do not lead keep the old stamp`() {
        assertNull(leadEnteredAwaitingStamp(complete(), "u1"))
        assertNull(leadEnteredAwaitingStamp(complete(), "admin-who-is-not-the-lead"))
        assertNull(leadEnteredAwaitingStamp(complete(), null))
    }

    @Test
    fun `lets a lead who is also an open volunteer see their own to-do instead`() {
        val leadOnCrew = complete(responders = listOf(responder("lead"), responder("u2")))
        assertNull(leadEnteredAwaitingStamp(leadOnCrew, "lead"))
    }

    @Test
    fun `does not apply once every volunteer is done, to drafts, cancelled or shift-born events`() {
        val allDone = complete(
            status = EventStatus.PARTIAL,
            responders = listOf(responder(status = ParticipationStatus.DONE)),
        )
        assertNull(leadEnteredAwaitingStamp(allDone, "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(status = EventStatus.DONE), "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(status = EventStatus.DRAFT), "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(isCancelled = true), "lead"))
        assertNull(leadEnteredAwaitingStamp(complete(origin = SHIFT_ORIGIN), "lead"))
    }

    @Test
    fun `stamp is green with a singular or counted caption and no em dash`() {
        assertEquals(StampDescriptor("הוזן בהצלחה", StampTone.DONE, "ממתין למתנדב"), leadEnteredStamp(1))
        assertEquals("ממתין ל־3 מתנדבים", leadEnteredStamp(3).caption)
        for (count in listOf(1, 2)) {
            val stamp = leadEnteredStamp(count)
            assertFalse("${stamp.label}${stamp.caption}".contains('—'))
        }
    }
}
