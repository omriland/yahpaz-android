package com.yahpz.domain

data class ResponderFillDraft(
    val vehiclePlate: String = "",
    val odometerStart: String = "",
    val odometerEnd: String = "",
    val route: String = "",
    val treatmentDetail: String = "",
    val treatmentNotes: String = "",
    val treatedPlates: List<TreatedPlate> = emptyList(),
    val treatedPlatePending: String = "",
)

data class ResponderFillErrors(
    val vehiclePlate: String? = null,
    val odometerStart: String? = null,
    val odometerEnd: String? = null,
    val route: String? = null,
    val treatmentDetail: String? = null,
    val treatedPlates: String? = null,
    val eventMedia: String? = null,
    val form: String? = null,
) {
    val isEmpty: Boolean
        get() = vehiclePlate == null && odometerStart == null && odometerEnd == null &&
            route == null && treatmentDetail == null && treatedPlates == null &&
            eventMedia == null && form == null

    val firstMessage: String?
        get() = form ?: vehiclePlate ?: odometerStart ?: odometerEnd ?: route ?: treatmentDetail
            ?: treatedPlates ?: eventMedia
}

enum class FillMode { DRAFT, COMPLETE }

/** Minimum trimmed length for פירוט הטיפול on complete. Typing past this is allowed. */
const val TREATMENT_DETAIL_MIN_CHARS = 25
const val TREATMENT_DETAIL_MIN_ERROR = "יש להזין לפחות 25 תווים בפירוט הטיפול."

/** Complete-mode error. A cancelled event has no minimum; any other event still needs 25 trimmed characters. */
fun treatmentDetailError(value: String, mode: FillMode, cancelled: Boolean = false): String? {
    if (mode != FillMode.COMPLETE || cancelled) return null
    return if (value.trim().length < TREATMENT_DETAIL_MIN_CHARS) TREATMENT_DETAIL_MIN_ERROR else null
}

data class TreatmentDetailCounter(val text: String, val met: Boolean)

/** Compact counter above the field: trimmed length against the minimum. */
fun treatmentDetailCounter(value: String): TreatmentDetailCounter {
    val count = value.trim().length
    return TreatmentDetailCounter("$count מתוך $TREATMENT_DETAIL_MIN_CHARS", met = count >= TREATMENT_DETAIL_MIN_CHARS)
}

/**
 * An equal pair is a real record: a vehicle that never left the spot, or a
 * responder who logs 0 in both fields. Only a reversed pair is a typo.
 */
const val ODOMETER_ORDER_ERROR = "מד אוץ סיום אינו יכול להיות קטן ממד אוץ התחלה"

private sealed class ParsedNumber {
    data object Missing : ParsedNumber()
    data object Invalid : ParsedNumber()
    data class Value(val value: Double) : ParsedNumber()
}

private fun parseOptionalNumber(raw: String): ParsedNumber {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ParsedNumber.Missing
    val value = trimmed.toDoubleOrNull()
    return if (value != null && value.isFinite()) ParsedNumber.Value(value) else ParsedNumber.Invalid
}

/** Display-only end − start. Null until both readings are numbers and end ≥ start. */
fun odometerDelta(odometerStart: String, odometerEnd: String): Double? {
    val start = parseOptionalNumber(odometerStart) as? ParsedNumber.Value ?: return null
    val end = parseOptionalNumber(odometerEnd) as? ParsedNumber.Value ?: return null
    return if (end.value < start.value) null else end.value - start.value
}

/** The order error for a reversed pair of numbers, otherwise null. */
fun odometerRangeError(odometerStart: String, odometerEnd: String): String? {
    val start = parseOptionalNumber(odometerStart) as? ParsedNumber.Value ?: return null
    val end = parseOptionalNumber(odometerEnd) as? ParsedNumber.Value ?: return null
    return if (end.value < start.value) ODOMETER_ORDER_ERROR else null
}

fun validateResponderFillDraft(
    draft: ResponderFillDraft,
    mode: FillMode,
    allowedPlates: List<String> = emptyList(),
    totalKm: Double? = null,
    unfinishedMediaDraftCount: Int = 0,
    cancelled: Boolean = false,
): ResponderFillErrors {
    var vehiclePlate: String? = null
    var odometerStart: String? = null
    var odometerEnd: String? = null
    var route: String? = null
    var treatmentDetail: String? = null
    var treatedPlates: String? = null
    var eventMedia: String? = null
    val start = parseOptionalNumber(draft.odometerStart)
    val end = parseOptionalNumber(draft.odometerEnd)
    val plate = plateDigits(draft.vehiclePlate)
    val allowed = allowedPlates.map(::plateDigits).filter { it.isNotEmpty() }.toSet()

    if (start is ParsedNumber.Invalid) odometerStart = "מד אוץ התחלה חייב להיות מספר."
    if (end is ParsedNumber.Invalid) odometerEnd = "מד אוץ סיום חייב להיות מספר."

    if (mode == FillMode.COMPLETE) {
        if (plate.isEmpty()) {
            vehiclePlate = "יש לבחור רכב."
        } else if (allowed.isNotEmpty() && plate !in allowed) {
            vehiclePlate = "יש לבחור רכב מהרשימה המקושרת למשתמש."
        } else if (allowed.isEmpty()) {
            vehiclePlate = "לא מקושר רכב למשתמש. פנו למנהל המערכת."
        }
        if (start is ParsedNumber.Missing || start is ParsedNumber.Invalid) {
            odometerStart = "יש למלא מד אוץ התחלה."
        }
        if (end is ParsedNumber.Missing || end is ParsedNumber.Invalid) {
            odometerEnd = "יש למלא מד אוץ סיום."
        }
        if (draft.route.trim().isEmpty()) route = "יש למלא נתיב נסיעה."
        treatmentDetail = treatmentDetailError(draft.treatmentDetail, mode, cancelled)
    }

    if (odometerEnd == null && start is ParsedNumber.Value && end is ParsedNumber.Value && end.value < start.value) {
        odometerEnd = ODOMETER_ORDER_ERROR
    }

    leftoverTreatedPlateError(pending = draft.treatedPlatePending, mode = mode)?.let {
        treatedPlates = it
    }
    leftoverEventMediaError(unfinishedMediaDraftCount, mode)?.let {
        eventMedia = it
    }

    return ResponderFillErrors(
        vehiclePlate = vehiclePlate,
        odometerStart = odometerStart,
        odometerEnd = odometerEnd,
        route = route,
        treatmentDetail = treatmentDetail,
        treatedPlates = treatedPlates,
        eventMedia = eventMedia,
    )
}

fun parsedOdometer(raw: String): Double? = when (val parsed = parseOptionalNumber(raw)) {
    is ParsedNumber.Value -> parsed.value
    else -> null
}

/** Shown on the fill while the event is cancelled. The report itself stays editable. */
const val FILL_CANCELLED_NOTE = "האירוע בוטל."

enum class FillWriteGate { PROCEED, LOCKED, ALREADY_COMPLETE }

/** Completing an already-done assignment is success; drafts stay locked. */
fun gateResponderFillWrite(
    complete: Boolean,
    participationStatus: ParticipationStatus,
    eventStatus: EventStatus?,
): FillWriteGate {
    if (participationStatus == ParticipationStatus.DONE) {
        return if (complete) FillWriteGate.ALREADY_COMPLETE else FillWriteGate.LOCKED
    }
    if (eventStatus == EventStatus.DONE) return FillWriteGate.LOCKED
    return FillWriteGate.PROCEED
}
