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
    fun mapsAttachmentMimeAndPath() {
        assertEquals("image/jpeg", normalizeFeedbackAttachmentMime("image/jpeg", "a.jpg"))
        assertEquals("image/png", normalizeFeedbackAttachmentMime("", "screen.PNG"))
        assertEquals("image", feedbackAttachmentKind("image/webp", "x.webp"))
        assertEquals("video", feedbackAttachmentKind("video/mp4", "x.mp4"))
        assertEquals("u1/f1/a1.png", feedbackAttachmentStoragePath("u1", "f1", "a1", "image/png", "shot.png"))
    }

    @Test
    fun rejectsFourthAttachmentAndKeepsThree() {
        val current = listOf(
            FeedbackPickedMeta("1.jpg", "image/jpeg", 10),
            FeedbackPickedMeta("2.jpg", "image/jpeg", 10),
            FeedbackPickedMeta("3.jpg", "image/jpeg", 10),
        )
        val result = addFeedbackAttachments(
            current,
            listOf(FeedbackPickedMeta("4.jpg", "image/jpeg", 10)),
        )
        assertEquals(FEEDBACK_ATTACH_MAX, result.files.size)
        assertEquals(FEEDBACK_ATTACH_COUNT_ERROR, result.error)
    }

    @Test
    fun rejectsWrongTypeAndOversizedAttachments() {
        val pdf = addFeedbackAttachments(
            emptyList(),
            listOf(FeedbackPickedMeta("note.pdf", "application/pdf", 10)),
        )
        assertTrue(pdf.files.isEmpty())
        assertEquals(FEEDBACK_ATTACH_TYPE_ERROR, pdf.error)

        val hugeImage = addFeedbackAttachments(
            emptyList(),
            listOf(FeedbackPickedMeta("big.jpg", "image/jpeg", FEEDBACK_IMAGE_MAX_BYTES + 1)),
        )
        assertTrue(hugeImage.files.isEmpty())
        assertEquals(FEEDBACK_ATTACH_IMAGE_SIZE_ERROR, hugeImage.error)

        val hugeVideo = addFeedbackAttachments(
            emptyList(),
            listOf(FeedbackPickedMeta("big.mp4", "video/mp4", FEEDBACK_VIDEO_MAX_BYTES + 1)),
        )
        assertTrue(hugeVideo.files.isEmpty())
        assertEquals(FEEDBACK_ATTACH_VIDEO_SIZE_ERROR, hugeVideo.error)
    }

    @Test
    fun acceptsValidImageAndVideo() {
        val image = addFeedbackAttachments(
            emptyList(),
            listOf(FeedbackPickedMeta("screen.jpg", "image/jpeg", 1024)),
        )
        assertNull(image.error)
        assertEquals(1, image.files.size)
        val video = addFeedbackAttachments(
            image.files,
            listOf(FeedbackPickedMeta("clip.mp4", "video/mp4", 2048)),
        )
        assertNull(video.error)
        assertEquals(2, video.files.size)
    }

    @Test
    fun sanitizesAttachmentName() {
        assertEquals("..evilname.png", sanitizeFeedbackAttachmentName("  ../evil\\name.png  "))
    }

    @Test
    fun buildsPagePath() {
        assertEquals("/fill/e1", feedbackPagePath("e1", "INBOX", "HUB"))
        assertEquals("/new_event", feedbackPagePath(null, "INBOX", "NEW_EVENT"))
        assertEquals("/inbox", feedbackPagePath(null, "INBOX", "HUB"))
    }
}
