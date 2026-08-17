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
    ShiftStatus.DRAFT -> StampDescriptor("טיוטה", StampTone.DRAFT)
    ShiftStatus.IN_PROGRESS -> StampDescriptor("במשמרת", StampTone.PENDING)
    ShiftStatus.CLOSED -> StampDescriptor("נסגרה", StampTone.DONE)
}

fun isShiftFuture(shiftDate: String, today: String): Boolean = shiftDate > today

fun isShiftPendingLog(
    shiftDate: String,
    odometerStart: Double?,
    odometerEnd: Double?,
    today: String,
): Boolean = !isShiftFuture(shiftDate, today) && (odometerStart == null || odometerEnd == null)

data class MineShiftItem(
    val id: String,
    val date: String,
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
        if (isShiftPendingLog(item.date, item.odometerStart, item.odometerEnd, today)) {
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

const val MINE_SHIFTS_PENDING_EMPTY = "מברוק! אין לך עוד משמרות לתעד כרגע"
const val MINE_SHIFTS_LOGGED_EMPTY = "אין משמרות שתועדו בתקופה זו"
const val MINE_SHIFTS_NONE = "אין משמרות עדיין"
