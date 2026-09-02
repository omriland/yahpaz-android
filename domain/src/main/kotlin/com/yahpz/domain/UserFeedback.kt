package com.yahpz.domain

const val FEEDBACK_BODY_MAX = 2000
const val FEEDBACK_AUDIO_MAX_BYTES = 5 * 1024 * 1024
const val FEEDBACK_RECORD_MAX_SECONDS = 90
const val FEEDBACK_ATTACH_MAX = 3
const val FEEDBACK_IMAGE_MAX_BYTES = 5 * 1024 * 1024
const val FEEDBACK_VIDEO_MAX_BYTES = 25 * 1024 * 1024
const val FEEDBACK_ATTACH_NAME_MAX = 200
const val FEEDBACK_NETWORK = "השליחה נכשלה. בדקו את החיבור ונסו שוב."
const val FEEDBACK_EMPTY_ERROR = "יש לכתוב הערה או להקליט הודעה."
const val FEEDBACK_BODY_ERROR = "ההערה ארוכה מדי. קצרו ל־2,000 תווים."
const val FEEDBACK_KIND_ERROR = "בחרו אם זה באג או הצעה."
const val FEEDBACK_AUDIO_SIZE_ERROR = "ההקלטה ארוכה מדי. הקליטו שוב בקצרה."
const val FEEDBACK_MIC_ERROR = "אין גישה למיקרופון. אפשר לכתוב הערה במקום."
const val FEEDBACK_ATTACH_HINT =
    "אפשר לצרף עד 3 קבצים: צילומי מסך עד 5 מ״ב, או סרטונים קצרים עד 25 מ״ב."
const val FEEDBACK_ATTACH_COUNT_ERROR = "אפשר לצרף עד 3 קבצים."
const val FEEDBACK_ATTACH_TYPE_ERROR = "אפשר לצרף רק צילומי מסך או סרטונים קצרים."
const val FEEDBACK_ATTACH_IMAGE_SIZE_ERROR = "התמונה גדולה מדי. בחרו קובץ עד 5 מ״ב."
const val FEEDBACK_ATTACH_VIDEO_SIZE_ERROR = "הסרטון גדול מדי. בחרו קובץ עד 25 מ״ב."
const val FEEDBACK_ATTACH_UNAVAILABLE =
    "צירוף הקבצים אינו זמין כרגע. שלחו בלי קבצים, או נסו שוב מאוחר יותר."
const val FEEDBACK_ATTACH_ADD = "צירוף קובץ"
const val FEEDBACK_SENT = "המשוב נשלח. תודה."
const val FEEDBACK_HIDE_UNTIL_REFRESH = "הסתרה עד הרענון הבא"
const val FEEDBACK_LABEL = "משוב"
const val FEEDBACK_KIND_BUG = "באג"
const val FEEDBACK_KIND_SUGGESTION = "הצעה"

data class FeedbackPickedMeta(
    val name: String,
    val mime: String,
    val size: Int,
)

data class FeedbackAttachResult(
    val files: List<FeedbackPickedMeta>,
    val error: String? = null,
)

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

private val FEEDBACK_IMAGE_MIMES = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/gif",
    "image/heic",
    "image/heif",
)

private val FEEDBACK_VIDEO_MIMES = setOf(
    "video/mp4",
    "video/webm",
    "video/quicktime",
    "video/3gpp",
)

private val FEEDBACK_EXT_MIME = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "webp" to "image/webp",
    "gif" to "image/gif",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "mp4" to "video/mp4",
    "webm" to "video/webm",
    "mov" to "video/quicktime",
    "3gp" to "video/3gpp",
)

fun normalizeFeedbackAttachmentMime(mime: String, name: String): String? {
    val fromMime = mime.lowercase().substringBefore(';').trim()
    if (fromMime == "image/jpg") return "image/jpeg"
    if (fromMime in FEEDBACK_IMAGE_MIMES || fromMime in FEEDBACK_VIDEO_MIMES) return fromMime
    val ext = name.substringAfterLast('.', "").lowercase()
    return FEEDBACK_EXT_MIME[ext]
}

fun feedbackAttachmentKind(mime: String, name: String = ""): String? {
    val normalized = normalizeFeedbackAttachmentMime(mime, name) ?: return null
    return if (normalized.startsWith("image/")) "image" else "video"
}

fun feedbackAttachmentExt(mime: String, name: String = ""): String? {
    return when (normalizeFeedbackAttachmentMime(mime, name)) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/quicktime" -> "mov"
        "video/3gpp" -> "3gp"
        else -> null
    }
}

fun feedbackAttachmentStoragePath(
    userId: String,
    feedbackId: String,
    attachmentId: String,
    mime: String,
    name: String = "",
): String? {
    val ext = feedbackAttachmentExt(mime, name) ?: return null
    return "$userId/$feedbackId/$attachmentId.$ext"
}

fun feedbackAttachmentError(file: FeedbackPickedMeta): String? {
    val kind = feedbackAttachmentKind(file.mime, file.name) ?: return FEEDBACK_ATTACH_TYPE_ERROR
    if (kind == "image" && file.size > FEEDBACK_IMAGE_MAX_BYTES) return FEEDBACK_ATTACH_IMAGE_SIZE_ERROR
    if (kind == "video" && file.size > FEEDBACK_VIDEO_MAX_BYTES) return FEEDBACK_ATTACH_VIDEO_SIZE_ERROR
    if (file.size <= 0) return FEEDBACK_ATTACH_TYPE_ERROR
    return null
}

fun addFeedbackAttachments(
    current: List<FeedbackPickedMeta>,
    incoming: List<FeedbackPickedMeta>,
): FeedbackAttachResult {
    val next = current.toMutableList()
    var error: String? = null
    for (file in incoming) {
        if (next.size >= FEEDBACK_ATTACH_MAX) {
            error = FEEDBACK_ATTACH_COUNT_ERROR
            break
        }
        val fileError = feedbackAttachmentError(file)
        if (fileError != null) {
            error = fileError
            continue
        }
        next += file
    }
    return FeedbackAttachResult(files = next, error = error)
}

fun sanitizeFeedbackAttachmentName(name: String): String {
    val trimmed = name.trim().replace("/", "").replace("\\", "").ifEmpty { "קובץ" }
    return trimmed.take(FEEDBACK_ATTACH_NAME_MAX)
}

fun isMissingFeedbackAttachmentsColumn(message: String?): Boolean {
    val text = message ?: return false
    return text.contains("attachments", ignoreCase = true) &&
        (text.contains("42703") || text.contains("PGRST204") || text.contains("does not exist") ||
            text.contains("Could not find"))
}

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
