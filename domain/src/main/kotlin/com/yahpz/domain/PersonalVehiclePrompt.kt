package com.yahpz.domain

import java.net.URLEncoder

/*
 * After login, a מתנדב פעיל or חניכה ברכב פרטי with no active vehicle is asked for
 * their plate number and sent to support on WhatsApp (matches the web). Nothing is
 * written to the database.
 */

const val PERSONAL_VEHICLE_PROMPT_TITLE = "רכב פרטי"
const val PERSONAL_VEHICLE_PROMPT_BODY = "לא נמצא רכב המשוייך למשתמש שלך, ועל כן - לא ניתן להציב אותך לאירועים."
const val PERSONAL_VEHICLE_PROMPT_HINT = "יש להזין מספר רכב על מנת לבקש מהתמיכה להוסיף אותו"
const val PERSONAL_VEHICLE_PROMPT_FIELD = "מספר רכב"
const val PERSONAL_VEHICLE_PROMPT_EMPTY_ERROR = "יש להזין מספר רכב."
const val PERSONAL_VEHICLE_PROMPT_SEND = "שליחה לתמיכה"
const val PERSONAL_VEHICLE_PROMPT_CANCEL = "ביטול"

private val PROMPT_STATUSES = setOf(
    VolunteerStatus.ACTIVE_VOLUNTEER.raw,
    VolunteerStatus.PERSONAL_VEHICLE_TRAINING.raw,
)

fun needsPersonalVehiclePrompt(volunteerStatus: String?, activeVehicleCount: Int): Boolean =
    activeVehicleCount == 0 && volunteerStatus in PROMPT_STATUSES

fun personalVehicleWhatsAppMessage(vehicleNumber: String): String =
    "היי, אבקש להוסיף לפרופיל שלי את הרכב שלי מספר: $vehicleNumber"

/** Null when the number is blank. Spaces are %20, like the web's encodeURIComponent. */
fun personalVehicleWhatsAppHref(raw: String): String? {
    val number = raw.trim()
    if (number.isEmpty()) return null
    val text = URLEncoder.encode(personalVehicleWhatsAppMessage(number), "UTF-8").replace("+", "%20")
    return "$SUPPORT_WHATSAPP_HREF?text=$text"
}
