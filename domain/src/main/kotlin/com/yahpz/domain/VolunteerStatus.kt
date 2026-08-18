package com.yahpz.domain

enum class VolunteerStatus(val raw: String) {
    ADMINISTRATION("administration"),
    BASIC_TRAINING("basic_training"),
    PHONE_TRAINING("phone_training"),
    PERSONAL_VEHICLE_TRAINING("personal_vehicle_training"),
    SHIFTS_ONLY("shifts_only"),
    ACTIVE_VOLUNTEER("active_volunteer");

    companion object {
        val DEFAULT = ACTIVE_VOLUNTEER

        fun fromRaw(raw: String?): VolunteerStatus = entries.find { it.raw == raw } ?: DEFAULT
    }
}

val VOLUNTEER_STATUS_LABELS = mapOf(
    VolunteerStatus.ADMINISTRATION to "מנהלה",
    VolunteerStatus.BASIC_TRAINING to "חניכה בסיסית",
    VolunteerStatus.PHONE_TRAINING to "חניכה טלפונית",
    VolunteerStatus.PERSONAL_VEHICLE_TRAINING to "חניכה ברכב פרטי",
    VolunteerStatus.SHIFTS_ONLY to "משמרות בלבד",
    VolunteerStatus.ACTIVE_VOLUNTEER to "מתנדב פעיל",
)

fun volunteerStatusLabel(raw: String?): String =
    VOLUNTEER_STATUS_LABELS.getValue(VolunteerStatus.fromRaw(raw))
