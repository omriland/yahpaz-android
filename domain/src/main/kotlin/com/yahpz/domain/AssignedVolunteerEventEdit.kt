package com.yahpz.domain

const val ASSIGNED_VOLUNTEER_EVENT_EDIT_ERROR =
    "לא ניתן לערוך אירוע עליו אתה מוצב כמתנדב. לעדכון פרטים יש לפנות לאחמ\"ש המזין או למנהל מערכת"
const val ASSIGNED_VOLUNTEER_EVENT_EDIT_CLOSE = "סגירה"

/**
 * True when the viewer has an event_responders row or is a secondary אחמ״ש.
 * Role (including admin combo) does not bypass — they fill as a volunteer.
 */
fun isAssignedVolunteerEventEditBlocked(
    viewerId: String?,
    responderIds: Collection<String?>,
    secondaryLeadIds: Collection<String?>,
): Boolean {
    val viewer = viewerId?.trim().orEmpty()
    if (viewer.isEmpty()) return false
    if (responderIds.any { it?.trim() == viewer }) return true
    if (secondaryLeadIds.any { it?.trim() == viewer }) return true
    return false
}

fun EventDraft.blocksAssignedVolunteerEdit(viewerId: String?): Boolean =
    isAssignedVolunteerEventEditBlocked(
        viewerId,
        responders.map { it.responderId },
        secondaryLeads.map { it.userId },
    )
