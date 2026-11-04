package com.yahpz.domain

/**
 * Shift-born events (`events.origin = 'shift'`): one shared report for the whole crew,
 * saved through `save_shift_born_event_fill`. Mirrors web `src/lib/shiftBornEvents.ts`
 * and `src/lib/shiftBornFill.ts`.
 */

const val SHIFT_ORIGIN = "shift"
const val SHIFT_BORN_CHIP = "ממשמרת"
const val SHIFT_BORN_FILL_TITLE = "תיעוד אירוע ממשמרת"
const val STALE_SAVE_MESSAGE = "מישהו שמר לפניך — רעננו"
const val COUNT_DECREASE_BLOCKED = "לא ניתן להקטין — קיימים אירועים שמולאו"
const val SHIFT_BORN_TOO_EARLY = "ניתן לתעד החל מתאריך המשמרת."
const val SHIFT_BORN_DONE_LOCKED = "התיעוד כבר הושלם. רק אחמ״ש יכול לערוך."
const val SHIFT_BORN_SAVE_FAILED = "שמירת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val SHIFT_BORN_NO_PERMISSION = "אין הרשאה"
const val SHIFT_BORN_ROAD_ERROR = "יש לבחור כביש"
const val SHIFT_BORN_LOCATION_ERROR = "יש להזין מיקום"
const val SHIFT_BORN_COMPLETE_FORM_ERROR = "יש להשלים את השדות המסומנים כדי לסיים את הדיווח."

data class ShiftBornEventSnapshot(
    val status: EventStatus,
    val policeEventId: String? = null,
    val treatmentDetail: String? = null,
    val treatmentNotes: String? = null,
    val roadId: String? = null,
    val location: String? = null,
    val treatedCount: Int = 0,
)

private fun blank(value: String?): Boolean = value.isNullOrBlank()

fun isShiftBornEventEmpty(event: ShiftBornEventSnapshot): Boolean =
    blank(event.policeEventId) &&
        blank(event.treatmentDetail) &&
        blank(event.treatmentNotes) &&
        blank(event.roadId) &&
        blank(event.location) &&
        event.treatedCount <= 0

/** Missing פירוט הטיפול always stays pending — never סיימת לתעד / הושלם. */
fun shiftBornFillStamp(event: ShiftBornEventSnapshot): StampDescriptor {
    if (blank(event.treatmentDetail)) return StampDescriptor("ממתין לתיעוד", StampTone.PENDING)
    if (event.status == EventStatus.DONE) return StampDescriptor("הושלם", StampTone.DONE)
    if (isShiftBornEventEmpty(event)) return StampDescriptor("ממתין לתיעוד", StampTone.DRAFT)
    return StampDescriptor("טיוטה נשמרה", StampTone.DRAFT)
}

/** Mine inbox: shift-born stays open until פירוט הטיפול is filled and status is done. */
fun mineShiftBornIsOpen(status: EventStatus, treatmentDetail: String?): Boolean {
    if (blank(treatmentDetail)) return true
    return status != EventStatus.DONE
}

/** Pending in the personal inbox: still open, and the shift date has come. */
fun mineShiftBornIsPending(
    status: EventStatus,
    treatmentDetail: String?,
    shiftDate: String,
    today: String,
): Boolean = !isShiftFuture(shiftDate, today) && mineShiftBornIsOpen(status, treatmentDetail)

/**
 * CTA on a shift-born card. It follows the shared report, not the viewer's own
 * participation row: a crew row marked done by an old per-responder fill must not hide
 * the way into a report that is still open.
 */
fun shiftBornFillCtaLabel(event: ShiftBornEventSnapshot): String? {
    if (!mineShiftBornIsOpen(event.status, event.treatmentDetail)) return null
    val started = !isShiftBornEventEmpty(event)
    return mineFillCtaLabel(if (started) ParticipationStatus.IN_PROGRESS else ParticipationStatus.PENDING)
}

fun lastSavedByLabel(name: String?): String? {
    val trimmed = name?.trim()
    if (trimmed.isNullOrEmpty()) return null
    return "נשמר ע״י $trimmed"
}

/** Lead/admin may document any date; crew only on or after the shift date. */
fun canDocumentShift(shiftDate: String, canManage: Boolean, today: String): Boolean =
    canManage || shiftDate <= today

/** Done is lead-only; before the shift date crew read only. */
fun shiftBornFillReadOnly(
    eventDone: Boolean,
    canManage: Boolean,
    shiftDate: String,
    today: String,
): Boolean = if (eventDone) !canManage else !canDocumentShift(shiftDate, canManage, today)

/** Why the report is read-only, in words the reader can act on; null when editable. */
fun shiftBornReadOnlyNotice(
    eventDone: Boolean,
    canManage: Boolean,
    shiftDate: String,
    today: String,
): String? = when {
    !shiftBornFillReadOnly(eventDone, canManage, shiftDate, today) -> null
    eventDone -> SHIFT_BORN_DONE_LOCKED
    else -> SHIFT_BORN_TOO_EARLY
}

data class ShiftBornTreated(val vehicleKindId: String, val quantity: Int)

data class ShiftBornFillDraft(
    val policeEventId: String = "",
    val treatmentDetail: String = "",
    val treatmentNotes: String = "",
    val roadId: String = "",
    val location: String = "",
    val treated: List<ShiftBornTreated> = emptyList(),
    val treatedPlates: List<TreatedPlate> = emptyList(),
)

data class ShiftBornFillErrors(
    val roadId: String? = null,
    val location: String? = null,
    val treatmentDetail: String? = null,
) {
    val isEmpty: Boolean get() = roadId == null && location == null && treatmentDetail == null
}

/**
 * Completion bar for the shared report: where it happened and what was done.
 * No end time, KM, plate or odometer — those never apply to a shift-born event.
 * A cancelled event keeps the place fields and has no פירוט minimum:
 * blank and short text are both valid.
 */
fun shiftBornCompleteErrors(draft: ShiftBornFillDraft, cancelled: Boolean = false): ShiftBornFillErrors {
    val detail = treatmentDetailError(draft.treatmentDetail, FillMode.COMPLETE, cancelled)
    return ShiftBornFillErrors(
        roadId = if (draft.roadId.isBlank()) SHIFT_BORN_ROAD_ERROR else null,
        location = if (draft.location.isBlank()) SHIFT_BORN_LOCATION_ERROR else null,
        treatmentDetail = detail,
    )
}

/** Counter step for רכבים שטופלו, clamped to 0..99; a zero row is dropped. */
fun bumpShiftBornTreated(
    treated: List<ShiftBornTreated>,
    vehicleKindId: String,
    delta: Int,
): List<ShiftBornTreated> {
    val previous = treated.firstOrNull { it.vehicleKindId == vehicleKindId }?.quantity ?: 0
    val quantity = (previous + delta).coerceIn(0, 99)
    val others = treated.filter { it.vehicleKindId != vehicleKindId }
    return if (quantity > 0) others + ShiftBornTreated(vehicleKindId, quantity) else others
}

/**
 * The RPC raises Hebrew messages the user can act on; show those as-is.
 * Anything else (network, unknown) gets the generic save failure.
 */
fun mapShiftBornFillError(message: String?): String {
    val raw = message.orEmpty()
    val known = listOf(
        STALE_SAVE_MESSAGE,
        COUNT_DECREASE_BLOCKED,
        SHIFT_BORN_TOO_EARLY,
        SHIFT_BORN_DONE_LOCKED,
        POLICE_EVENT_ID_DUPLICATE_ERROR,
    )
    known.firstOrNull { raw.contains(it) }?.let { return it }
    if (raw.contains(SHIFT_BORN_NO_PERMISSION)) return SHIFT_BORN_NO_PERMISSION
    return SHIFT_BORN_SAVE_FAILED
}
