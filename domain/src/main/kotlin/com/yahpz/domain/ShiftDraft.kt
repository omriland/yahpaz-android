package com.yahpz.domain

const val SHIFT_DRAFT_DATE_ERROR = "יש לבחור תאריך"
const val SHIFT_DRAFT_KIND_ERROR = "יש לבחור שם משמרת"
const val SHIFT_DRAFT_VEHICLE_ERROR = "יש לבחור סוג רכב"
const val SHIFT_DRAFT_CREW_ERROR = "יש לשבץ בין כונן אחד לשלושה"
const val SHIFT_DRAFT_PLATE_ERROR = "יש לבחור לוחית לרכב פרטי"
const val SHIFT_DRAFT_FORM_ERROR = "יש למלא תאריך, שם משמרת וסוג רכב לפני השמירה."
const val SHIFT_DRAFT_SAVE_FAILED = "שמירת המשמרת נכשלה. בדקו את החיבור ונסו שוב."
const val SHIFT_DRAFT_SAVED = "המשמרת נשמרה."
const val SHIFT_NEW_TITLE = "משמרת חדשה"
const val SHIFT_EDIT_TITLE = "עריכת משמרת"
const val SHIFT_SAVE_TITLE = "שמירה"
const val SHIFT_ASSIGN_OPEN = "שיבוץ כוננים"
const val SHIFT_ASSIGN_CLOSE = "סגירת שיבוץ"
const val SHIFT_ASSIGN_EMPTY = "יש לשבץ כוננים למשמרת."
const val SHIFT_EDIT_LOAD_FAILED = "טעינת המשמרת נכשלה. בדקו את החיבור ונסו שוב."
const val UNIT_SHIFTS_LOAD_FAILED = "טעינת המשמרות נכשלה. בדקו את החיבור ונסו שוב."

const val SHIFT_CREW_MIN = 1
const val SHIFT_CREW_MAX = 3

/** Order matches the web `SHIFT_KIND_OPTIONS`. */
val SHIFT_KIND_ORDER = listOf("morning", "midday", "reinforcement", "escort", "other")

/**
 * Patrol vehicles are always offered. Personal is added only when a selected crew
 * member has a plate in the vehicles lookup — same gate as the web form.
 */
val SHIFT_VEHICLE_TYPE_ORDER = listOf("patrol_north", "patrol_center")

fun offeredShiftVehicleTypes(includePersonal: Boolean): List<String> =
    if (includePersonal) SHIFT_VEHICLE_TYPE_ORDER + "personal" else SHIFT_VEHICLE_TYPE_ORDER

fun shiftKindLabel(kind: String): String = SHIFT_KIND_LABELS[kind] ?: kind

fun shiftVehicleTypeLabel(vehicleType: String): String = VEHICLE_TYPE_LABELS[vehicleType] ?: vehicleType

fun crewVehicleLabel(plateNumber: String, model: String?): String {
    val plate = formatPlate(plateNumber)
    val modelText = model?.trim().orEmpty()
    return if (modelText.isEmpty()) plate else "$plate · $modelText"
}

/** Drop a personal plate that no longer belongs to the assigned crew. */
fun keepPersonalVehicleId(selectedId: String?, availableIds: Set<String>): String? =
    selectedId?.takeIf { it in availableIds }

data class ShiftDraft(
    val shiftDate: String,
    val shiftKind: String = "",
    val vehicleType: String = "",
    val notes: String = "",
    val responderIds: List<String> = emptyList(),
    val personalVehicleId: String? = null,
)

data class ShiftDraftErrors(
    val shiftDate: String? = null,
    val shiftKind: String? = null,
    val vehicleType: String? = null,
    val crew: String? = null,
    val plate: String? = null,
) {
    val isEmpty: Boolean
        get() = shiftDate == null && shiftKind == null && vehicleType == null && crew == null && plate == null

    val formMessage: String?
        get() = when {
            isEmpty -> null
            crew != null && shiftDate == null && shiftKind == null && vehicleType == null && plate == null ->
                SHIFT_DRAFT_CREW_ERROR
            else -> SHIFT_DRAFT_FORM_ERROR
        }
}

/** Minimal save gate: date + shift kind + vehicle type + one to three crew (+ plate when private). */
fun validateShiftDraft(draft: ShiftDraft): ShiftDraftErrors = ShiftDraftErrors(
    shiftDate = if (normalizeReturnDate(draft.shiftDate) == null) SHIFT_DRAFT_DATE_ERROR else null,
    shiftKind = if (draft.shiftKind.isEmpty()) SHIFT_DRAFT_KIND_ERROR else null,
    vehicleType = if (draft.vehicleType.isEmpty()) SHIFT_DRAFT_VEHICLE_ERROR else null,
    plate = if (draft.vehicleType == "personal" && draft.personalVehicleId.isNullOrEmpty()) {
        SHIFT_DRAFT_PLATE_ERROR
    } else {
        null
    },
    crew = if (draft.responderIds.size !in SHIFT_CREW_MIN..SHIFT_CREW_MAX) SHIFT_DRAFT_CREW_ERROR else null,
)

fun shiftCrewSummary(count: Int): String = when (count) {
    0 -> "טרם שובצו כוננים"
    1 -> "כונן אחד משובץ"
    else -> "$count כוננים משובצים"
}

/** Selecting past the crew ceiling is ignored rather than silently dropping someone else. */
fun toggleCrewSelection(selected: List<String>, responderId: String): List<String> {
    if (selected.contains(responderId)) return selected.filterNot { it == responderId }
    if (selected.size >= SHIFT_CREW_MAX) return selected
    return selected + responderId
}
