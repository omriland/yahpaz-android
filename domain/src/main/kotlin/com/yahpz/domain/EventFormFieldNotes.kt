package com.yahpz.domain

const val EVENT_TIMES_FIELD_NOTE =
    "שימו לב! מעתה הזנת זמנים תהיה עבור האירוע כולו ולא לכל מתנדב בנפרד"

const val EVENT_TIMES_FIELD_TOOLTIP =
    "זמן ההתחלה יהיה זמן היציאה של המתנדב הראשון וזמן הסיום יהיה זמן העזיבה של המתנדב האחרון"

const val PATROL_CALLSIGN_FIELD_NOTE =
    "אתם מתבקשים להזין או\"ק מלא של הניידת כולל קידומת (אביב, חוף וכו')"

const val LOCATION_FIELD_NOTE = "חדש! הזנת כביש באופן אוטומטי מבוסס על המיקום הנבחר"

const val LOCATION_FIELD_TOOLTIP =
    "מיקמנו את שדה 'מיקום' ראשון כדי להקל עליכם והטמענו הזנה אוטומטית של מספר הכביש. במקרה של כביש וק\"מ או מיקום שאינו נמצא, תוכלו עדין להזין מספר כביש באופן ידני"

enum class EventFormFieldNoteId {
    SHIFT_LEAD_ID,
    SECONDARY_LEADS,
    EVENT_DATE,
    POLICE_EVENT_ID,
    PATROL_CALLSIGN,
    PATROL_CALLSIGN_PREFIX,
    PATROL_CALLSIGN_NUMBER,
    EVENT_TIMES,
    STARTED_AT,
    ENDED_AT,
    DISTRICT_ID,
    STATION,
    EVENT_TYPE_ID,
    EVENT_TYPE_DETAIL,
    LOCATION,
    ROAD_ID,
    NOTES,
}

data class FieldNoteCopy(
    val note: String,
    val tooltip: String? = null,
)

val EVENT_FORM_FIELD_NOTES: Map<EventFormFieldNoteId, FieldNoteCopy> = mapOf(
    EventFormFieldNoteId.EVENT_TIMES to FieldNoteCopy(
        note = EVENT_TIMES_FIELD_NOTE,
        tooltip = EVENT_TIMES_FIELD_TOOLTIP,
    ),
    EventFormFieldNoteId.PATROL_CALLSIGN to FieldNoteCopy(
        note = PATROL_CALLSIGN_FIELD_NOTE,
    ),
    EventFormFieldNoteId.LOCATION to FieldNoteCopy(
        note = LOCATION_FIELD_NOTE,
        tooltip = LOCATION_FIELD_TOOLTIP,
    ),
)

fun eventFormFieldNote(id: EventFormFieldNoteId): FieldNoteCopy? = EVENT_FORM_FIELD_NOTES[id]
