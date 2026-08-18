package com.yahpz.domain

/**
 * Unit-wide תפוצה. Mirrors the web `unitBroadcast.ts`: the app picks a channel and an
 * audience, previews who is reachable, and the `unit-broadcast` edge function does the
 * actual sending. Push is added server-side for anyone with a registered device.
 */
enum class BroadcastChannel(val raw: String) {
    EMAIL("email"),
    SMS("sms"),
    BOTH("both");

    companion object {
        fun fromRaw(raw: String?): BroadcastChannel = entries.find { it.raw == raw } ?: BOTH
    }
}

enum class BroadcastAudience(val raw: String) {
    ALL("all"),
    ADMINS("admins"),
    SHIFT_LEADS("shift_leads");

    companion object {
        fun fromRaw(raw: String?): BroadcastAudience = entries.find { it.raw == raw } ?: ALL
    }
}

const val BROADCAST_SUBJECT_MAX = 200
const val BROADCAST_BODY_MAX = 2000

const val BROADCAST_TITLE = "תפוצה לכלל היחידה"
const val BROADCAST_CAPTION = "שליחת הודעה למנהלים, לאחמ״שים או לכלל המשתמשים הפעילים."
const val BROADCAST_SUBJECT_REQUIRED = "יש למלא נושא לדוא״ל."
const val BROADCAST_SUBJECT_TOO_LONG = "הנושא ארוך מדי."
const val BROADCAST_BODY_REQUIRED = "יש למלא את תוכן ההודעה."
const val BROADCAST_BODY_TOO_LONG = "ההודעה ארוכה מדי."
const val BROADCAST_NO_RECIPIENTS = "אין נמענים לשליחה בקהל ובערוץ שנבחרו."
const val BROADCAST_SEND_FAILED = "השליחה נכשלה. בדקו את החיבור ונסו שוב."
const val BROADCAST_LOAD_FAILED = "טעינת התפוצה נכשלה. בדקו את החיבור ונסו שוב."
const val BROADCAST_LOG_EMPTY = "עדיין לא נשלחה תפוצה."
const val BROADCAST_LOADING_RECIPIENTS = "טוען נמענים…"

val BROADCAST_CHANNEL_LABELS = mapOf(
    BroadcastChannel.EMAIL to "אימייל",
    BroadcastChannel.SMS to "SMS",
    BroadcastChannel.BOTH to "SMS + אימייל",
)

val BROADCAST_AUDIENCE_LABELS = mapOf(
    BroadcastAudience.ALL to "כלל המשתמשים",
    BroadcastAudience.ADMINS to "מנהלים",
    BroadcastAudience.SHIFT_LEADS to "אחמ״שים",
)

fun broadcastChannelLabel(channel: BroadcastChannel): String =
    BROADCAST_CHANNEL_LABELS.getValue(channel)

fun broadcastAudienceLabel(audience: BroadcastAudience): String =
    BROADCAST_AUDIENCE_LABELS.getValue(audience)

/** The `unit-broadcast` edge function checks `has_role(admin)`, so אחמ״ש cannot send. */
fun canSendUnitBroadcast(roles: List<String>): Boolean = isAdmin(roles)

data class BroadcastCandidate(
    val id: String,
    val email: String? = null,
    val phone: String? = null,
    val roles: List<String> = emptyList(),
    val active: Boolean = true,
    val invitePending: Boolean = false,
    val hasApp: Boolean = false,
)

data class BroadcastDraft(
    val channel: BroadcastChannel = BroadcastChannel.BOTH,
    val audience: BroadcastAudience = BroadcastAudience.ALL,
    val subject: String = "",
    val body: String = "",
)

data class BroadcastPreview(
    val audienceCount: Int,
    val recipientCount: Int,
    val emailCount: Int,
    val smsCount: Int,
    val pushCount: Int,
    val skippedNoPhone: Int,
    val skippedNoEmail: Int,
) {
    val canSend: Boolean get() = recipientCount > 0
}

data class BroadcastSendResult(
    val recipientCount: Int,
    val skippedNoPhone: Int,
    val skippedNoEmail: Int,
    val failedCount: Int,
    val pushCount: Int,
    val pushFailedCount: Int,
)

data class BroadcastDraftErrors(
    val subject: String? = null,
    val body: String? = null,
) {
    val isEmpty: Boolean get() = subject == null && body == null
    val firstMessage: String? get() = subject ?: body
}

/** SMS-only broadcasts carry no subject line. */
fun needsBroadcastSubject(channel: BroadcastChannel): Boolean = channel != BroadcastChannel.SMS

fun validateBroadcastDraft(draft: BroadcastDraft): BroadcastDraftErrors {
    val subject = draft.subject.trim()
    val body = draft.body.trim()
    return BroadcastDraftErrors(
        subject = when {
            !needsBroadcastSubject(draft.channel) -> null
            subject.isEmpty() -> BROADCAST_SUBJECT_REQUIRED
            subject.length > BROADCAST_SUBJECT_MAX -> BROADCAST_SUBJECT_TOO_LONG
            else -> null
        },
        body = when {
            body.isEmpty() -> BROADCAST_BODY_REQUIRED
            body.length > BROADCAST_BODY_MAX -> BROADCAST_BODY_TOO_LONG
            else -> null
        },
    )
}

private fun BroadcastCandidate.isEligible(): Boolean = active && !invitePending

private fun BroadcastCandidate.matches(audience: BroadcastAudience): Boolean = when (audience) {
    BroadcastAudience.ALL -> true
    BroadcastAudience.ADMINS -> roles.contains(AppRole.ADMIN.raw)
    BroadcastAudience.SHIFT_LEADS -> roles.contains(AppRole.SHIFT_LEAD.raw)
}

/**
 * Anyone reachable on at least one of the requested channels counts once. Push is
 * always attempted for users with the app, whatever the channel.
 */
fun previewUnitBroadcast(
    candidates: List<BroadcastCandidate>,
    channel: BroadcastChannel,
    audience: BroadcastAudience,
): BroadcastPreview {
    val pool = candidates.filter { it.isEligible() && it.matches(audience) }
    val wantsEmail = channel != BroadcastChannel.SMS
    val wantsSms = channel != BroadcastChannel.EMAIL

    var emailCount = 0
    var smsCount = 0
    var pushCount = 0
    var skippedNoEmail = 0
    var skippedNoPhone = 0
    var recipientCount = 0

    for (candidate in pool) {
        val hasEmail = !candidate.email.isNullOrBlank()
        val hasSms = isValidIlMobile(candidate.phone)
        val emailOk = wantsEmail && hasEmail
        val smsOk = wantsSms && hasSms
        if (wantsEmail && !hasEmail) skippedNoEmail += 1
        if (wantsSms && !hasSms) skippedNoPhone += 1
        if (emailOk) emailCount += 1
        if (smsOk) smsCount += 1
        if (candidate.hasApp) pushCount += 1
        if (emailOk || smsOk || candidate.hasApp) recipientCount += 1
    }

    return BroadcastPreview(
        audienceCount = pool.size,
        recipientCount = recipientCount,
        emailCount = emailCount,
        smsCount = smsCount,
        pushCount = pushCount,
        skippedNoPhone = skippedNoPhone,
        skippedNoEmail = skippedNoEmail,
    )
}

private fun audienceNoun(audience: BroadcastAudience): String = when (audience) {
    BroadcastAudience.ADMINS -> "מנהלים פעילים"
    BroadcastAudience.SHIFT_LEADS -> "אחמ״שים פעילים"
    BroadcastAudience.ALL -> "משתמשים פעילים"
}

private fun previewExtras(preview: BroadcastPreview): List<String> = buildList {
    if (preview.pushCount > 0) add("${formatNumber(preview.pushCount)} עם האפליקציה")
    if (preview.skippedNoPhone > 0) add("${formatNumber(preview.skippedNoPhone)} בלי טלפון ידולגו")
    if (preview.skippedNoEmail > 0) add("${formatNumber(preview.skippedNoEmail)} בלי דוא״ל ידולגו")
}

/** Confirmation copy, also reused as the caption when there is nobody to send to. */
fun broadcastConfirmCopy(
    preview: BroadcastPreview,
    channel: BroadcastChannel,
    audience: BroadcastAudience,
): String {
    if (!preview.canSend) return BROADCAST_NO_RECIPIENTS
    val head = "יישלח ל־${formatNumber(preview.recipientCount)} ${audienceNoun(audience)} " +
        "(${broadcastChannelLabel(channel)})."
    val extras = previewExtras(preview)
    if (extras.isEmpty()) return "$head לשלוח?"
    return "$head ${extras.joinToString(". ")}. לשלוח?"
}

/** Caption under the compose form: how many will be reached and who is skipped. */
fun broadcastPreviewCaption(
    preview: BroadcastPreview,
    channel: BroadcastChannel,
    audience: BroadcastAudience,
): String {
    if (!preview.canSend) return broadcastConfirmCopy(preview, channel, audience)
    return listOf(
        "${formatNumber(preview.recipientCount)} נמענים ישלחו.",
        previewExtras(preview).joinToString(". "),
    ).filter { it.isNotEmpty() }.joinToString(" ")
}

fun broadcastResultCopy(result: BroadcastSendResult): String {
    val parts = mutableListOf("נשלח ל־${formatNumber(result.recipientCount)}.")
    if (result.skippedNoPhone > 0) parts += "${formatNumber(result.skippedNoPhone)} בלי טלפון דולגו."
    if (result.skippedNoEmail > 0) parts += "${formatNumber(result.skippedNoEmail)} בלי דוא״ל דולגו."
    if (result.failedCount > 0) parts += "${formatNumber(result.failedCount)} נכשלו."
    val pushOk = result.pushCount - result.pushFailedCount
    if (pushOk > 0) parts += "${formatNumber(pushOk)} התראות נשלחו."
    if (result.pushFailedCount > 0) parts += "${formatNumber(result.pushFailedCount)} התראות נכשלו."
    return parts.joinToString(" ")
}

data class BroadcastLogEntry(
    val id: String,
    val createdAt: String,
    val channel: BroadcastChannel,
    val audience: BroadcastAudience,
    val subject: String = "",
    val body: String = "",
    val recipientCount: Int = 0,
    val pushCount: Int = 0,
    val pushFailedCount: Int = 0,
    val senderName: String? = null,
    val senderCallsign: String? = null,
) {
    val senderDisplay: String get() = personDisplay(senderName, senderCallsign, fallback = "—")

    val summary: String
        get() = buildList {
            add(broadcastChannelLabel(channel))
            add(broadcastAudienceLabel(audience))
            add("נשלח ל־${formatNumber(recipientCount)}")
            if (pushCount > 0) add("${formatNumber(pushCount)} התראות")
            if (pushFailedCount > 0) add("${formatNumber(pushFailedCount)} התראות נכשלו")
        }.joinToString(" · ")
}
