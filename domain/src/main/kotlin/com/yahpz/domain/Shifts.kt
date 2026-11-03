package com.yahpz.domain

enum class ShiftStatus(val raw: String) {
    DRAFT("draft"),
    IN_PROGRESS("in_progress"),
    CLOSED("closed");

    companion object {
        fun fromRaw(raw: String?): ShiftStatus =
            entries.find { it.raw == raw } ?: DRAFT
    }
}

fun shiftStamp(status: ShiftStatus): StampDescriptor = when (status) {
    ShiftStatus.IN_PROGRESS -> StampDescriptor("פתוחה", StampTone.PENDING)
    ShiftStatus.DRAFT -> StampDescriptor("טיוטה", StampTone.DRAFT)
    ShiftStatus.CLOSED -> StampDescriptor("נסגרה", StampTone.DONE)
}

fun isShiftFuture(shiftDate: String, today: String): Boolean = shiftDate > today

fun isShiftPendingLog(
    shiftDate: String,
    status: ShiftStatus,
    today: String,
): Boolean = !isShiftFuture(shiftDate, today) && status != ShiftStatus.CLOSED

fun isShiftPendingLog(
    shiftDate: String,
    odometerStart: Double?,
    odometerEnd: Double?,
    today: String,
): Boolean = isShiftPendingLog(
    shiftDate,
    if (odometerStart != null && odometerEnd != null) ShiftStatus.CLOSED else ShiftStatus.IN_PROGRESS,
    today,
)

data class MineShiftItem(
    val id: String,
    val date: String,
    val status: ShiftStatus,
    val odometerStart: Double?,
    val odometerEnd: Double?,
)

data class MineShiftSections<T>(
    val pending: List<T>,
    val future: List<T>,
    val logged: List<T>,
    val hasMoreLogged: Boolean,
)

fun partitionMineShifts(
    items: List<MineShiftItem>,
    today: String,
    windowsLoaded: Int,
): MineShiftSections<MineShiftItem> {
    val start = loggedWindowStart(today, windowsLoaded)
    val pending = mutableListOf<MineShiftItem>()
    val future = mutableListOf<MineShiftItem>()
    val logged = mutableListOf<MineShiftItem>()
    var hasMoreLogged = false
    for (item in items) {
        if (isShiftFuture(item.date, today)) {
            future += item
            continue
        }
        if (isShiftPendingLog(item.date, item.status, today)) {
            pending += item
            continue
        }
        when {
            item.date >= start && item.date <= today -> logged += item
            item.date < start -> hasMoreLogged = true
        }
    }
    return MineShiftSections(
        pending = pending.sortedByDescending { it.date },
        future = future.sortedBy { it.date },
        logged = logged.sortedByDescending { it.date },
        hasMoreLogged = hasMoreLogged,
    )
}

/** Web `SHIFT_TOO_EARLY_MESSAGE`: crew document a shift from its date on. */
const val SHIFT_TOO_EARLY_MESSAGE = "ניתן לערוך החל מתאריך המשמרת"
const val SHIFT_FILL_TITLE = "תיעוד משמרת"
const val SHIFT_ODOMETER_SAVE_TITLE = "שמירת מד אוץ"
const val SHIFT_ODOMETER_SAVED = "מד האוץ נשמר"
const val SHIFT_ODOMETER_SAVE_FAILED = "שמירת מד האוץ נכשלה. בדקו את החיבור ונסו שוב."
const val SHIFT_ODOMETER_NOT_ALLOWED = "לא ניתן לעדכן את המשמרת הזו."

enum class ShiftFillButton { HIDDEN, ENABLED, TOO_EARLY }

/**
 * The card's תיעוד משמרת button, as on web `ShiftCard`: shown while the shift is
 * pending, and shown disabled before the shift date for crew (never for a lead).
 */
fun shiftFillButton(
    shiftDate: String,
    status: ShiftStatus,
    canManage: Boolean,
    today: String,
): ShiftFillButton = when {
    !canManage && isShiftFuture(shiftDate, today) -> ShiftFillButton.TOO_EARLY
    isShiftPendingLog(shiftDate, status, today) -> ShiftFillButton.ENABLED
    else -> ShiftFillButton.HIDDEN
}

/** The shift form's cross-field rule: either reading may be blank; only a reversed pair is refused. */
fun shiftOdometerError(odometerStart: String, odometerEnd: String): String? =
    odometerRangeError(odometerStart, odometerEnd)

const val MINE_SHIFTS_PENDING_EMPTY = "מברוק! אין לך עוד משמרות לתעד כרגע"
const val MINE_SHIFTS_LOGGED_EMPTY = "אין משמרות שתועדו בתקופה זו"
const val MINE_SHIFTS_NONE = "אין משמרות עדיין"
