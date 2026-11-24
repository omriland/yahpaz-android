package com.yahpz.domain

import java.time.Duration
import java.time.Instant

/** The caller's live shift from `my_current_shift()`. Mirrors web `src/lib/currentShift.ts`. */

const val NEW_EVENT_SHEET_TITLE = "איזה אירוע?"
const val NEW_EVENT_SHEET_CLOSE = "סגירה"
const val NEW_EVENT_MORE_TYPES_TITLE = "סוגים נוספים"
const val NEW_EVENT_HINT =
    "לחיצה פותחת את האירוע ואת הדיווח. פתחת אירוע בטעות? ניתן למחוק אותו כל עוד לא הוזנו בו נתונים"

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

private fun hoursLeftToDocument(endsAt: Instant, now: Instant): Long {
    val millisLeft = Duration.between(now, endsAt.plus(CREW_GRACE)).toMillis()
    return Math.floorDiv(millisLeft + MILLIS_PER_HOUR - 1, MILLIS_PER_HOUR)
}

private fun hoursLeftLabel(hours: Long): String =
    if (hours == 1L) "עוד שעה" else "עוד $hours שעות"

private const val MILLIS_PER_HOUR = 3_600_000L

fun barText(shift: CurrentShift, phase: ShiftPhase, now: Instant): String {
    if (phase == ShiftPhase.FINISHING) {
        return "המשמרת הסתיימה, אפשר להשלים תיעוד · ${hoursLeftLabel(hoursLeftToDocument(shift.endsAt, now))}"
    }
    return "במשמרת ${shiftKindLabel(shift.shiftKind)} עכשיו · עד ${formatJerusalemTimeOfDay(shift.endsAt)}"
}

fun shouldOpenShiftScreen(phase: ShiftPhase, alreadyOpened: Boolean): Boolean =
    phase == ShiftPhase.ACTIVE && !alreadyOpened

fun showBar(phase: ShiftPhase, onShiftScreenOfThisShift: Boolean): Boolean =
    crewCanEdit(phase) && !onShiftScreenOfThisShift

fun canDeleteShiftEvent(canManage: Boolean, phase: ShiftPhase, empty: Boolean, done: Boolean): Boolean {
    if (canManage) return true
    return crewCanEdit(phase) && empty && !done
}

fun <T> orderEventTypesForNewEvent(types: List<T>, nameOf: (T) -> String): NewEventTypeOrder<T> {
    val primary = NEW_EVENT_PRIMARY_TYPES.mapNotNull { name -> types.firstOrNull { nameOf(it) == name } }
    val more = types.filter { it !in primary }
    return NewEventTypeOrder(primary, more)
}
