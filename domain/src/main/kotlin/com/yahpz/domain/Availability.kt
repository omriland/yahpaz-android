package com.yahpz.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class AvailabilityStatus(val raw: String) {
    AVAILABLE("available"),
    UNAVAILABLE("unavailable");

    companion object {
        fun fromRaw(raw: String?): AvailabilityStatus =
            entries.find { it.raw == raw } ?: AVAILABLE
    }
}

val AVAILABILITY_LABELS = mapOf(
    AvailabilityStatus.AVAILABLE to "זמין",
    AvailabilityStatus.UNAVAILABLE to "לא זמין",
)

const val AVAILABILITY_DATE_ERROR = "יש לבחור תאריך עתידי"

fun availabilityLabel(status: AvailabilityStatus): String =
    AVAILABILITY_LABELS[status] ?: AVAILABILITY_LABELS.getValue(AvailabilityStatus.AVAILABLE)

fun availabilitySearchLabel(
    status: AvailabilityStatus,
    availableFrom: String?,
    today: String,
): String = availabilityLabel(effectiveAvailability(status, availableFrom, today))

fun israelToday(now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Jerusalem"))): String =
    now.format(DateTimeFormatter.ISO_LOCAL_DATE)

fun effectiveAvailability(
    status: AvailabilityStatus,
    availableFrom: String?,
    today: String,
): AvailabilityStatus {
    if (status == AvailabilityStatus.AVAILABLE) return AvailabilityStatus.AVAILABLE
    if (availableFrom != null && availableFrom <= today) return AvailabilityStatus.AVAILABLE
    return AvailabilityStatus.UNAVAILABLE
}

fun availabilityReturnCaption(availableFrom: String?): String? {
    if (availableFrom.isNullOrEmpty()) return null
    return "חזרה ב־${formatDate(availableFrom)}"
}

sealed class AvailabilityWrite {
    data class Ok(val availability: AvailabilityStatus, val availableFrom: String?) : AvailabilityWrite()
    data class Error(val message: String) : AvailabilityWrite()
}

fun buildAvailabilityWrite(
    status: AvailabilityStatus,
    availableFrom: String?,
    today: String,
): AvailabilityWrite {
    if (status == AvailabilityStatus.AVAILABLE) {
        return AvailabilityWrite.Ok(AvailabilityStatus.AVAILABLE, null)
    }
    val date = availableFrom?.trim().orEmpty()
    if (date.isEmpty()) return AvailabilityWrite.Ok(AvailabilityStatus.UNAVAILABLE, null)
    val iso = normalizeReturnDate(date) ?: return AvailabilityWrite.Error(AVAILABILITY_DATE_ERROR)
    if (iso <= today) return AvailabilityWrite.Error(AVAILABILITY_DATE_ERROR)
    return AvailabilityWrite.Ok(AvailabilityStatus.UNAVAILABLE, iso)
}

fun formatReturnDateInput(raw: String): String {
    val digits = digitsOnly(raw).take(8)
    val day = digits.take(2)
    val month = digits.drop(2).take(2)
    val year = digits.drop(4).take(4)
    return listOf(day, month, year).filter { it.isNotEmpty() }.joinToString("/")
}

fun applyReturnDateKeystroke(previous: String, incoming: String): String {
    val previousDigits = digitsOnly(previous)
    var nextDigits = digitsOnly(incoming).take(8)
    if (nextDigits == previousDigits && incoming.length < previous.length && previousDigits.isNotEmpty()) {
        nextDigits = previousDigits.dropLast(1)
    }
    return formatReturnDateInput(nextDigits)
}

fun returnDateToInput(stored: String): String {
    val trimmed = stored.trim()
    if (trimmed.isEmpty()) return ""
    val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").matchEntire(trimmed)
    if (iso != null) {
        return "${iso.groupValues[3]}/${iso.groupValues[2]}/${iso.groupValues[1]}"
    }
    return formatReturnDateInput(trimmed)
}

fun parseReturnDateInput(raw: String): String? {
    val digits = digitsOnly(raw)
    if (digits.length != 8) return null
    val day = digits.substring(0, 2).toIntOrNull() ?: return null
    val month = digits.substring(2, 4).toIntOrNull() ?: return null
    val year = digits.substring(4, 8).toIntOrNull() ?: return null
    val date = runCatching { java.time.LocalDate.of(year, month, day) }.getOrNull() ?: return null
    return date.toString()
}

fun normalizeReturnDate(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
        val parts = trimmed.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        return runCatching { java.time.LocalDate.of(parts[0], parts[1], parts[2]).toString() }.getOrNull()
    }
    return parseReturnDateInput(trimmed)
}

fun isValidReturnDate(availableFrom: String, today: String): Boolean {
    val iso = normalizeReturnDate(availableFrom) ?: return false
    return iso > today
}

fun tomorrowJerusalem(today: String = israelToday()): String = addCalendarDays(today, 1)
