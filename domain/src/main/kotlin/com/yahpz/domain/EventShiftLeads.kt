package com.yahpz.domain

const val MAIN_LEAD_LABEL = "אחמ״ש ראשי"
const val MAIN_LEAD_LABEL_SHORT = "אחמ״ש"
const val SECONDARY_LEAD_LABEL = "אחמ״ש משני"
const val SECONDARY_LEAD_ADD = "הוספת אחמ״ש משני"
const val SECONDARY_LEAD_REMOVE = "הסרת אחמ״ש משני"
const val SECONDARY_LEAD_LOCKED_HINT = "נוסף אוטומטית בעריכה — לא ניתן להסיר"
const val SECONDARY_LEAD_PICKER_EMPTY = "אין אחמ״שים פעילים להוספה."
const val SECONDARY_LEAD_PICKER_NONE = "לא נמצאו אחמ״שים"
const val MAIN_LEAD_LOCKED_HINT = "רק מנהל יכול להחליף אחמ״ש ראשי לאחר יצירת האירוע."

data class SecondaryLead(
    val userId: String,
    val locked: Boolean = false,
    val fullName: String = "",
    val callsign: String = "",
    val addedAt: String? = null,
) {
    val display: String get() = formatLeadPerson(fullName, callsign)
}

fun canManageSecondaryLeads(roles: List<String>): Boolean {
    val s = roleSet(roles)
    return AppRole.SHIFT_LEAD in s || AppRole.ADMIN in s || AppRole.SUPER_ADMIN in s
}

fun canChangeEventMainLead(
    roles: List<String>,
    eventExists: Boolean,
    viewerIsCurrentMain: Boolean,
    hasSecondaries: Boolean,
): Boolean {
    if (isAdmin(roles)) return true
    if (AppRole.SHIFT_LEAD !in roleSet(roles)) return false
    if (!eventExists) return true
    return viewerIsCurrentMain && !hasSecondaries
}

fun canRemoveSecondaryLead(roles: List<String>, locked: Boolean): Boolean =
    !locked && canManageSecondaryLeads(roles)

fun shouldAutoLockSecondary(
    viewerId: String?,
    mainLeadId: String?,
    persistedFieldChange: Boolean,
    viewerHasShiftLead: Boolean,
): Boolean {
    if (!persistedFieldChange || !viewerHasShiftLead) return false
    val viewer = viewerId?.trim().orEmpty()
    val main = mainLeadId?.trim().orEmpty()
    return viewer.isNotEmpty() && main.isNotEmpty() && viewer != main
}

fun createTimeCreatorSecondary(creatorId: String, mainLeadId: String): SecondaryLead? {
    val creator = creatorId.trim()
    val main = mainLeadId.trim()
    if (creator.isEmpty() || main.isEmpty() || creator == main) return null
    return SecondaryLead(userId = creator, locked = false)
}

data class MainLeadReassignment(
    val mainId: String,
    val secondaries: List<SecondaryLead>,
)

fun reassignMainLeads(
    previousMainId: String,
    nextMainId: String,
    previousMainName: String,
    previousMainCallsign: String,
    secondaries: List<SecondaryLead>,
    previousMainLocked: Boolean = false,
): MainLeadReassignment {
    val previous = previousMainId.trim()
    val next = nextMainId.trim()
    if (next.isEmpty() || next == previous) {
        return MainLeadReassignment(mainId = previous, secondaries = secondaries)
    }
    val kept = secondaries.filter { it.userId != next && it.userId != previous }
    val already = secondaries.firstOrNull { it.userId == previous }
    val demoted = already?.copy(locked = already.locked || previousMainLocked)
        ?: SecondaryLead(
            userId = previous,
            locked = previousMainLocked,
            fullName = previousMainName,
            callsign = previousMainCallsign,
        )
    return MainLeadReassignment(mainId = next, secondaries = kept + demoted)
}

fun filterShiftLeadPicker(
    people: List<AssignableProfile>,
    excludeIds: Collection<String>,
    query: String,
): List<AssignableProfile> {
    val excluded = excludeIds.toSet()
    return filterAssignableProfiles(people.filter { it.id !in excluded }, query)
}

fun eventLeadFieldLabel(hasSecondaries: Boolean): String =
    if (hasSecondaries) MAIN_LEAD_LABEL else MAIN_LEAD_LABEL_SHORT

fun formatLeadPerson(fullName: String?, callsign: String?): String =
    listOfNotNull(
        fullName?.trim()?.takeIf { it.isNotEmpty() },
        callsign?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")

fun formatLeadsCaption(
    mainFullName: String?,
    mainCallsign: String?,
    secondaries: List<Pair<String?, String?>> = emptyList(),
): String {
    val parts = buildList {
        formatLeadPerson(mainFullName, mainCallsign).takeIf { it.isNotEmpty() }?.let { add(it) }
        secondaries.forEach { (name, callsign) ->
            formatLeadPerson(name, callsign).takeIf { it.isNotEmpty() }?.let { add(it) }
        }
    }
    return parts.joinToString(" · ")
}

/** Unit lists: main `שם · או״ק` only, plus ` +N` when secondaries exist. */
fun formatListLeadCaption(
    mainFullName: String?,
    mainCallsign: String?,
    secondaries: List<Pair<String?, String?>> = emptyList(),
): String {
    val main = formatLeadPerson(mainFullName, mainCallsign)
    if (main.isEmpty()) return ""
    val count = secondaries.size
    return if (count > 0) "$main +$count" else main
}

fun SecondaryLead.namePair(): Pair<String?, String?> = fullName to callsign

fun eventLeadsCaption(
    origin: String?,
    mainFullName: String?,
    mainCallsign: String?,
    secondaries: List<Pair<String?, String?>> = emptyList(),
): String {
    if (origin == "shift") return ""
    return formatListLeadCaption(mainFullName, mainCallsign, secondaries)
}
