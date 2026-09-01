package com.yahpz.domain

data class EventFreezeFlags(
    val frozenOver60km: Boolean = false,
    val frozenSuspiciousDuplicate: Boolean = false,
) {
    val isFrozen: Boolean get() = frozenOver60km || frozenSuspiciousDuplicate
    val countsTowardFuelRefund: Boolean get() = !isFrozen

    val tooltipHe: String?
        get() = when {
            frozenOver60km && frozenSuspiciousDuplicate ->
                "האירוע מוקפא בגלל חריגת קילומטרים (מעל 60 ק״מ) ובגלל חשד לאירוע כפול, וממתין לאישור מנהל."
            frozenOver60km ->
                "האירוע מוקפא בגלל חריגת קילומטרים (מעל 60 ק״מ) וממתין לאישור מנהל."
            frozenSuspiciousDuplicate ->
                "האירוע מוקפא בגלל חשד לאירוע כפול וממתין לאישור מנהל."
            else -> null
        }
}

fun computeFreezeFlags(
    matchesOver60km: Boolean,
    matchesSuspiciousDuplicate: Boolean,
    approvedOver60km: Boolean,
    approvedSuspiciousDuplicate: Boolean,
): EventFreezeFlags = EventFreezeFlags(
    frozenOver60km = matchesOver60km && !approvedOver60km,
    frozenSuspiciousDuplicate = matchesSuspiciousDuplicate && !approvedSuspiciousDuplicate,
)
