package com.yahpz.domain

const val EVENT_MEDIA_CAP = 20
const val EVENT_MEDIA_CAPTION_MAX = 200
const val EVENT_MEDIA_LEFTOVER_ERROR = "בחרו מתי צולמה כל תמונה."
const val EVENT_MEDIA_CAP_ERROR = "ניתן לצרף עד 20 תמונות לאירוע."
const val EVENT_MEDIA_CAPTION_ERROR = "התיאור קצר עד 200 תווים."
const val EVENT_MEDIA_BAD_TYPE = "לא ניתן להעלות קובץ זה. בחרו תמונה."
const val EVENT_MEDIA_TOO_LARGE = "הקובץ גדול מדי. בחרו תמונה אחרת."
const val EVENT_MEDIA_COMPRESS_FAIL = "לא הצלחנו לדחוס את התמונה. נסו תמונה אחרת."
const val EVENT_MEDIA_HEIC_FAIL =
    "לא הצלחנו לקרוא את התמונה. שמרו כ-JPEG או PNG ונסו שוב."
const val EVENT_MEDIA_NETWORK = "ההעלאה נכשלה. נסו שוב."
const val EVENT_MEDIA_TITLE = "מדיה"
const val EVENT_MEDIA_EMPTY = "אין תמונות לאירוע זה."
const val EVENT_MEDIA_ADDED = "התמונה נוספה"
const val EVENT_MEDIA_UPDATED = "התמונה עודכנה"
const val EVENT_MEDIA_DELETED = "התמונה נמחקה"
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

fun groupMediaByTakenWhen(items: List<EventMedia>): EventMediaBands {
    val byCreated = compareBy<EventMedia> { it.createdAt }
    return EventMediaBands(
        before = items.filter { it.takenWhen == EventMediaTakenWhen.BEFORE_TREATMENT }.sortedWith(byCreated),
        during = items.filter { it.takenWhen == EventMediaTakenWhen.DURING_AFTER_TREATMENT }.sortedWith(byCreated),
    )
}

fun eventMediaStoragePath(eventId: String, mediaId: String): String = "$eventId/$mediaId.jpg"

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
