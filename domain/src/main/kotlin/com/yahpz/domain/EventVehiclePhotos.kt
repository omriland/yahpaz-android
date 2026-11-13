package com.yahpz.domain

const val VEHICLE_PHOTOS_TITLE = "לא צורפו תמונות לרכבים הבאים:"
const val VEHICLE_PHOTOS_BACK = "חזרה לעריכה"
const val VEHICLE_PHOTOS_PROCEED = "שמירה ללא תמונות נוספות"

/**
 * Under the vehicle list when the person has photos with no vehicle: the photo was
 * saved, it just is not linked. Mirrors web `unlinkedPhotosNote`.
 */
fun unlinkedPhotosNote(count: Int): String? {
    if (count <= 0) return null
    val lead = if (count == 1) "יש לך תמונה אחת ללא שיוך לרכב." else "יש לך $count תמונות ללא שיוך לרכב."
    return "$lead כדי לשייך: חזרה לעריכה, פתיחת התמונה ובחירת הרכב."
}

/** The viewer's own photos that have no vehicle and are not marked as not-a-vehicle. */
fun unlinkedOwnPhotoCount(
    photos: List<Triple<String, Boolean, Int>>,
    viewerId: String?,
): Int = if (viewerId.isNullOrBlank()) 0 else photos.count { (uploadedBy, notVehicle, links) ->
    uploadedBy == viewerId && !notVehicle && links == 0
}

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
