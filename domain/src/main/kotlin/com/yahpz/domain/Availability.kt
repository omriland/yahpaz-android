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

const val AVAILABILITY_DATE_ERROR = "בחרו תאריך מהמחר או השאירו ריק."

fun availabilityLabel(status: AvailabilityStatus): String =
    AVAILABILITY_LABELS[status] ?: AVAILABILITY_LABELS.getValue(AvailabilityStatus.AVAILABLE)

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
    if (!isValidReturnDate(date, today)) return AvailabilityWrite.Error(AVAILABILITY_DATE_ERROR)
    return AvailabilityWrite.Ok(AvailabilityStatus.UNAVAILABLE, date)
}

fun isValidReturnDate(availableFrom: String, today: String): Boolean =
    availableFrom.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) && availableFrom > today

fun tomorrowJerusalem(today: String = israelToday()): String = addCalendarDays(today, 1)
