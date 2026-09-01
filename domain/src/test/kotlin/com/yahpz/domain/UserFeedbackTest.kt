package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFeedbackTest {
    @Test
    fun requiresKindAndContent() {
        assertEquals(
            FEEDBACK_KIND_ERROR,
            feedbackSubmitError(null, "יש באג", false),
        )
        assertEquals(
            FEEDBACK_EMPTY_ERROR,
            feedbackSubmitError("bug", "   ", false),
        )
        assertNull(feedbackSubmitError("bug", "מסך קפוא", false))
        assertNull(feedbackSubmitError("suggestion", "", true))
    }

    @Test
    fun rejectsLongBody() {
        assertNull(feedbackBodyError("א".repeat(FEEDBACK_BODY_MAX)))
        assertEquals(FEEDBACK_BODY_ERROR, feedbackBodyError("א".repeat(FEEDBACK_BODY_MAX + 1)))
    }

    @Test
    fun mapsAudioMimeAndPath() {
        assertEquals("m4a", feedbackStorageExt("audio/mp4"))
        assertEquals("audio/mp4", normalizeFeedbackAudioMime("audio/mp4"))
        assertEquals("u1/f1.m4a", feedbackStoragePath("u1", "f1", "audio/mp4"))
    }

    @Test
    fun formatsTimerAndCapsAtNinety() {
        assertEquals("00:00", formatRecordSeconds(0))
        assertEquals("00:09", formatRecordSeconds(9))
        assertEquals("01:15", formatRecordSeconds(75))
        assertEquals("01:30", formatRecordSeconds(200))
        assertTrue(shouldAutoStopRecording(90))
    }

    @Test
    fun buildsPagePath() {
        assertEquals("/fill/e1", feedbackPagePath("e1", "INBOX", "HUB"))
        assertEquals("/new_event", feedbackPagePath(null, "INBOX", "NEW_EVENT"))
        assertEquals("/inbox", feedbackPagePath(null, "INBOX", "HUB"))
    }
}
