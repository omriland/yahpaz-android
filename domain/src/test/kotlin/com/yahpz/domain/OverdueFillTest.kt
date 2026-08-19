package com.yahpz.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverdueFillTest {
    private val t0 = "2026-08-16T10:00:00.000Z"
    private val t0Ms = Instant.parse(t0).toEpochMilli()

    @Test
    fun overdueCardTip() {
        assertEquals("אירוע ממתין לתיעוד מעל ל־48 שעות", OVERDUE_FILL_CARD_TIP)
    }

    @Test
    fun notOverdueBefore48Hours() {
        assertFalse(
            isMineFillOverdue(
                isCancelled = false,
                participationStatus = ParticipationStatus.PENDING,
                fillCompletableAt = t0,
                nowMs = t0Ms + OVERDUE_48H_MS - 1,
            ),
        )
    }

    @Test
    fun overdueAt48HoursForOpenParticipation() {
        assertTrue(
            isMineFillOverdue(
                isCancelled = false,
                participationStatus = ParticipationStatus.IN_PROGRESS,
                fillCompletableAt = t0,
                nowMs = t0Ms + OVERDUE_48H_MS,
            ),
        )
    }

    @Test
    fun notOverdueWhenDoneCancelledOrNotCompletable() {
        val now = t0Ms + OVERDUE_48H_MS
        assertFalse(
            isMineFillOverdue(
                isCancelled = false,
                participationStatus = ParticipationStatus.DONE,
                fillCompletableAt = t0,
                nowMs = now,
            ),
        )
        assertFalse(
            isMineFillOverdue(
                isCancelled = true,
                participationStatus = ParticipationStatus.PENDING,
                fillCompletableAt = t0,
                nowMs = now,
            ),
        )
        assertFalse(
            isMineFillOverdue(
                isCancelled = false,
                participationStatus = ParticipationStatus.PENDING,
                fillCompletableAt = null,
                nowMs = now,
            ),
        )
    }
}
