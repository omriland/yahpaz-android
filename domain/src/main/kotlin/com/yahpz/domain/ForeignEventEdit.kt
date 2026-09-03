package com.yahpz.domain

const val FOREIGN_EVENT_EDIT_BODY = "כל שינוי שתבצע יתועד ויישמר במערכת"
const val FOREIGN_EVENT_EDIT_CONFIRM = "עריכה"
const val FOREIGN_EVENT_EDIT_CANCEL = "ביטול"
const val FOREIGN_EVENT_EDIT_LEAD_FALLBACK = "אחמ״ש אחר"

fun isForeignShiftLeadEvent(viewerId: String?, shiftLeadId: String?): Boolean {
    val viewer = viewerId?.trim().orEmpty()
    val lead = shiftLeadId?.trim().orEmpty()
    return viewer.isNotEmpty() && lead.isNotEmpty() && viewer != lead
}

fun foreignEventEditLeadName(fullName: String?, callsign: String?): String {
    val name = fullName?.trim().orEmpty()
    if (name.isNotEmpty()) return name
    val sign = callsign?.trim().orEmpty()
    return sign.ifEmpty { FOREIGN_EVENT_EDIT_LEAD_FALLBACK }
}

fun foreignEventEditTitle(leadName: String): String =
    "האם אתה בטוח שברצונך לערוך אירוע שהוזן על ידי $leadName?"
