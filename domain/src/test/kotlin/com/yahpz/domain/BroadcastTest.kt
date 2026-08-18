package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastTest {
    private val full = BroadcastCandidate(
        id = "full",
        email = "a@b.com",
        phone = "0501234567",
        roles = listOf("responder"),
    )
    private val emailOnly = BroadcastCandidate(id = "email", email = "c@d.com", phone = null)
    private val phoneOnly = BroadcastCandidate(id = "phone", email = "  ", phone = "0521111111")
    private val appOnly = BroadcastCandidate(id = "app", email = null, phone = "12", hasApp = true)
    private val lead = BroadcastCandidate(
        id = "lead",
        email = "lead@b.com",
        phone = "0533333333",
        roles = listOf("responder", "shift_lead"),
    )
    private val boss = BroadcastCandidate(
        id = "boss",
        email = "boss@b.com",
        phone = "0544444444",
        roles = listOf("admin"),
    )

    @Test
    fun `both channels reach anyone with an email or a mobile`() {
        val preview = previewUnitBroadcast(
            listOf(full, emailOnly, phoneOnly),
            BroadcastChannel.BOTH,
            BroadcastAudience.ALL,
        )
        assertEquals(3, preview.audienceCount)
        assertEquals(3, preview.recipientCount)
        assertEquals(2, preview.emailCount)
        assertEquals(2, preview.smsCount)
        assertEquals(1, preview.skippedNoEmail)
        assertEquals(1, preview.skippedNoPhone)
        assertTrue(preview.canSend)
    }

    @Test
    fun `sms only ignores email addresses`() {
        val preview = previewUnitBroadcast(
            listOf(emailOnly),
            BroadcastChannel.SMS,
            BroadcastAudience.ALL,
        )
        assertEquals(0, preview.recipientCount)
        assertEquals(1, preview.skippedNoPhone)
        assertEquals(0, preview.skippedNoEmail)
        assertFalse(preview.canSend)
    }

    @Test
    fun `a user with the app counts even without email or a valid mobile`() {
        val preview = previewUnitBroadcast(
            listOf(appOnly),
            BroadcastChannel.BOTH,
            BroadcastAudience.ALL,
        )
        assertEquals(1, preview.recipientCount)
        assertEquals(1, preview.pushCount)
        assertTrue(preview.canSend)
    }

    @Test
    fun `inactive and pending invitees never receive a broadcast`() {
        val candidates = listOf(
            full.copy(id = "off", active = false),
            full.copy(id = "pending", invitePending = true),
        )
        val preview = previewUnitBroadcast(candidates, BroadcastChannel.BOTH, BroadcastAudience.ALL)
        assertEquals(0, preview.audienceCount)
        assertEquals(0, preview.recipientCount)
    }

    @Test
    fun `audiences narrow to the matching role`() {
        val all = listOf(full, lead, boss)
        assertEquals(
            1,
            previewUnitBroadcast(all, BroadcastChannel.BOTH, BroadcastAudience.ADMINS).recipientCount,
        )
        assertEquals(
            1,
            previewUnitBroadcast(all, BroadcastChannel.BOTH, BroadcastAudience.SHIFT_LEADS).recipientCount,
        )
        assertEquals(
            3,
            previewUnitBroadcast(all, BroadcastChannel.BOTH, BroadcastAudience.ALL).recipientCount,
        )
    }

    @Test
    fun `subject is required unless the channel is sms only`() {
        val draft = BroadcastDraft(channel = BroadcastChannel.BOTH, subject = "", body = "טקסט")
        assertEquals(BROADCAST_SUBJECT_REQUIRED, validateBroadcastDraft(draft).subject)
        assertNull(validateBroadcastDraft(draft.copy(channel = BroadcastChannel.SMS)).subject)
        assertTrue(validateBroadcastDraft(draft.copy(channel = BroadcastChannel.SMS)).isEmpty)
        assertFalse(needsBroadcastSubject(BroadcastChannel.SMS))
        assertTrue(needsBroadcastSubject(BroadcastChannel.EMAIL))
    }

    @Test
    fun `body is required and both fields have a ceiling`() {
        val long = "א".repeat(BROADCAST_BODY_MAX + 1)
        assertEquals(
            BROADCAST_BODY_REQUIRED,
            validateBroadcastDraft(BroadcastDraft(subject = "נושא", body = "   ")).body,
        )
        assertEquals(
            BROADCAST_BODY_TOO_LONG,
            validateBroadcastDraft(BroadcastDraft(subject = "נושא", body = long)).body,
        )
        assertEquals(
            BROADCAST_SUBJECT_TOO_LONG,
            validateBroadcastDraft(
                BroadcastDraft(subject = "א".repeat(BROADCAST_SUBJECT_MAX + 1), body = "טקסט"),
            ).subject,
        )
    }

    @Test
    fun `confirm copy names the count, the audience and the skips`() {
        val preview = previewUnitBroadcast(
            listOf(full, emailOnly, appOnly),
            BroadcastChannel.BOTH,
            BroadcastAudience.ALL,
        )
        val copy = broadcastConfirmCopy(preview, BroadcastChannel.BOTH, BroadcastAudience.ALL)
        assertTrue(copy.startsWith("יישלח ל־3 משתמשים פעילים (SMS + אימייל)."))
        assertTrue(copy.contains("1 עם האפליקציה"))
        assertTrue(copy.contains("2 בלי טלפון ידולגו"))
        assertTrue(copy.endsWith("לשלוח?"))
    }

    @Test
    fun `empty audience explains there is nobody to send to`() {
        val preview = previewUnitBroadcast(emptyList(), BroadcastChannel.SMS, BroadcastAudience.ADMINS)
        assertEquals(
            BROADCAST_NO_RECIPIENTS,
            broadcastConfirmCopy(preview, BroadcastChannel.SMS, BroadcastAudience.ADMINS),
        )
        assertEquals(
            BROADCAST_NO_RECIPIENTS,
            broadcastPreviewCaption(preview, BroadcastChannel.SMS, BroadcastAudience.ADMINS),
        )
    }

    @Test
    fun `preview caption counts recipients`() {
        val preview = previewUnitBroadcast(listOf(full), BroadcastChannel.BOTH, BroadcastAudience.ALL)
        assertEquals(
            "1 נמענים ישלחו.",
            broadcastPreviewCaption(preview, BroadcastChannel.BOTH, BroadcastAudience.ALL),
        )
    }

    @Test
    fun `result copy reports skips, failures and push`() {
        val copy = broadcastResultCopy(
            BroadcastSendResult(
                recipientCount = 12,
                skippedNoPhone = 2,
                skippedNoEmail = 1,
                failedCount = 3,
                pushCount = 5,
                pushFailedCount = 1,
            ),
        )
        assertEquals(
            "נשלח ל־12. 2 בלי טלפון דולגו. 1 בלי דוא״ל דולגו. 3 נכשלו. 4 התראות נשלחו. 1 התראות נכשלו.",
            copy,
        )
        assertEquals(
            "נשלח ל־4.",
            broadcastResultCopy(BroadcastSendResult(4, 0, 0, 0, 0, 0)),
        )
    }

    @Test
    fun `only admin may send a unit broadcast`() {
        assertTrue(canSendUnitBroadcast(listOf("admin")))
        assertTrue(canSendUnitBroadcast(listOf("super_admin")))
        assertFalse(canSendUnitBroadcast(listOf("shift_lead")))
        assertFalse(canSendUnitBroadcast(listOf("responder")))
    }

    @Test
    fun `channel and audience raw values round-trip for the edge function`() {
        BroadcastChannel.entries.forEach {
            assertEquals(it, BroadcastChannel.fromRaw(it.raw))
            assertTrue(broadcastChannelLabel(it).isNotEmpty())
        }
        BroadcastAudience.entries.forEach {
            assertEquals(it, BroadcastAudience.fromRaw(it.raw))
            assertTrue(broadcastAudienceLabel(it).isNotEmpty())
        }
        assertEquals(BroadcastChannel.BOTH, BroadcastChannel.fromRaw("whatsapp"))
        assertEquals(BroadcastAudience.ALL, BroadcastAudience.fromRaw("everyone"))
    }

    @Test
    fun `log entry summarises the send`() {
        val entry = BroadcastLogEntry(
            id = "1",
            createdAt = "2026-08-01T10:00:00Z",
            channel = BroadcastChannel.SMS,
            audience = BroadcastAudience.SHIFT_LEADS,
            recipientCount = 9,
            pushCount = 4,
            senderName = "דנה כהן",
            senderCallsign = "12",
        )
        assertEquals("SMS · אחמ״שים · נשלח ל־9 · 4 התראות", entry.summary)
        assertEquals("דנה כהן · 12", entry.senderDisplay)
        assertEquals("—", entry.copy(senderName = null, senderCallsign = null).senderDisplay)
    }
}
