package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillDraftSurvivalTest {
    private val now = 1_788_000_000_000L
    private val server = ResponderFillDraft(treatmentDetail = "מהשרת")
    private val typed = ResponderFillDraft(treatmentDetail = "חילוץ מכביש 6")

    @Test
    fun stashKeysAreScopedPerFlow() {
        assertEquals("yahpaz.fillDraft.responder.a1", fillDraftKey("responder", "a1"))
        assertTrue(fillDraftKey("responder", "a1") != fillDraftKey("shiftBorn", "a1"))
    }

    @Test
    fun keepLiveBootOnlyWhenReadyWithTypedDraft() {
        assertTrue(shouldKeepLiveFormBoot(loadState = "ready", hasTypedDraft = true))
        assertFalse(shouldKeepLiveFormBoot(loadState = "loading", hasTypedDraft = true))
        assertFalse(shouldKeepLiveFormBoot(loadState = "denied", hasTypedDraft = true))
        assertFalse(shouldKeepLiveFormBoot(loadState = "ready", hasTypedDraft = false))
    }

    @Test
    fun preferStashWhenItDiffersAndIsFresh() {
        assertTrue(
            shouldPreferStashedFillDraft(
                stashed = typed,
                savedAt = now,
                server = server,
                now = now,
            ),
        )
        assertFalse(
            shouldPreferStashedFillDraft(
                stashed = server,
                savedAt = now,
                server = server,
                now = now,
            ),
        )
    }

    @Test
    fun ignoreMissingOrStaleStash() {
        assertFalse(
            shouldPreferStashedFillDraft(
                stashed = null,
                savedAt = now,
                server = server,
                now = now,
            ),
        )
        assertFalse(
            shouldPreferStashedFillDraft(
                stashed = typed,
                savedAt = now - FILL_DRAFT_MAX_AGE_MS - 1,
                server = server,
                now = now,
            ),
        )
        assertTrue(
            shouldPreferStashedFillDraft(
                stashed = typed,
                savedAt = now - FILL_DRAFT_MAX_AGE_MS + 1,
                server = server,
                now = now,
            ),
        )
    }

    @Test
    fun backDropsUnfinishedPhotoBeforeLeaving() {
        assertEquals(
            FillBackAction.DROP_UNFINISHED_PHOTO,
            decideFillBack(onMediaPane = true, unfinishedMediaDraftCount = 1),
        )
        assertEquals(
            FillBackAction.DROP_UNFINISHED_PHOTO,
            decideFillBack(onMediaPane = false, unfinishedMediaDraftCount = 2),
        )
    }

    @Test
    fun backFromMediaWithoutUnfinishedPhotoReturnsToDocs() {
        assertEquals(
            FillBackAction.SHOW_DOCS,
            decideFillBack(onMediaPane = true, unfinishedMediaDraftCount = 0),
        )
    }

    @Test
    fun backFromDocsLeavesAfterPersist() {
        assertEquals(
            FillBackAction.LEAVE,
            decideFillBack(onMediaPane = false, unfinishedMediaDraftCount = 0),
        )
    }

    @Test
    fun savedLabelIs24HourClock() {
        val noonJerusalem = java.time.Instant.parse("2026-05-01T09:00:00Z").toEpochMilli()
        assertEquals("12:00", fillDraftSavedLabel(noonJerusalem))
    }
}
