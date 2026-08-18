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
    val form: String? = null,
) {
    val isEmpty: Boolean
        get() = vehiclePlate == null && odometerStart == null && odometerEnd == null &&
            route == null && treatmentDetail == null && treatedPlates == null && form == null

    val firstMessage: String?
        get() = form ?: vehiclePlate ?: odometerStart ?: odometerEnd ?: route ?: treatmentDetail
            ?: treatedPlates
}

enum class FillMode { DRAFT, COMPLETE }

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

fun validateResponderFillDraft(
    draft: ResponderFillDraft,
    mode: FillMode,
    allowedPlates: List<String> = emptyList(),
    totalKm: Double? = null,
): ResponderFillErrors {
    var vehiclePlate: String? = null
    var odometerStart: String? = null
    var odometerEnd: String? = null
    var route: String? = null
    var treatmentDetail: String? = null
    var treatedPlates: String? = null
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
        if (draft.treatmentDetail.trim().isEmpty()) treatmentDetail = "יש למלא פירוט הטיפול."
    }

    if (odometerEnd == null && start is ParsedNumber.Value && end is ParsedNumber.Value && end.value <= start.value) {
        odometerEnd = "מד אוץ סיום חייב להיות גדול ממד אוץ התחלה"
    }

    leftoverTreatedPlateError(pending = draft.treatedPlatePending, mode = mode)?.let {
        treatedPlates = it
    }

    return ResponderFillErrors(
        vehiclePlate = vehiclePlate,
        odometerStart = odometerStart,
        odometerEnd = odometerEnd,
        route = route,
        treatmentDetail = treatmentDetail,
        treatedPlates = treatedPlates,
    )
}

fun parsedOdometer(raw: String): Double? = when (val parsed = parseOptionalNumber(raw)) {
    is ParsedNumber.Value -> parsed.value
    else -> null
}
