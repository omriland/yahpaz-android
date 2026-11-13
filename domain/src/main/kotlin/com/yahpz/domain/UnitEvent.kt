package com.yahpz.domain

// אירוע יחידתי copy. Mirrors web src/lib/eventForm.ts.
const val UNIT_EVENT_TOGGLE_LABEL = "אירוע יחידתי"
const val UNIT_EVENT_TIP = "אירוע פנימי ללא מספר אירוע משטרתי. האירוע יקבל מזהה ייחודי ליחידה"
const val UNIT_EVENT_ID_LABEL = "מזהה יחידתי"
const val UNIT_EVENT_ISSUE_ERROR = "לא ניתן היה להנפיק מזהה יחידתי. בדקו את החיבור ונסו שוב."

/**
 * The switch shows to a lead or admin on a form opened to create an event, and on
 * any form whose event already holds a unit ID (so it can go back to a police number).
 */
fun showUnitEventToggle(canManage: Boolean, openedToCreate: Boolean, policeEventId: String): Boolean =
    canManage && (isUnitEventId(policeEventId) || (openedToCreate && policeEventId.isBlank()))
