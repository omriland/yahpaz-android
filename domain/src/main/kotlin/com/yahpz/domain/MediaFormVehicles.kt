package com.yahpz.domain

/**
 * Photos and the vehicles typed into the form being filled. Mirrors web
 * EventMediaGallery (formVehicles, pending links) and eventMedia.ts.
 *
 * A vehicle added to the form has no saved row until the report is saved, and
 * every save replaces the rows with new ids. So in a form, a chip is keyed by the
 * plate digits; the row id is looked up only when a photo is linked.
 */

const val EVENT_MEDIA_PENDING_VEHICLE = "הרכב עוד לא נשמר. התמונה תשויך אליו כשתשמרו את הדיווח."

private const val FORM_VEHICLE_KEY_PREFIX = "plate:"

sealed interface MediaPlateOwner {
    data class Responder(val eventResponderId: String) : MediaPlateOwner
    data class Event(val eventId: String) : MediaPlateOwner
}

/** A photo's vehicles that had no saved row when it was linked. */
data class PendingMediaLink(val mediaId: String, val digits: List<String>)

data class ResolvedPlates(val ids: List<String>, val missing: List<String>)

fun formVehicleKey(digits: String): String = FORM_VEHICLE_KEY_PREFIX + digits

fun formVehicleDigits(key: String): String? =
    if (key.startsWith(FORM_VEHICLE_KEY_PREFIX)) key.removePrefix(FORM_VEHICLE_KEY_PREFIX) else null

/** The form's vehicles as chip options, one per plate; without a form, the saved rows. */
fun mediaVehicleOptions(
    formVehicles: List<TreatedPlate>?,
    saved: List<EventMediaPlateOption>,
): List<EventMediaPlateOption> {
    if (formVehicles == null) return saved
    val seen = mutableSetOf<String>()
    return formVehicles.mapNotNull { vehicle ->
        val digits = plateDigits(vehicle.plateNumber)
        if (digits.isEmpty() || !seen.add(digits)) return@mapNotNull null
        EventMediaPlateOption(
            id = formVehicleKey(digits),
            plateNumber = vehicle.plateNumber,
            model = vehicle.model,
            color = vehicle.color,
            logoSlug = vehicle.logoSlug,
        )
    }
}

private fun ownsPlate(plate: EventMediaPlateOption, owner: MediaPlateOwner?): Boolean = when (owner) {
    null -> true
    is MediaPlateOwner.Responder -> plate.eventResponderId == owner.eventResponderId
    is MediaPlateOwner.Event -> plate.eventId == owner.eventId
}

/** Saved rows for the digits; digits with no saved row of this owner come back as missing. */
fun resolvePlateDigits(
    plates: List<EventMediaPlateOption>,
    digits: List<String>,
    owner: MediaPlateOwner?,
): ResolvedPlates {
    val ids = mutableListOf<String>()
    val missing = mutableListOf<String>()
    for (wanted in digits.distinct()) {
        val hit = plates.firstOrNull { plateDigits(it.plateNumber) == wanted && ownsPlate(it, owner) }
        if (hit != null) ids += hit.id else missing += wanted
    }
    return ResolvedPlates(ids, missing)
}

/** Chosen chips to the saved row ids to link now and the digits still waiting for a save. */
fun resolveMediaPlateChoice(
    chosen: List<String>,
    saved: List<EventMediaPlateOption>,
    owner: MediaPlateOwner?,
): ResolvedPlates {
    val rowIds = chosen.filter { formVehicleDigits(it) == null }
    val resolved = resolvePlateDigits(saved, chosen.mapNotNull(::formVehicleDigits), owner)
    return ResolvedPlates(uniquePlateIds(rowIds + resolved.ids), resolved.missing)
}

/** A saved photo's vehicles as chip keys: its linked rows plus the digits still pending. */
fun mediaChoiceKeys(
    linkedIds: List<String>,
    saved: List<EventMediaPlateOption>,
    pendingDigits: List<String>,
    formMode: Boolean,
): List<String> {
    if (!formMode) return linkedIds
    val digitsById = saved.associate { it.id to plateDigits(it.plateNumber) }
    val digits = linkedIds.mapNotNull { id -> digitsById[id]?.takeIf { it.isNotEmpty() } } + pendingDigits
    return digits.distinct().map(::formVehicleKey)
}

/** Keeps only the chosen chips that are still offered (a vehicle removed from the form drops out). */
fun keepOfferedChoices(chosen: List<String>, options: List<EventMediaPlateOption>): List<String> {
    val offered = options.map { it.id }.toSet()
    return chosen.filter { it in offered }
}

/** The pending links for vehicles still in the form. */
fun pendingLinksInForm(
    pending: Map<String, List<String>>,
    options: List<EventMediaPlateOption>,
): List<PendingMediaLink> {
    val inForm = options.mapNotNull { formVehicleDigits(it.id) }.toSet()
    return pending.mapNotNull { (mediaId, digits) ->
        val kept = digits.filter { it in inForm }
        if (kept.isEmpty()) null else PendingMediaLink(mediaId, kept)
    }
}

/** After the form saved and photos were re-read: drops the pending digits that are now linked. */
fun prunePendingLinks(
    pending: Map<String, List<String>>,
    items: List<EventMedia>,
    saved: List<EventMediaPlateOption>,
): Map<String, List<String>> {
    val digitsById = saved.associate { it.id to plateDigits(it.plateNumber) }
    val out = mutableMapOf<String, List<String>>()
    for ((mediaId, digits) in pending) {
        val item = items.firstOrNull { it.id == mediaId } ?: continue
        val linked = item.treatedPlateIds.mapNotNull { digitsById[it] }.toSet()
        val left = digits.filter { it !in linked }
        if (left.isNotEmpty()) out[mediaId] = left
    }
    return out
}

/** Saved photo coverage plus photos linked to vehicles the form has not saved yet. Null stays null. */
fun withPendingPhotoLinks(saved: Set<String>?, pending: List<PendingMediaLink>): Set<String>? {
    if (saved == null) return null
    val covered = saved.toMutableSet()
    for (link in pending) {
        for (digits in link.digits) {
            val key = plateDigits(digits)
            if (key.isNotEmpty()) covered += key
        }
    }
    return covered
}
