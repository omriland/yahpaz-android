package com.yahpz.domain

const val VEHICLE_PHOTOS_TITLE = "לא צורפו תמונות לרכבים הבאים:"
const val VEHICLE_PHOTOS_BACK = "חזרה לעריכה"
const val VEHICLE_PHOTOS_PROCEED = "שמירה ללא תמונות נוספות"

data class PhotoCoveragePlate(
    val id: String,
    val plateDigits: String,
)

/**
 * Plate digits that already have at least one attached photo (any uploader).
 * [mediaPlateIds] holds each photo's linked treated-plate ids.
 */
fun coveredPlateDigits(
    mediaPlateIds: List<List<String>>,
    plates: List<PhotoCoveragePlate>,
): Set<String> {
    val digitsById = plates.associate { it.id to it.plateDigits }
    val covered = mutableSetOf<String>()
    for (plateIds in mediaPlateIds) {
        for (plateId in plateIds) {
            val digits = digitsById[plateId].orEmpty()
            if (digits.isNotEmpty()) covered += digits
        }
    }
    return covered
}

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

/**
 * Vehicles the "no photo for these vehicles" prompt names, or empty for no prompt.
 * [covered] is null when the coverage read failed: then nobody is told a vehicle
 * lacks a photo. A vehicle the person already skipped is not asked about again;
 * one added after that is.
 */
fun vehiclePhotosPrompt(
    plates: List<TreatedPlate>,
    covered: Set<String>?,
    skipped: Set<String> = emptySet(),
): List<TreatedPlate> {
    if (covered == null) return emptyList()
    return vehiclesMissingPhotos(plates, covered).filter { plateDigits(it.plateNumber) !in skipped }
}

/** Plate digits of the vehicles a photo prompt named, to remember once the person skips. */
fun skippedPhotoDigits(prompted: List<TreatedPlate>): List<String> =
    prompted.map { plateDigits(it.plateNumber) }.filter { it.isNotEmpty() }
