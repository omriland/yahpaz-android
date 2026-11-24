package com.yahpz.domain

const val SHIFT_EVENT_DELETE_NOT_EMPTY = "ניתן למחוק רק אירוע שעדיין לא הוזנו בו נתונים."
const val SHIFT_EVENT_DELETE_DONE = "לא ניתן למחוק אירוע שהושלם."
const val SHIFT_EVENT_REOPEN_FORBIDDEN = "לא ניתן לפתוח מחדש את האירוע."
const val SHIFT_NOT_LIVE = "המשמרת אינה משמרת חיה."
const val SHIFT_EVENT_CREATE_FAILED = "פתיחת האירוע נכשלה. בדקו את החיבור ונסו שוב."
const val SHIFT_EVENT_DELETE_FAILED = "מחיקת האירוע נכשלה. בדקו את החיבור ונסו שוב."

/** Database text that marks an overlap refusal; its DETAIL is the other shift's id. */
const val SHIFT_OVERLAP_DB_MESSAGE = "המשמרת חופפת"

private val LIVE_SHIFT_SERVER_MESSAGES = listOf(
    SHIFT_NOT_STARTED,
    SHIFT_WINDOW_CLOSED,
    SHIFT_EVENT_DELETE_NOT_EMPTY,
    SHIFT_EVENT_DELETE_DONE,
    SHIFT_EVENT_REOPEN_FORBIDDEN,
    SHIFT_NOT_LIVE,
)

/** The server's Hebrew refusal for a live shift when the raw message holds one, else null. */
fun knownLiveShiftMessage(rawMessage: String?): String? {
    val raw = rawMessage.orEmpty()
    return LIVE_SHIFT_SERVER_MESSAGES.firstOrNull { raw.contains(it) }
}

fun isShiftOverlapRefusal(rawMessage: String?): Boolean =
    rawMessage.orEmpty().contains(SHIFT_OVERLAP_DB_MESSAGE)
