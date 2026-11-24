package com.yahpz.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Hours of a live shift and the crew window around them. Mirrors web `src/lib/shiftWindow.ts`
 * and the hours helpers of `src/lib/shiftForm.ts`.
 */

enum class ShiftPhase { LEGACY, PLANNED, ACTIVE, FINISHING, CLOSED }

const val SHIFT_OVERLAP_GENERAL_MESSAGE = "המשמרת חופפת למשמרת אחרת של אחד מאנשי הצוות."

val CREW_GRACE: Duration = Duration.ofHours(48)

private val JERUSALEM_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")
private val TIME_OF_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class TimeRange(val start: Instant, val end: Instant)

data class ShiftHours(val start: String, val end: String)

data class ShiftWindow(val startsAt: Instant, val endsAt: Instant)

data class EditedShift(val shiftDate: String, val shiftKind: String)

data class OverlapKnownShift(
    val id: String,
    val shiftKind: String,
    val startsAt: Instant,
    val endsAt: Instant,
)

fun shiftWindowPhase(startsAt: Instant?, endsAt: Instant?, now: Instant): ShiftPhase {
    if (startsAt == null || endsAt == null) return ShiftPhase.LEGACY
    return when {
        now.isBefore(startsAt) -> ShiftPhase.PLANNED
        now.isBefore(endsAt) -> ShiftPhase.ACTIVE
        now.isBefore(endsAt.plus(CREW_GRACE)) -> ShiftPhase.FINISHING
        else -> ShiftPhase.CLOSED
    }
}

/** Legacy shifts have no window and keep the old date rules, so they answer false here. */
fun crewCanEdit(phase: ShiftPhase): Boolean =
    phase == ShiftPhase.ACTIVE || phase == ShiftPhase.FINISHING

fun rangesOverlap(first: TimeRange, second: TimeRange): Boolean =
    first.start.isBefore(second.end) && second.start.isBefore(first.end)

fun defaultShiftHours(kind: String): ShiftHours? = when (kind) {
    "morning" -> ShiftHours("07:00", "15:00")
    "midday" -> ShiftHours("15:00", "23:00")
    else -> null
}

/** An end earlier than the start lands on the next day. */
fun buildShiftWindow(shiftDate: String, startTime: String, endTime: String): ShiftWindow {
    val date = LocalDate.parse(shiftDate)
    val start = LocalTime.parse(startTime)
    val end = LocalTime.parse(endTime)
    val endDate = if (end.isBefore(start)) date.plusDays(1) else date
    return ShiftWindow(
        startsAt = toJerusalemInstant(date, start),
        endsAt = toJerusalemInstant(endDate, end),
    )
}

/**
 * The window to save after an edit. Unchanged date and kind keep it; a change rebuilds it from the
 * kind's default hours, or from the old time of day when the kind has none. A legacy shift stays legacy.
 */
fun windowAfterShiftEdit(
    old: EditedShift,
    newDate: String,
    newKind: String,
    oldWindow: ShiftWindow?,
): ShiftWindow? {
    if (oldWindow == null) return null
    if (old.shiftDate == newDate && old.shiftKind == newKind) return oldWindow
    val hours = defaultShiftHours(newKind) ?: ShiftHours(
        start = formatJerusalemTimeOfDay(oldWindow.startsAt),
        end = formatJerusalemTimeOfDay(oldWindow.endsAt),
    )
    return buildShiftWindow(newDate, hours.start, hours.end)
}

private fun toJerusalemInstant(date: LocalDate, time: LocalTime): Instant =
    LocalDateTime.of(date, time).atZone(JERUSALEM_ZONE).toInstant()

fun formatJerusalemTimeOfDay(instant: Instant): String =
    TIME_OF_DAY_FORMAT.format(instant.atZone(JERUSALEM_ZONE))

private fun overlapShiftLabel(shift: OverlapKnownShift): String {
    val hours = "${formatJerusalemTimeOfDay(shift.startsAt)}–${formatJerusalemTimeOfDay(shift.endsAt)}"
    return "${shiftKindLabel(shift.shiftKind)} באותן שעות ($hours)"
}

/** The database overlap refusal carries the other shift's id as its DETAIL. */
fun mapShiftOverlapError(otherShiftId: String?, knownShifts: List<OverlapKnownShift>): String {
    val other = knownShifts.firstOrNull { it.id == otherShiftId }
        ?: return SHIFT_OVERLAP_GENERAL_MESSAGE
    return "המשמרת חופפת למשמרת ${overlapShiftLabel(other)} של אחד מאנשי הצוות."
}

fun crewOverlapMessage(memberName: String, clash: OverlapKnownShift): String =
    "לא ניתן לשבץ את $memberName: כבר משובץ במשמרת ${overlapShiftLabel(clash)}."

data class CrewMember(val id: String, val name: String)

data class CrewLiveShift(
    val id: String,
    val shiftKind: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val responderIds: List<String>,
)

/** The refusal for the first crew member already on another live shift in these hours, else null. */
fun findCrewOverlapMessage(
    window: ShiftWindow,
    crew: List<CrewMember>,
    others: List<CrewLiveShift>,
    ignoreShiftId: String?,
): String? {
    val ownRange = TimeRange(window.startsAt, window.endsAt)
    val candidates = others.filter { it.id != ignoreShiftId }
    for (member in crew) {
        val clash = candidates.firstOrNull { other ->
            member.id in other.responderIds && rangesOverlap(ownRange, TimeRange(other.startsAt, other.endsAt))
        }
        if (clash != null) {
            return crewOverlapMessage(member.name, OverlapKnownShift(clash.id, clash.shiftKind, clash.startsAt, clash.endsAt))
        }
    }
    return null
}
