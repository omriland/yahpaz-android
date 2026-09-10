package com.yahpz.domain

const val VEHICLE_PHOTOS_TITLE = "לא צורפו תמונות לרכבים הבאים:"
const val VEHICLE_PHOTOS_BACK = "חזרה לעריכה"
const val VEHICLE_PHOTOS_PROCEED = "שמירה ללא תמונות נוספות"

data class PhotoCoveragePlate(
    val id: String,
    val plateDigits: String,
)

/** Plate digits that already have at least one attached photo (any uploader). */
fun coveredPlateDigits(
    media: List<EventMedia>,
    plates: List<PhotoCoveragePlate>,
): Set<String> {
    val digitsById = plates.associate { it.id to it.plateDigits }
    val covered = mutableSetOf<String>()
    for (item in media) {
        for (plateId in item.treatedPlateIds) {
            val digits = digitsById[plateId].orEmpty()
            if (digits.isNotEmpty()) covered += digits
        }
    }
    return covered
}

fun coveragePlatesFromOptions(options: List<EventMediaPlateOption>): List<PhotoCoveragePlate> =
    options.map { PhotoCoveragePlate(id = it.id, plateDigits = plateDigits(it.plateNumber)) }

/**
 * Vehicles on this responder's log that have no photo on the event.
 * A photo attached to several plates counts for each; another responder's
 * photo on the same plate number also counts.
 */
fun vehiclesMissingPhotos(
    myPlates: List<TreatedPlate>,
    coveredDigits: Set<String>,
): List<TreatedPlate> = myPlates.filter { plate ->
    val digits = plateDigits(plate.plateNumber)
    digits.isNotEmpty() && digits !in coveredDigits
}
