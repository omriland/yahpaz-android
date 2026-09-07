package com.yahpz.domain

const val ASSIGNED_VOLUNTEER_EVENT_EDIT_ERROR =
    "לא ניתן לערוך אירוע עליו אתה מוצב כמתנדב. לעדכון פרטים יש לפנות לאחמ\"ש המזין או למנהל מערכת"
const val ASSIGNED_VOLUNTEER_EVENT_EDIT_CLOSE = "סגירה"

/**
 * True when the viewer has an event_responders row.
 * אחמ״ש משני is a co-lead, not a volunteer — that assignment must not block edit.
 * Role (including admin combo) does not bypass a real responder row.
 */
fun isAssignedVolunteerEventEditBlocked(
    viewerId: String?,
    responderIds: Collection<String?>,
    @Suppress("UNUSED_PARAMETER") secondaryLeadIds: Collection<String?> = emptyList(),
): Boolean {
    val viewer = viewerId?.trim().orEmpty()
    if (viewer.isEmpty()) return false
    return responderIds.any { it?.trim() == viewer }
}

fun EventDraft.blocksAssignedVolunteerEdit(viewerId: String?): Boolean =
    isAssignedVolunteerEventEditBlocked(
        viewerId,
        responders.map { it.responderId },
        secondaryLeads.map { it.userId },
    )
