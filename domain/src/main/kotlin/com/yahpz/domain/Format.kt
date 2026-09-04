package com.yahpz.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun digitsOnly(value: String): String = value.filter { it.isDigit() }

fun plateDigits(value: String): String = digitsOnly(value)

fun formatPlate(raw: String): String {
    val digits = digitsOnly(raw)
    return when (digits.length) {
        7 -> "${digits.take(2)}-${digits.substring(2, 5)}-${digits.takeLast(2)}"
        8 -> "${digits.take(3)}-${digits.substring(3, 5)}-${digits.takeLast(3)}"
        else -> raw
    }
}

fun plateNumberForSave(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return formatPlate(trimmed)
}

/** Returns duplicated plate digits, or null when every non-empty plate is unique. */
fun findDuplicatePlate(plates: List<String>): String? {
    val seen = mutableSetOf<String>()
    for (plate in plates) {
        val digits = plateDigits(plate)
        if (digits.isEmpty()) continue
        if (!seen.add(digits)) return digits
    }
    return null
}

fun formatDate(value: String): String {
    val ymd = value.take(10)
    val parts = ymd.split("-")
    if (parts.size != 3) return value
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}

fun hebrewWeekdayLetter(value: String): String {
    val ymd = value.take(10)
    val parts = ymd.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return ""
    val date = LocalDate.of(parts[0], parts[1], parts[2])
    val letters = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ש")
    // iOS Calendar weekday: Sunday=1 … Saturday=7
    val iosWeekday = if (date.dayOfWeek.value == 7) 1 else date.dayOfWeek.value + 1
    return if (iosWeekday in 1..7) letters[iosWeekday] else ""
}

fun formatDateTime(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.Builder().setLanguage("he").setRegion("IL").build())
        .withZone(ZoneId.of("Asia/Jerusalem"))
    return formatter.format(instant)
}

/** `HH:mm` off a wall `timestamp` or ISO string, without shifting the zone. */
fun formatTime(value: String?): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val timePart = when {
        raw.contains('T') -> raw.substringAfter('T')
        raw.contains(' ') -> raw.substringAfter(' ')
        else -> raw
    }
    return timePart.take(5).takeIf { it.length == 5 }
}

/** Digits → `HH:mm` as the user types (colon after the hour). */
fun formatTimeInput(digits: String): String {
    val cleaned = digitsOnly(digits).take(4)
    val hour = cleaned.take(2)
    val minute = cleaned.drop(2)
    return listOf(hour, minute).filter { it.isNotEmpty() }.joinToString(":")
}

/** Same backspace semantics as the return-date field: deleting over `:` removes a digit. */
fun applyTimeKeystroke(previous: String, incoming: String): String {
    val previousDigits = digitsOnly(previous)
    var nextDigits = digitsOnly(incoming).take(4)
    if (nextDigits == previousDigits && incoming.length < previous.length && previousDigits.isNotEmpty()) {
        nextDigits = previousDigits.dropLast(1)
    }
    return formatTimeInput(nextDigits)
}

/** True when typing just completed a 4-digit clock (not when editing an already-full field). */
fun shouldAdvanceAfterTimeEntry(previous: String, next: String): Boolean =
    digitsOnly(previous).length < 4 && digitsOnly(next).length == 4

fun firstName(fullName: String): String = fullName.split(" ").firstOrNull() ?: fullName

/** Grouped decimal like the web `Intl.NumberFormat('he-IL')`: whole numbers lose the fraction. */
fun formatNumber(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) {
        String.format(Locale.US, "%,d", rounded.toLong())
    } else {
        String.format(Locale.US, "%,.1f", rounded)
    }
}

fun formatNumber(value: Int): String = String.format(Locale.US, "%,d", value)

/**
 * Road names sort with the urban road (`עירוני`, including legacy `עירוני (101)`)
 * pinned first, then pure numbers ascending, then names containing letters
 * by Hebrew name.
 */
fun compareRoadNames(left: String, right: String): Int {
    val leftUrban = left.contains("עירוני")
    val rightUrban = right.contains("עירוני")
    if (leftUrban != rightUrban) return if (leftUrban) -1 else 1
    val leftNumber = left.trim().toIntOrNull()
    val rightNumber = right.trim().toIntOrNull()
    if (leftNumber != null && rightNumber != null) return leftNumber - rightNumber
    if ((leftNumber == null) != (rightNumber == null)) return if (leftNumber != null) -1 else 1
    return left.trim().compareTo(right.trim())
}

fun <T> sortByRoadName(items: List<T>, name: (T) -> String): List<T> =
    items.sortedWith { left, right -> compareRoadNames(name(left), name(right)) }

fun passwordStrengthError(password: String): String? {
    val missing = mutableListOf<String>()
    if (password.length < 8) missing += "8 תווים לפחות"
    if (!password.contains(Regex("[A-Z]"))) missing += "אות גדולה"
    if (!password.contains(Regex("[^A-Za-z0-9]"))) missing += "תו מיוחד (למשל !)"
    if (missing.isEmpty()) return null
    return "הסיסמה אינה עומדת בדרישות. יש לכלול: ${formatHebrewList(missing)}."
}

fun formatHebrewList(items: List<String>): String = when (items.size) {
    1 -> items[0]
    2 -> "${items[0]} ו${items[1]}"
    else -> "${items.dropLast(1).joinToString(", ")} ו${items.last()}"
}

val SHIFT_KIND_LABELS = mapOf(
    "morning" to "בוקר",
    "midday" to "צהריים",
    "reinforcement" to "תגבור",
    "escort" to "ליווי",
    "other" to "אחר",
)

val VEHICLE_TYPE_LABELS = mapOf(
    "patrol_north" to "ניידת צפון",
    "patrol_center" to "ניידת מרכז",
    "personal" to "רכב פרטי",
)

fun addCalendarDays(ymd: String, days: Int): String {
    val parts = ymd.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return ymd
    return LocalDate.of(parts[0], parts[1], parts[2]).plusDays(days.toLong()).toString()
}
