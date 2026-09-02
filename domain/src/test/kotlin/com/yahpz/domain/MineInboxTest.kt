package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MineInboxTest {
    @Test
    fun openMineSummary() {
        assertEquals("אין אירועים שממתינים לתיעוד.", openMineSummary(0, true))
        assertEquals("יש לך אירוע אחד לתעד.", openMineSummary(1, true))
        assertEquals("יש לך שני אירועים לתעד.", openMineSummary(2, true))
        assertEquals("יש לך 3 אירועים לתעד.", openMineSummary(3, true))
        assertEquals("טוען את הדיווחים שלך…", openMineSummary(0, false))
    }

    @Test
    fun pendingTabLabelIncludesCountOnlyWhenThereIsWork() {
        assertEquals("ממתינים לתיעוד", minePendingTabLabel(0))
        assertEquals("ממתינים לתיעוד 3", minePendingTabLabel(3))
    }

    @Test
    fun loggedTabLabel() {
        assertEquals("תועדו", MINE_LOGGED_TAB_LABEL)
    }

    @Test
    fun pendingEmptyCopy() {
        assertEquals("אין אירועים שממתינים לתיעוד.", MINE_PENDING_EMPTY_TITLE)
        assertEquals("אירוע חדש יופיע כאן כשישויך אליך.", MINE_PENDING_EMPTY_CAPTION)
        assertEquals("לצפייה באירועים שתועדו", MINE_PENDING_EMPTY_VIEW_LOGGED)
    }

    @Test
    fun mineEventMatchesQuery() {
        val event = MineSearchFields("12-34", "כביש 1", "צומת גזר")
        assertTrue(mineEventMatchesQuery(event, "גזר"))
        assertTrue(mineEventMatchesQuery(event, "12"))
        assertFalse(mineEventMatchesQuery(event, "איילון"))
    }

    @Test
    fun partitionPutsOpenParticipationsInPending() {
        val sections = partitionMineList(
            listOf(
                MineListEvent("a", "2026-08-17", ParticipationStatus.PENDING),
                MineListEvent("b", "2026-08-16", ParticipationStatus.DONE),
                MineListEvent("c", "2026-08-10", ParticipationStatus.IN_PROGRESS),
            ),
            "2026-08-17",
            1,
        )
        assertEquals(listOf("a", "c"), sections.pending.map { it.id })
        assertEquals(listOf("b"), sections.logged.map { it.id })
    }

    @Test
    fun fillCtaLabels() {
        assertEquals("השלמת התיעוד שלי", mineFillCtaLabel(ParticipationStatus.PENDING))
        assertEquals("המשך התיעוד", mineFillCtaLabel(ParticipationStatus.IN_PROGRESS))
        assertNull(mineFillCtaLabel(ParticipationStatus.DONE))
    }

    @Test
    fun participationStampsForViewer() {
        assertEquals("הושלם", participationStamp(ParticipationStatus.DONE, true).label)
        assertEquals("טיוטה נשמרה", participationStamp(ParticipationStatus.IN_PROGRESS, true).label)
        assertEquals("ממתין לתיעוד", participationStamp(ParticipationStatus.PENDING, true).label)
    }

    @Test
    fun searchHighlightRangesMarksMatchingSubstring() {
        assertEquals(
            listOf(TextHighlightRange(5, 8)),
            searchHighlightRanges("צומת גזר", "גזר"),
        )
    }

    @Test
    fun searchHighlightRangesFindsEnglishKeyboardMappedHebrew() {
        val ranges = searchHighlightRanges("צומת גזר", "dzr")
        assertEquals(listOf(TextHighlightRange(5, 8)), ranges)
    }

    @Test
    fun searchHighlightRangesEmptyWhenQueryBlank() {
        assertTrue(searchHighlightRanges("צומת גזר", "   ").isEmpty())
    }
}
