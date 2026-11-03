package com.yahpz.domain

const val EVENT_MEDIA_CAP = 20
const val EVENT_MEDIA_CAPTION_MAX = 200
const val EVENT_MEDIA_LEFTOVER_ERROR = "בחרו מתי צולמה כל תמונה."
const val EVENT_MEDIA_CAP_ERROR = "ניתן לצרף עד 20 תמונות וסרטונים לאירוע."
const val EVENT_MEDIA_CAPTION_ERROR = "התיאור קצר עד 200 תווים."
const val EVENT_MEDIA_BAD_TYPE = "לא ניתן להעלות קובץ זה. בחרו תמונה או סרטון."
const val EVENT_MEDIA_TOO_LARGE = "הקובץ גדול מדי. בחרו תמונה אחרת."
const val EVENT_MEDIA_COMPRESS_FAIL = "לא הצלחנו לדחוס את התמונה. נסו תמונה אחרת."
const val EVENT_MEDIA_HEIC_FAIL =
    "לא הצלחנו לקרוא את התמונה. שמרו כ-JPEG או PNG ונסו שוב."
const val EVENT_MEDIA_NETWORK = "ההעלאה נכשלה. נסו שוב."
const val EVENT_MEDIA_CAMERA_FAILED = "לא הצלחנו לפתוח את המצלמה."
const val EVENT_MEDIA_TITLE = "מדיה"
/** Paperclip mark on the unit Events list when an event has media. */
const val EVENT_ATTACHMENT_LABEL = "יש תמונה מצורפת"
const val EVENT_MEDIA_EMPTY = "אין תמונות או סרטונים לאירוע זה."
const val EVENT_MEDIA_ADDED = "התמונה נוספה"
const val EVENT_MEDIA_UPDATED = "התמונה עודכנה"
const val EVENT_MEDIA_DELETED = "התמונה נמחקה"
const val EVENT_MEDIA_VIDEO_ADDED = "הסרטון נוסף"
const val EVENT_MEDIA_VIDEO_DELETED = "הסרטון נמחק"
const val EVENT_MEDIA_VIDEO_TOO_LARGE = "הסרטון ארוך מדי. ניתן לצרף סרטון קצר, עד 25MB."
const val EVENT_MEDIA_VIDEO_MAX_BYTES = 25L * 1024 * 1024
const val EVENT_MEDIA_TAB_LABEL = "מדיה"
const val EVENT_MEDIA_DOCS_TAB_LABEL = "תיעוד"

enum class EventMediaTakenWhen(val raw: String) {
    BEFORE_TREATMENT("before_treatment"),
    DURING_AFTER_TREATMENT("during_after_treatment"),
}

fun eventMediaTakenWhenLabel(value: EventMediaTakenWhen): String = when (value) {
    EventMediaTakenWhen.BEFORE_TREATMENT -> "לפני הטיפול"
    EventMediaTakenWhen.DURING_AFTER_TREATMENT -> "במהלך/לאחר הטיפול"
}

fun parseEventMediaTakenWhen(raw: String): EventMediaTakenWhen? =
    EventMediaTakenWhen.entries.firstOrNull { it.raw == raw }

data class EventMedia(
    val id: String,
    val eventId: String,
    val uploadedBy: String,
    val uploaderName: String?,
    val treatedPlateIds: List<String>,
    val caption: String?,
    val takenWhen: EventMediaTakenWhen,
    val storagePath: String,
    val mimeType: String,
    val byteSize: Int,
    val width: Int?,
    val height: Int?,
    val createdAt: String,
    val signedUrl: String?,
)

data class EventMediaPlateOption(
    val id: String,
    val plateNumber: String,
    val model: String?,
    val color: String?,
    val logoSlug: String?,
)

data class EventMediaBands(
    val before: List<EventMedia>,
    val during: List<EventMedia>,
)

fun leftoverEventMediaError(unfinishedDraftCount: Int, mode: FillMode): String? {
    if (mode != FillMode.COMPLETE) return null
    if (unfinishedDraftCount <= 0) return null
    return EVENT_MEDIA_LEFTOVER_ERROR
}

fun captionError(caption: String): String? {
    if (caption.length <= EVENT_MEDIA_CAPTION_MAX) return null
    return EVENT_MEDIA_CAPTION_ERROR
}

fun slotsRemaining(savedCount: Int, inFlightCount: Int): Int =
    (EVENT_MEDIA_CAP - savedCount - inFlightCount).coerceAtLeast(0)

fun canAddMoreMedia(savedCount: Int, inFlightCount: Int): Boolean =
    slotsRemaining(savedCount, inFlightCount) > 0

/** Mirrors the event_media_delete RLS policy: only the uploader, while they may write. */
fun canDeleteEventMedia(item: EventMedia, viewerId: String?, canWrite: Boolean): Boolean =
    canWrite && viewerId != null && item.uploadedBy == viewerId

fun groupMediaByTakenWhen(items: List<EventMedia>): EventMediaBands {
    val byCreated = compareBy<EventMedia> { it.createdAt }
    return EventMediaBands(
        before = items.filter { it.takenWhen == EventMediaTakenWhen.BEFORE_TREATMENT }.sortedWith(byCreated),
        during = items.filter { it.takenWhen == EventMediaTakenWhen.DURING_AFTER_TREATMENT }.sortedWith(byCreated),
    )
}

/** Videos are stored as uploaded. Keep in sync with the event_media_type_and_size check. */
private val EVENT_MEDIA_VIDEO_EXTENSIONS = mapOf(
    "video/mp4" to "mp4",
    "video/quicktime" to "mov",
    "video/webm" to "webm",
)

enum class EventMediaKind { IMAGE, VIDEO }

/** Any image is compressed to JPEG; only MP4, QuickTime and WebM videos are kept. */
fun eventMediaKind(mimeType: String?): EventMediaKind? = when {
    mimeType == null -> null
    mimeType.startsWith("image/") -> EventMediaKind.IMAGE
    mimeType in EVENT_MEDIA_VIDEO_EXTENSIONS -> EventMediaKind.VIDEO
    else -> null
}

fun isVideoMedia(mimeType: String): Boolean = mimeType.startsWith("video/")

fun rejectVideoFile(byteSize: Long): String? =
    if (byteSize <= 0 || byteSize > EVENT_MEDIA_VIDEO_MAX_BYTES) EVENT_MEDIA_VIDEO_TOO_LARGE else null

fun eventMediaStoragePath(eventId: String, mediaId: String, mimeType: String = "image/jpeg"): String =
    "$eventId/$mediaId.${EVENT_MEDIA_VIDEO_EXTENSIONS[mimeType] ?: "jpg"}"

fun mergeMediaPlates(
    responderKeyed: List<EventMediaPlateOption>,
    eventKeyed: List<EventMediaPlateOption>,
): List<EventMediaPlateOption> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<EventMediaPlateOption>()
    for (row in responderKeyed + eventKeyed) {
        if (!seen.add(row.id)) continue
        out += row
    }
    return out
}

fun uniquePlateIds(ids: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<String>()
    for (id in ids) {
        if (id.isEmpty() || !seen.add(id)) continue
        out += id
    }
    return out
}

fun togglePlateId(ids: List<String>, id: String): List<String> {
    if (id.isEmpty()) return uniquePlateIds(ids)
    return if (ids.contains(id)) ids.filter { it != id } else uniquePlateIds(ids + id)
}

fun mapEventMediaError(message: String?): String {
    if (message?.contains("event_media_cap") == true) return EVENT_MEDIA_CAP_ERROR
    return EVENT_MEDIA_NETWORK
}

fun mediaPlateLabel(plate: EventMediaPlateOption): String {
    val caption = treatedPlateCaption(model = plate.model, color = plate.color)
    val plateLabel = formatPlate(plate.plateNumber)
    return if (caption != null) "$plateLabel $caption" else plateLabel
}

const val EVENT_MEDIA_PLATES_TITLE = "רכבים בתמונה"
const val EVENT_MEDIA_ASSIGN = "שיוך לרכב"
const val EVENT_MEDIA_ASSIGN_HINT = "בחרו את הרכבים שמופיעים בתמונה."
const val EVENT_MEDIA_ASSIGN_SAVED = "השיוך נשמר."
const val EVENT_MEDIA_AUTO_LINKED = "התמונה שויכה לרכב"
const val EVENT_MEDIA_AUTO_LINK_UNDONE = "השיוך בוטל."

/**
 * Ids of saved plates whose digits equal a scanned plate, in [plates] order,
 * skipping ids already in [linkedIds] and repeats.
 */
fun autoLinkPlateIds(
    scannedDigits: List<String>,
    plates: List<EventMediaPlateOption>,
    linkedIds: List<String>,
): List<String> {
    val scanned = scannedDigits.map(::plateDigits).filter { it.isNotEmpty() }.toSet()
    if (scanned.isEmpty()) return emptyList()
    val seen = linkedIds.toMutableSet()
    val out = mutableListOf<String>()
    for (plate in plates) {
        if (plateDigits(plate.plateNumber) !in scanned) continue
        if (!seen.add(plate.id)) continue
        out += plate.id
    }
    return out
}
