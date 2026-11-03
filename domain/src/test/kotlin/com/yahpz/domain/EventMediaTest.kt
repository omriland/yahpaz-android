package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMediaTest {
    private fun media(patch: EventMedia.() -> EventMedia = { this }): EventMedia {
        val base = EventMedia(
            id = "m1",
            eventId = "e1",
            uploadedBy = "u1",
            uploaderName = "דנה",
            treatedPlateIds = emptyList(),
            caption = null,
            takenWhen = EventMediaTakenWhen.BEFORE_TREATMENT,
            storagePath = "e1/m1.jpg",
            mimeType = "image/jpeg",
            byteSize = 1000,
            width = 800,
            height = 600,
            createdAt = "2026-08-19T10:00:00.000Z",
            signedUrl = null,
        )
        return base.patch()
    }

    @Test
    fun leftoverIgnoresDraftsOnDraftSave() {
        assertNull(leftoverEventMediaError(2, FillMode.DRAFT))
    }

    @Test
    fun leftoverBlocksCompleteWhenDraftMissingWhenTaken() {
        assertEquals(EVENT_MEDIA_LEFTOVER_ERROR, leftoverEventMediaError(1, FillMode.COMPLETE))
    }

    @Test
    fun leftoverAllowsCompleteWithZeroUnfinished() {
        assertNull(leftoverEventMediaError(0, FillMode.COMPLETE))
    }

    @Test
    fun captionAllowsEmptyAnd200() {
        assertNull(captionError(""))
        assertNull(captionError("א".repeat(200)))
    }

    @Test
    fun captionRejects201() {
        assertEquals(EVENT_MEDIA_CAPTION_ERROR, captionError("א".repeat(201)))
    }

    @Test
    fun capAllowsTwentiethAndBlocksTwentyFirst() {
        assertTrue(canAddMoreMedia(19, 0))
        assertFalse(canAddMoreMedia(19, 1))
        assertFalse(canAddMoreMedia(20, 0))
        assertEquals(1, slotsRemaining(18, 1))
        assertEquals(20, EVENT_MEDIA_CAP)
    }

    @Test
    fun groupSortsEachBandByCreatedAt() {
        val grouped = groupMediaByTakenWhen(
            listOf(
                media { copy(id = "b2", createdAt = "2026-08-19T12:00:00.000Z") },
                media {
                    copy(
                        id = "d1",
                        takenWhen = EventMediaTakenWhen.DURING_AFTER_TREATMENT,
                        createdAt = "2026-08-19T11:00:00.000Z",
                    )
                },
                media { copy(id = "b1", createdAt = "2026-08-19T10:00:00.000Z") },
            ),
        )
        assertEquals(listOf("b1", "b2"), grouped.before.map { it.id })
        assertEquals(listOf("d1"), grouped.during.map { it.id })
    }

    @Test
    fun storagePathIsEventIdSlashMediaIdJpg() {
        assertEquals("e1/m1.jpg", eventMediaStoragePath("e1", "m1"))
    }

    @Test
    fun mapEventMediaErrorMapsCap() {
        assertEquals(EVENT_MEDIA_CAP_ERROR, mapEventMediaError("event_media_cap"))
        assertEquals(EVENT_MEDIA_CAP_ERROR, mapEventMediaError("new row violates event_media_cap"))
        assertEquals(EVENT_MEDIA_NETWORK, mapEventMediaError("jwt expired"))
        assertEquals(EVENT_MEDIA_NETWORK, mapEventMediaError(null))
    }

    @Test
    fun togglePlateIdAddsAndRemoves() {
        assertEquals(listOf("a", "b"), togglePlateId(listOf("a"), "b"))
        assertEquals(listOf("b"), togglePlateId(listOf("a", "b"), "a"))
    }

    @Test
    fun uniquePlateIdsDropsBlanksAndDuplicates() {
        assertEquals(listOf("b", "a"), uniquePlateIds(listOf("b", "", "a", "b")))
    }

    @Test
    fun mergeMediaPlatesUnionsById() {
        val a = EventMediaPlateOption("a", "12-345-67", "REXTON", "שחור", "ssangyong")
        val b = EventMediaPlateOption("b", "123-45-678", null, null, null)
        assertEquals(listOf(a, b), mergeMediaPlates(listOf(a), listOf(a, b)))
    }

    @Test
    fun `video storage paths use the video extension`() {
        assertEquals("e1/m1.mp4", eventMediaStoragePath("e1", "m1", "video/mp4"))
        assertEquals("e1/m1.mov", eventMediaStoragePath("e1", "m1", "video/quicktime"))
        assertEquals("e1/m1.webm", eventMediaStoragePath("e1", "m1", "video/webm"))
        assertEquals("e1/m1.jpg", eventMediaStoragePath("e1", "m1", "image/jpeg"))
    }

    @Test
    fun `media kind keeps images and the three video types only`() {
        assertEquals(EventMediaKind.IMAGE, eventMediaKind("image/heic"))
        assertEquals(EventMediaKind.VIDEO, eventMediaKind("video/mp4"))
        assertEquals(EventMediaKind.VIDEO, eventMediaKind("video/quicktime"))
        assertEquals(EventMediaKind.VIDEO, eventMediaKind("video/webm"))
        assertNull(eventMediaKind("video/3gpp"))
        assertNull(eventMediaKind(null))
        assertTrue(isVideoMedia("video/mp4"))
        assertFalse(isVideoMedia("image/jpeg"))
    }

    @Test
    fun `videos up to 25 MB are accepted`() {
        assertNull(rejectVideoFile(25L * 1024 * 1024))
        assertEquals(EVENT_MEDIA_VIDEO_TOO_LARGE, rejectVideoFile(25L * 1024 * 1024 + 1))
        assertEquals(EVENT_MEDIA_VIDEO_TOO_LARGE, rejectVideoFile(0))
    }

    @Test
    fun `copy matches the web`() {
        assertEquals("ניתן לצרף עד 20 תמונות וסרטונים לאירוע.", EVENT_MEDIA_CAP_ERROR)
        assertEquals("אין תמונות או סרטונים לאירוע זה.", EVENT_MEDIA_EMPTY)
        assertEquals("הסרטון ארוך מדי. ניתן לצרף סרטון קצר, עד 25MB.", EVENT_MEDIA_VIDEO_TOO_LARGE)
    }

    @Test
    fun uploaderCanDeleteWhileWriting() {
        assertTrue(canDeleteEventMedia(media(), "u1", canWrite = true))
    }

    @Test
    fun someoneElsesMediaIsViewOnly() {
        assertFalse(canDeleteEventMedia(media(), "u2", canWrite = true))
    }

    @Test
    fun uploaderCannotDeleteWhenReadOnly() {
        assertFalse(canDeleteEventMedia(media(), "u1", canWrite = false))
    }

    @Test
    fun signedOutViewerCannotDelete() {
        assertFalse(canDeleteEventMedia(media(), null, canWrite = true))
    }
}
