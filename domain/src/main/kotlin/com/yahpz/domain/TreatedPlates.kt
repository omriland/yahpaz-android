package com.yahpz.domain

data class TreatedPlate(
    val plateNumber: String,
    val model: String? = null,
    val color: String? = null,
    val leftWhere: String? = null,
    val manufacturer: String? = null,
    val logoSlug: String? = null,
)

const val TREATED_PLATE_LENGTH_ERROR = "יש להזין 7 או 8 ספרות."
const val TREATED_PLATE_DUPLICATE_ERROR = "מספר זה כבר נוסף."
const val TREATED_PLATE_LEFTOVER_ERROR = "יש ללחוץ הוספה לשמירת המספר"

sealed class CommitTreatedPlateResult {
    data class Ok(val plate: TreatedPlate, val plates: List<TreatedPlate>) : CommitTreatedPlateResult()
    data class Error(val message: String) : CommitTreatedPlateResult()
}

fun treatedPlateCaption(model: String?, color: String?): String? {
    val nextModel = model?.trim().orEmpty()
    val nextColor = color?.trim().orEmpty()
    if (nextModel.isNotEmpty() && nextColor.isNotEmpty()) return "$nextModel · $nextColor"
    if (nextModel.isNotEmpty()) return nextModel
    if (nextColor.isNotEmpty()) return nextColor
    return null
}

fun commitTreatedPlate(
    pending: String,
    plates: List<TreatedPlate>,
): CommitTreatedPlateResult {
    val digits = plateDigits(pending)
    if (digits.length != 7 && digits.length != 8) {
        return CommitTreatedPlateResult.Error(TREATED_PLATE_LENGTH_ERROR)
    }
    if (plates.any { plateDigits(it.plateNumber) == digits }) {
        return CommitTreatedPlateResult.Error(TREATED_PLATE_DUPLICATE_ERROR)
    }
    val plate = TreatedPlate(plateNumber = formatPlate(digits))
    return CommitTreatedPlateResult.Ok(plate = plate, plates = plates + plate)
}

fun leftoverTreatedPlateError(pending: String, mode: FillMode): String? {
    if (mode != FillMode.COMPLETE) return null
    if (plateDigits(pending).isEmpty()) return null
    return TREATED_PLATE_LEFTOVER_ERROR
}

fun removeTreatedPlate(plates: List<TreatedPlate>, plateDigitsKey: String): List<TreatedPlate> {
    val key = plateDigits(plateDigitsKey)
    return plates.filter { plateDigits(it.plateNumber) != key }
}

fun setTreatedPlateLeftWhere(
    plates: List<TreatedPlate>,
    plateDigitsKey: String,
    leftWhere: String,
): List<TreatedPlate> {
    val key = plateDigits(plateDigitsKey)
    return plates.map { row ->
        if (plateDigits(row.plateNumber) != key) row
        else row.copy(leftWhere = leftWhere.ifEmpty { null })
    }
}

fun applyTreatedPlateLookup(
    plates: List<TreatedPlate>,
    plateDigitsKey: String,
    hit: PlateLookupHit,
): List<TreatedPlate> {
    val key = plateDigits(plateDigitsKey)
    val manufacturer = hit.manufacturer?.trim()?.takeIf { it.isNotEmpty() }
    return plates.map { row ->
        if (plateDigits(row.plateNumber) != key) row
        else row.copy(
            model = hit.model,
            color = hit.color,
            manufacturer = manufacturer,
            logoSlug = resolveCarLogoSlug(manufacturer),
        )
    }
}

data class TreatedPlateRowInput(
    val plateNumber: String? = null,
    val model: String? = null,
    val color: String? = null,
    val leftWhere: String? = null,
    val manufacturer: String? = null,
    val logoSlug: String? = null,
    val sortOrder: Int? = null,
)

/** Map DB rows (optional sort_order) into TreatedPlate[], ordered. */
fun mapTreatedPlateRows(rows: List<TreatedPlateRowInput>?): List<TreatedPlate> {
    return (rows ?: emptyList())
        .sortedBy { it.sortOrder ?: 0 }
        .mapNotNull { row ->
            val plateNumber = row.plateNumber?.trim().orEmpty()
            if (plateNumber.isEmpty()) null
            else {
                val manufacturer = row.manufacturer?.trim()?.takeIf { it.isNotEmpty() }
                val storedSlug = row.logoSlug?.trim()?.takeIf { it.isNotEmpty() }
                TreatedPlate(
                    plateNumber = plateNumber,
                    model = row.model,
                    color = row.color,
                    leftWhere = row.leftWhere?.trim()?.takeIf { it.isNotEmpty() },
                    manufacturer = manufacturer,
                    logoSlug = storedSlug ?: resolveCarLogoSlug(manufacturer),
                )
            }
        }
}
