package com.yahpz.domain

const val FEEDBACK_BODY_MAX = 2000
const val FEEDBACK_AUDIO_MAX_BYTES = 5 * 1024 * 1024
const val FEEDBACK_RECORD_MAX_SECONDS = 90
const val FEEDBACK_NETWORK = "השליחה נכשלה. בדקו את החיבור ונסו שוב."
const val FEEDBACK_EMPTY_ERROR = "יש לכתוב הערה או להקליט הודעה."
const val FEEDBACK_BODY_ERROR = "ההערה ארוכה מדי. קצרו ל־2,000 תווים."
const val FEEDBACK_KIND_ERROR = "בחרו אם זה באג או הצעה."
const val FEEDBACK_AUDIO_SIZE_ERROR = "ההקלטה ארוכה מדי. הקליטו שוב בקצרה."
const val FEEDBACK_MIC_ERROR = "אין גישה למיקרופון. אפשר לכתוב הערה במקום."
const val FEEDBACK_SENT = "המשוב נשלח. תודה."
const val FEEDBACK_HIDE_UNTIL_REFRESH = "הסתרה עד הרענון הבא"
const val FEEDBACK_LABEL = "משוב"
const val FEEDBACK_KIND_BUG = "באג"
const val FEEDBACK_KIND_SUGGESTION = "הצעה"

fun feedbackBodyError(body: String): String? =
    if (body.length > FEEDBACK_BODY_MAX) FEEDBACK_BODY_ERROR else null

fun feedbackSubmitError(kind: String?, body: String, hasAudio: Boolean): String? {
    if (kind != "bug" && kind != "suggestion") return FEEDBACK_KIND_ERROR
    val trimmed = body.trim()
    feedbackBodyError(trimmed)?.let { return it }
    if (trimmed.isEmpty() && !hasAudio) return FEEDBACK_EMPTY_ERROR
    return null
}

fun feedbackStorageExt(mime: String): String {
    val lower = mime.lowercase()
    return when {
        "webm" in lower -> "webm"
        "ogg" in lower -> "ogg"
        "mpeg" in lower || "mp3" in lower -> "mp3"
        "mp4" in lower || "m4a" in lower || "aac" in lower -> "m4a"
        else -> "m4a"
    }
}

fun normalizeFeedbackAudioMime(mime: String): String {
    val lower = mime.lowercase()
    return when {
        "webm" in lower -> "audio/webm"
        "ogg" in lower -> "audio/ogg"
        "mpeg" in lower || "mp3" in lower -> "audio/mpeg"
        else -> "audio/mp4"
    }
}

fun feedbackStoragePath(userId: String, feedbackId: String, mime: String): String =
    "$userId/$feedbackId.${feedbackStorageExt(mime)}"

fun formatRecordSeconds(total: Int): String {
    val safe = total.coerceIn(0, FEEDBACK_RECORD_MAX_SECONDS)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun shouldAutoStopRecording(elapsedSeconds: Int): Boolean =
    elapsedSeconds >= FEEDBACK_RECORD_MAX_SECONDS

fun feedbackPagePath(
    fillEventId: String?,
    tab: String,
    toolsDestination: String,
): String {
    if (!fillEventId.isNullOrBlank()) return "/fill/$fillEventId"
    if (toolsDestination != "HUB") return "/${toolsDestination.lowercase()}"
    return "/${tab.lowercase()}"
}
