package com.yahpz.domain

import java.time.Duration
import java.time.Instant

/** The caller's live shift from `my_current_shift()`. Mirrors web `src/lib/currentShift.ts`. */

const val NEW_EVENT_SHEET_TITLE = "איזה אירוע?"
const val NEW_EVENT_SHEET_CLOSE = "סגירה"
const val NEW_EVENT_MORE_TYPES_TITLE = "סוגים נוספים"
const val NEW_EVENT_HINT =
    "לחיצה פותחת את האירוע ואת הדיווח. פתחת אירוע בטעות? ניתן למחוק אותו כל עוד לא הוזנו בו נתונים"

const val LIVE_SHIFT_NEW_EVENT_ACTION = "אירוע חדש"
const val LIVE_SHIFT_ODOMETER_ACTION = "קילומטרים"
const val ON_SHIFT_BAR_ACTION = "למשמרת"
const val LIVE_SHIFT_ACTIVE_STAMP = "במשמרת"
const val LIVE_SHIFT_PLANNED_STAMP = "מתוכננת"
/** Same label as the website gives an empty shift event, so the phone and the site read alike. */
const val LIVE_SHIFT_EMPTY_EVENT_STAMP = "ממתין לתיעוד"
const val LIVE_SHIFT_NO_EVENTS = "עוד לא נפתחו אירועים במשמרת."
const val LIVE_SHIFT_BACK = "חזרה"
const val LIVE_SHIFT_LOADING = "טוען משמרת…"
const val LIVE_SHIFT_RETRY = "רענון"
const val LIVE_SHIFT_UNNAMED_CREW_MEMBER = "מתנדב"
const val LIVE_SHIFT_LOAD_FAILED = "טעינת המשמרת נכשלה. בדקו את החיבור ונסו שוב."
const val LIVE_SHIFT_NOT_AVAILABLE = "אין לך הרשאה לצפות במשמרת זו או שהמשמרת אינה קיימת."
const val SHIFT_EVENT_DELETE_TITLE = "למחוק את האירוע?"
const val SHIFT_EVENT_DELETE_BODY = "האירוע עוד ריק, ולכן אפשר למחוק אותו."
const val SHIFT_EVENT_DELETE_CANCEL = "ביטול"

val NEW_EVENT_PRIMARY_TYPES = listOf("תאונה", "תקוע", "נטוש", "הפוך")

data class CurrentShift(
    val id: String,
    val shiftKind: String,
    val shiftDate: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val vehicleType: String,
)

data class NewEventTypeOrder<T>(val primary: List<T>, val more: List<T>)

private fun hoursLeftLabel(hours: Long): String =
    if (hours == 1L) "עוד שעה" else "עוד $hours שעות"

private const val MILLIS_PER_HOUR = 3_600_000L

private fun hoursLeftUntil(deadline: Instant, now: Instant): Long {
    val millisLeft = Duration.between(now, deadline).toMillis()
    return Math.floorDiv(millisLeft + MILLIS_PER_HOUR - 1, MILLIS_PER_HOUR)
}

/** Hours left while active, hours left to document while finishing, nothing otherwise. */
fun shiftTimeLeftLabel(phase: ShiftPhase, endsAt: Instant, now: Instant): String? = when (phase) {
    ShiftPhase.ACTIVE -> hoursLeftLabel(hoursLeftUntil(endsAt, now))
    ShiftPhase.FINISHING -> hoursLeftLabel(hoursLeftUntil(endsAt.plus(CREW_GRACE), now))
    else -> null
}

fun liveShiftStamp(phase: ShiftPhase, status: ShiftStatus): StampDescriptor = when (phase) {
    ShiftPhase.ACTIVE -> StampDescriptor(LIVE_SHIFT_ACTIVE_STAMP, StampTone.PARTIAL)
    ShiftPhase.PLANNED -> StampDescriptor(LIVE_SHIFT_PLANNED_STAMP, StampTone.DRAFT)
    else -> shiftStamp(status)
}

fun liveShiftEventStamp(empty: Boolean, done: Boolean): StampDescriptor = when {
    done -> StampDescriptor("הושלם", StampTone.DONE)
    empty -> StampDescriptor(LIVE_SHIFT_EMPTY_EVENT_STAMP, StampTone.DRAFT)
    else -> StampDescriptor("טיוטה נשמרה", StampTone.DRAFT)
}

fun liveShiftCrewTitle(count: Int): String = "מתנדבים ($count)"

fun liveShiftOpenedAtLabel(openedAt: Instant): String = "נפתח ב-${formatJerusalemTimeOfDay(openedAt)}"

fun liveShiftEventsTitle(count: Int): String = "אירועי המשמרת ($count)"

fun liveShiftEventTypeLabel(typeName: String): String =
    if (typeName.isEmpty()) "(משמרת)" else "$typeName (משמרת)"

fun barText(shift: CurrentShift, phase: ShiftPhase, now: Instant): String {
    if (phase == ShiftPhase.FINISHING) {
        return "המשמרת הסתיימה, אפשר להשלים תיעוד · ${hoursLeftLabel(hoursLeftUntil(shift.endsAt.plus(CREW_GRACE), now))}"
    }
    return "במשמרת ${shiftKindLabel(shift.shiftKind)} עכשיו · עד ${formatJerusalemTimeOfDay(shift.endsAt)}"
}

fun shouldOpenShiftScreen(phase: ShiftPhase, alreadyOpened: Boolean): Boolean =
    phase == ShiftPhase.ACTIVE && !alreadyOpened

fun showBar(phase: ShiftPhase, onShiftScreenOfThisShift: Boolean): Boolean =
    crewCanEdit(phase) && !onShiftScreenOfThisShift

/** Crew open events while the window is open; leads, admins and super admins in any phase. */
fun canCreateShiftEvent(canManage: Boolean, phase: ShiftPhase): Boolean =
    canManage || crewCanEdit(phase)

fun canDeleteShiftEvent(canManage: Boolean, phase: ShiftPhase, empty: Boolean, done: Boolean): Boolean {
    if (!empty) return false
    if (canManage) return true
    return crewCanEdit(phase) && !done
}

fun <T> orderEventTypesForNewEvent(types: List<T>, nameOf: (T) -> String): NewEventTypeOrder<T> {
    val primary = NEW_EVENT_PRIMARY_TYPES.mapNotNull { name -> types.firstOrNull { nameOf(it) == name } }
    val more = types.filter { it !in primary }
    return NewEventTypeOrder(primary, more)
}
