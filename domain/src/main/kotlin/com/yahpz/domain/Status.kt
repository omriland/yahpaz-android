package com.yahpz.domain

enum class EventStatus(val raw: String) {
    DRAFT("draft"),
    IN_PROGRESS("in_progress"),
    PARTIAL("partial"),
    DONE("done");

    companion object {
        fun fromRaw(raw: String?): EventStatus =
            entries.find { it.raw == raw } ?: IN_PROGRESS
    }
}

enum class ParticipationStatus(val raw: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    DONE("done");

    companion object {
        fun fromRaw(raw: String?): ParticipationStatus =
            entries.find { it.raw == raw } ?: PENDING
    }
}

enum class StampTone {
    DONE, PARTIAL, PENDING, DRAFT, ALERT
}

data class StampDescriptor(val label: String, val tone: StampTone)

const val MISSING_KM_STAMP_LABEL = "חסר ק״מ"

fun eventStamp(status: EventStatus): StampDescriptor = when (status) {
    EventStatus.DRAFT -> StampDescriptor("אירוע בהזנה", StampTone.DRAFT)
    EventStatus.IN_PROGRESS -> StampDescriptor("ממתין לתיעוד", StampTone.PENDING)
    EventStatus.PARTIAL -> StampDescriptor("תועד חלקית", StampTone.PARTIAL)
    EventStatus.DONE -> StampDescriptor("הושלם", StampTone.DONE)
}

/**
 * Viewer-relative documentation stamp for אחמ״ש lists.
 * Does not change event/participation status — only the lead-facing label.
 */
fun reportingDocumentationStamp(status: EventStatus, missingKm: Boolean): StampDescriptor =
    overlayMissingKmOnDoneStamp(eventStamp(status), missingKm)

fun overlayMissingKmOnDoneStamp(stamp: StampDescriptor, missingKm: Boolean): StampDescriptor =
    if (missingKm && stamp.tone == StampTone.DONE && stamp.label == "הושלם") {
        StampDescriptor(MISSING_KM_STAMP_LABEL, StampTone.ALERT)
    } else {
        stamp
    }

fun cancelledStamp(): StampDescriptor = StampDescriptor("בוטל", StampTone.DRAFT)

fun participationStamp(status: ParticipationStatus, isViewer: Boolean): StampDescriptor {
    if (status == ParticipationStatus.DONE) return StampDescriptor("הושלם", StampTone.DONE)
    if (status == ParticipationStatus.IN_PROGRESS && isViewer) {
        return StampDescriptor("טיוטה נשמרה", StampTone.DRAFT)
    }
    return StampDescriptor(if (isViewer) "ממתין לתיעוד" else "ממתין למתנדב", StampTone.PENDING)
}

/** Responder finished fill; lead KM still missing. Replaces הושלם on their own stamp. */
const val FILL_DONE_AWAITING_KM_LABEL = "סיימת לתעד"

/** Responder-facing: they finished; the lead has not entered KM yet. */
const val LEAD_KM_PENDING_NOTE = "אחמ״ש טרם הזין ק״מ"

fun leadKmPendingNote(participation: ParticipationStatus?, totalKm: Double?): String? =
    if (participation == ParticipationStatus.DONE && totalKm == null) LEAD_KM_PENDING_NOTE else null

/** Own-row stamp for mine inbox / fill / detail: סיימת לתעד when lead KM is still null. */
fun mineParticipationStamp(status: ParticipationStatus?, totalKm: Double?): StampDescriptor {
    val resolved = status ?: ParticipationStatus.PENDING
    if (resolved == ParticipationStatus.DONE && totalKm == null) {
        return StampDescriptor(FILL_DONE_AWAITING_KM_LABEL, StampTone.DONE)
    }
    return participationStamp(resolved, true)
}

/** Mine inbox: fill still open, or fill done but lead KM is missing. */
fun mineInboxIsOpen(participation: ParticipationStatus?, totalKm: Double?): Boolean {
    if (participation != ParticipationStatus.DONE) return true
    return totalKm == null
}

fun mineFillCtaLabel(status: ParticipationStatus): String? = when (status) {
    ParticipationStatus.DONE -> null
    ParticipationStatus.IN_PROGRESS -> "המשך התיעוד"
    ParticipationStatus.PENDING -> "השלמת התיעוד שלי"
}

fun deriveEventStatusAfterParticipation(statuses: List<ParticipationStatus>): EventStatus {
    if (statuses.isEmpty()) return EventStatus.DRAFT
    if (statuses.all { it == ParticipationStatus.DONE }) return EventStatus.DONE
    if (statuses.contains(ParticipationStatus.DONE)) return EventStatus.PARTIAL
    return EventStatus.IN_PROGRESS
}
